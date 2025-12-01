/*
 * Copyright 2025 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.oid4vc.dcql.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.exception.OID4VCException;
import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.util.SimpleJWTDecoder;

@Slf4j
public class DCQLCredentialMatcher {

  public static boolean matchesFormat(String requiredFormat) {
    if (requiredFormat == null) {
      return true;
    }

    Set<String> supportedFormats = Set.of("dc+sd-jwt", "vc+sd-jwt");

    boolean isSupported = supportedFormats.contains(requiredFormat);

    if (!isSupported) {
      log.info("Unsupported format: {}", requiredFormat);
    }

    return isSupported;
  }

  public static boolean matchesMetadata(SDJWT sdjwt, Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return true;
    }

    try {
      if (metadata.containsKey("vct_values")) {
        List<String> requiredVcts = (List<String>) metadata.get("vct_values");
        if (!checkVctValues(sdjwt, requiredVcts)) {
          log.info("VCT value matching failed");
          return false;
        }
      }

      log.info("All metadata conditions satisfied");
      return true;

    } catch (OID4VCException | IllegalArgumentException e) {
      log.error("Error during metadata check: {}", e.getMessage(), e);
      return false;
    }
  }

  public static Set<String> extractMatchingClaimNames(DCQLQuery dcqlQuery, SDJWT sdjwt) {
    if (dcqlQuery == null || dcqlQuery.getCredentials() == null || sdjwt == null) {
      return Collections.emptySet();
    }

    Set<String> matchingClaims = new HashSet<>();

    Map<String, Object> allClaims = extractAllCredentialClaims(sdjwt);

    dcqlQuery.getCredentials().forEach(credential -> {
      if (credential.getClaims() == null) {
        matchingClaims.addAll(allClaims.keySet());
        log.info("credential.claims is null, including all claims: {}", allClaims.keySet());
        return;
      }

      if (credential.getClaims().isEmpty()) {
        log.info("credential.claims is empty, excluding claims");
        return;
      }

      credential.getClaims().forEach(claimQuery -> {
        List<Object> path = claimQuery.getPath();

        if (path == null || path.isEmpty()) {
          log.info("Claim path is empty or null");
          return;
        }

        processPathAndCollectClaims(allClaims, path, claimQuery, matchingClaims, sdjwt);
      });
    });

    return matchingClaims;
  }

  private static Map<String, Object> extractAllCredentialClaims(SDJWT sdjwt) {
    Map<String, Object> allClaims = new HashMap<>();

    try {
      SimpleJWTDecoder.SimpleJWT jwt = SimpleJWTDecoder.parse(sdjwt.getCredentialJwt());
      Map<String, Object> payload = jwt.getPayloadAsMap();

      payload.entrySet().stream().filter(entry -> !isReservedJWTClaim(entry.getKey()))
          .forEach(entry -> allClaims.put(entry.getKey(), entry.getValue()));

      log.info("Extracted claims from JWT payload: {}", allClaims.keySet());

    } catch (OID4VCException | IllegalArgumentException e) {
      log.error("JWT payload extraction failed: {}", e.getMessage(), e);
    }

    if (sdjwt.getDisclosures() != null && !sdjwt.getDisclosures().isEmpty()) {
      for (Disclosure disclosure : sdjwt.getDisclosures()) {
        if (disclosure.getClaimName() != null) {
          allClaims.put(disclosure.getClaimName(), disclosure.getClaimValue());
          log.info("Added top-level disclosed claim: {} = {}", disclosure.getClaimName(),
              disclosure.getClaimValue());
        }
      }

      integrateDisclosuresIntoCredential(allClaims, sdjwt);
    }

    log.info("All extracted claims after integrating disclosures: {}", allClaims.keySet());
    return allClaims;
  }

  private static void integrateDisclosuresIntoCredential(Map<String, Object> allClaims,
      SDJWT sdjwt) {
    Map<String, Disclosure> digestToDisclosure = new HashMap<>();
    for (Disclosure disclosure : sdjwt.getDisclosures()) {
      String digest = disclosure.digest();
      digestToDisclosure.put(digest, disclosure);
      log.info("Disclosure digest: {} -> {}.{}", digest, disclosure.getClaimName(),
          disclosure.getClaimValue());
    }

    for (Map.Entry<String, Object> entry : new HashMap<>(allClaims).entrySet()) {
      String claimName = entry.getKey();
      Object claimValue = entry.getValue();

      if (claimValue instanceof Map) {
        integrateDisclosuresIntoObject(claimName, (Map<String, Object>) claimValue,
            digestToDisclosure, allClaims);
      }
    }
  }

  private static void integrateDisclosuresIntoObject(String parentName,
      Map<String, Object> parentObject, Map<String, Disclosure> digestToDisclosure,
      Map<String, Object> allClaims) {

    Object sdValue = parentObject.get("_sd");
    if (sdValue instanceof List) {
      List<?> sdArray = (List<?>) sdValue;

      for (Object sdItem : sdArray) {
        if (sdItem instanceof String) {
          String digest = (String) sdItem;

          Disclosure disclosure = digestToDisclosure.get(digest);
          if (disclosure != null && disclosure.getClaimName() != null) {
            String claimName = disclosure.getClaimName();
            Object claimValue = disclosure.getClaimValue();

            parentObject.put(claimName, claimValue);
            log.info("Integrated disclosure: {}.{} = {}", parentName, claimName, claimValue);

            if (claimValue instanceof Map) {
              integrateDisclosuresIntoObject(parentName + "." + claimName,
                  (Map<String, Object>) claimValue, digestToDisclosure, allClaims);
            }
          }
        }
      }
    }
  }

  private static void processPathAndCollectClaims(Map<String, Object> allClaims, List<Object> path,
      DCQLQuery.ClaimQuery claimQuery, Set<String> matchingClaims, SDJWT sdjwt) {

    if (path.isEmpty() || !(path.get(0) instanceof String)) {
      log.info("Invalid path: first element must be a string (top-level claim)");
      return;
    }

    String topLevelClaim = (String) path.get(0);
    Object rootValue = allClaims.get(topLevelClaim);

    log.info("Processing path: {}, topLevelClaim: {}, rootValue type: {}", path, topLevelClaim,
        rootValue != null ? rootValue.getClass().getSimpleName() : "null");

    if (rootValue == null) {
      log.info("Top-level claim not found: {}", topLevelClaim);
      return;
    }

    if (path.size() == 1) {
      if (meetsClaimConditions(claimQuery, rootValue)) {
        matchingClaims.add(topLevelClaim);
        log.info("Single-level path matched: {} = {}", topLevelClaim, rootValue);
      } else {
        log.info("Single-level path condition not met: {} = {}", topLevelClaim, rootValue);
      }
      return;
    }

    List<Object> remainingPath = new ArrayList<>(path.subList(1, path.size()));
    log.info("Multi-depth path detected: {} with remaining path: {}", topLevelClaim, remainingPath);
    collectMatchingValuesFromPath(rootValue, remainingPath, claimQuery, topLevelClaim,
        matchingClaims);
  }

  private static void collectMatchingValuesFromPath(Object current, List<Object> remainingPath,
      DCQLQuery.ClaimQuery claimQuery, String claimNamePrefix, Set<String> matchingClaims) {

    log.info("collectMatchingValuesFromPath: claimNamePrefix={}, remainingPath={}, currentType={}",
        claimNamePrefix, remainingPath,
        current != null ? current.getClass().getSimpleName() : "null");

    if (remainingPath.isEmpty()) {
      log.info("End of path reached for: {}, value: {}", claimNamePrefix, current);
      if (meetsClaimConditions(claimQuery, current)) {
        matchingClaims.add(claimNamePrefix);
        log.info("Deep path matched: {} = {}", claimNamePrefix, current);
      }
      return;
    }

    Object nextPathElement = remainingPath.get(0);
    List<Object> nextRemaining = new ArrayList<>(remainingPath.subList(1, remainingPath.size()));

    if (nextPathElement == null) {
      if (!(current instanceof List)) {
        log.info("Wildcard path element (null) but current is not a list: {}", current.getClass());
        return;
      }

      List<?> currentList = (List<?>) current;
      for (int i = 0; i < currentList.size(); i++) {
        Object item = currentList.get(i);
        String newClaimName = claimNamePrefix + "[" + i + "]";
        collectMatchingValuesFromPath(item, nextRemaining, claimQuery, newClaimName,
            matchingClaims);
      }
      return;
    }

    if (nextPathElement instanceof Integer) {
      if (!(current instanceof List)) {
        log.info("Integer index but current is not a list: {}", current.getClass());
        return;
      }

      int idx = (Integer) nextPathElement;
      List<?> currentList = (List<?>) current;

      if (idx < 0 || idx >= currentList.size()) {
        log.info("Index out of bounds: {} (list size: {})", idx, currentList.size());
        return;
      }

      Object element = currentList.get(idx);
      String newClaimName = claimNamePrefix + "[" + idx + "]";
      collectMatchingValuesFromPath(element, nextRemaining, claimQuery, newClaimName,
          matchingClaims);
      return;
    }

    if (nextPathElement instanceof String) {
      if (!(current instanceof Map)) {
        log.info("String key but current is not a map: type={}, value={}",
            current.getClass().getSimpleName(), current);
        return;
      }

      Map<?, ?> currentMap = (Map<?, ?>) current;
      String key = (String) nextPathElement;

      log.info("Searching for key '{}' in map with keys: {}", key, currentMap.keySet());

      Object nextValue = currentMap.get(key);
      if (nextValue == null) {
        log.info("Key not found in map: {} (available keys: {})", key, currentMap.keySet());
        return;
      }

      String newClaimName = claimNamePrefix + "." + key;
      log.info("Found nested value at path '{}': {}", newClaimName, nextValue);
      collectMatchingValuesFromPath(nextValue, nextRemaining, claimQuery, newClaimName,
          matchingClaims);
    }
  }

  private static boolean checkVctValues(SDJWT sdjwt, List<String> requiredVcts) {
    if (requiredVcts == null || requiredVcts.isEmpty()) {
      return true;
    }

    try {
      SimpleJWTDecoder.SimpleJWT jwt = SimpleJWTDecoder.parse(sdjwt.getCredentialJwt());
      Map<String, Object> payload = jwt.getPayloadAsMap();

      Object vctClaim = payload.get("vct");
      if (vctClaim == null) {
        log.info("SD-JWT has no vct claim");
        return false;
      }

      String actualVct = vctClaim.toString();
      boolean matches = requiredVcts.contains(actualVct);

      log.info("VCT matching: required={}, actual={}, result={}", requiredVcts, actualVct, matches);

      return matches;

    } catch (OID4VCException | IllegalArgumentException e) {
      log.error("VCT check error: {}", e.getMessage(), e);
      return false;
    }
  }

  private static boolean meetsClaimConditions(DCQLQuery.ClaimQuery claimQuery, Object actualValue) {
    if (actualValue == null) {
      return false;
    }

    if (claimQuery.getValues() != null && !claimQuery.getValues().isEmpty()) {
      boolean valueMatches = claimQuery.getValues().contains(actualValue);
      log.info("  values condition: {} in {} = {}", actualValue, claimQuery.getValues(),
          valueMatches);
      if (!valueMatches) {
        return false;
      }
    }

    if (claimQuery.getValue() != null) {
      boolean exactMatch = claimQuery.getValue().equals(actualValue);
      log.info("  value condition: {} == {} = {}", actualValue, claimQuery.getValue(), exactMatch);
      if (!exactMatch) {
        return false;
      }
    }

    if (claimQuery.getMin() != null) {
      if (!checkMinCondition(actualValue, claimQuery.getMin())) {
        return false;
      }
    }

    if (claimQuery.getMax() != null) {
      if (!checkMaxCondition(actualValue, claimQuery.getMax())) {
        return false;
      }
    }

    return true;
  }

  private static boolean checkMinCondition(Object actualValue, Object minValue) {
    try {
      if (actualValue instanceof Number && minValue instanceof Number) {
        double actual = ((Number) actualValue).doubleValue();
        double min = ((Number) minValue).doubleValue();
        return actual >= min;
      }

      if (actualValue instanceof String && minValue instanceof String) {
        return ((String) actualValue).compareTo((String) minValue) >= 0;
      }

      return true;

    } catch (OID4VCException | IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean checkMaxCondition(Object actualValue, Object maxValue) {
    try {
      if (actualValue instanceof Number && maxValue instanceof Number) {
        double actual = ((Number) actualValue).doubleValue();
        double max = ((Number) maxValue).doubleValue();
        return actual <= max;
      }

      if (actualValue instanceof String && maxValue instanceof String) {
        return ((String) actualValue).compareTo((String) maxValue) <= 0;
      }

      return true;

    } catch (OID4VCException | IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isReservedJWTClaim(String claimName) {
    Set<String> reservedClaims = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti", "_sd_alg",
        "_sd", "cnf", "vct");
    return reservedClaims.contains(claimName);
  }
}