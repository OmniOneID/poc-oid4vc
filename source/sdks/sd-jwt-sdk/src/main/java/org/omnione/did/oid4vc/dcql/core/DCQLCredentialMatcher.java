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

import lombok.extern.slf4j.Slf4j;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.exception.OID4VCException;
import org.omnione.did.sdjwt.util.SimpleJWTDecoder;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;

//TODO: Multi-depth support must be implemented.
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

    Map<String, Object> actualValues = extractClaimValues(sdjwt);

    dcqlQuery.getCredentials().forEach(credential -> {
      if (credential.getClaims() == null) {
        matchingClaims.addAll(actualValues.keySet());
        log.info("credential.claims is null, including all claims: {}", actualValues.keySet());
        return;
      }

      if (credential.getClaims().isEmpty()) {
        log.info("credential.claims is empty, excluding claims");
        return;
      }

      credential.getClaims().forEach(claimQuery -> {
        if (DCQLPathProcessor.isIndexBasedPath(claimQuery.getPath())) {
          Integer index = DCQLPathProcessor.extractIndex(claimQuery.getPath());
          if (index != null && sdjwt.getDisclosures() != null && index >= 0 && index < sdjwt.getDisclosures().size()) {
            var disclosure = sdjwt.getDisclosures().get(index);
            String claimName = disclosure.getClaimName();
            Object claimValue = disclosure.getClaimValue();

            if (meetsClaimConditions(claimQuery, claimValue)) {
              matchingClaims.add(claimName);
              log.info("Index-based condition satisfied claim added: [{}] {}={}", index, claimName, claimValue);
            } else {
              log.info("Index-based condition unsatisfied claim excluded: [{}] {}={}", index, claimName, claimValue);
            }
          } else {
            log.info("Invalid index or disclosures: index={}, disclosures size={}", index,
                sdjwt.getDisclosures() != null ? sdjwt.getDisclosures().size() : 0);
          }
        } else {
          String claimName = DCQLPathProcessor.pathToClaimName(claimQuery.getPath());
          if (claimName != null && meetsClaimConditions(claimQuery, actualValues.get(claimName))) {
            matchingClaims.add(claimName);
            log.info("Condition satisfied claim added: {}={}", claimName,
                actualValues.get(claimName));
          } else {
            log.info("Condition unsatisfied claim excluded: {}={}", claimName,
                actualValues.get(claimName));
          }
        }
      });
    });

    return matchingClaims;
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

  private static Map<String, Object> extractClaimValues(SDJWT sdjwt) {
    Map<String, Object> claimValues = new HashMap<>();

    if (sdjwt.getDisclosures() != null) {
      for (var disclosure : sdjwt.getDisclosures()) {
        String claimName = disclosure.getClaimName();
        Object claimValue = disclosure.getClaimValue();

        claimValues.put(claimName, claimValue);

        if (claimValue instanceof Map) {
          extractNestedClaims(claimName, (Map<String, Object>) claimValue, claimValues);
        }
      }
    }

    try {
      SimpleJWTDecoder.SimpleJWT jwt = SimpleJWTDecoder.parse(sdjwt.getCredentialJwt());
      Map<String, Object> payload = jwt.getPayloadAsMap();

      payload.entrySet().stream().filter(entry -> !isReservedJWTClaim(entry.getKey()))
          .forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();

            claimValues.put(key, value);

            if (value instanceof Map) {
              extractNestedClaims(key, (Map<String, Object>) value, claimValues);
            }
          });

    } catch (OID4VCException | IllegalArgumentException e) {
      log.error("JWT payload claim extraction failed: {}", e.getMessage(), e);
    }

    return claimValues;
  }

  private static void extractNestedClaims(String parentPath, Map<String, Object> nestedMap,
      Map<String, Object> claimValues) {
    if (nestedMap == null) {
      return;
    }

    for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
      String nestedKey = entry.getKey();
      Object nestedValue = entry.getValue();
      String fullPath = parentPath + "." + nestedKey;

      claimValues.put(fullPath, nestedValue);

      if (nestedValue instanceof Map) {
        extractNestedClaims(fullPath, (Map<String, Object>) nestedValue, claimValues);
      }
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