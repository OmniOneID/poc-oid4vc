/*
 * Copyright 2026 OmniOne.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.dcql.core.credential.CredentialAdapter;
import org.omnione.did.oid4vc.dcql.core.credential.CredentialAdapterRegistry;
import org.omnione.did.oid4vc.dcql.core.credential.ParsedCredential;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.dcql.exception.DCQLException;

/**
 * Format-agnostic credential matcher for DCQL queries.
 * Uses CredentialAdapter to support multiple credential formats.
 */
@Slf4j
public class DCQLCredentialMatcher {

  private static final CredentialAdapterRegistry registry = CredentialAdapterRegistry.getInstance();

  /**
   * Checks if the required format is supported.
   *
   * @param requiredFormat the format to check
   * @return true if the format is supported or null
   */
  public static boolean matchesFormat(String requiredFormat) {
    if (requiredFormat == null) {
      return true;
    }

    boolean isSupported = registry.isFormatSupported(requiredFormat);

    if (!isSupported) {
      log.info("Unsupported format: {}", requiredFormat);
    }

    return isSupported;
  }

  /**
   * Checks if the credential matches the metadata requirements.
   *
   * @param credential the parsed credential
   * @param metadata the metadata requirements
   * @return true if the credential matches
   */
  public static boolean matchesMetadata(ParsedCredential credential, Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return true;
    }

    Optional<CredentialAdapter> adapterOpt = registry.findAdapter(credential.getFormat());
    if (adapterOpt.isEmpty()) {
      log.warn("No adapter found for format: {}", credential.getFormat());
      return false;
    }

    return adapterOpt.get().matchesMetadata(credential, metadata);
  }

  /**
   * Extracts matching claim names based on DCQL query.
   *
   * @param dcqlQuery the DCQL query
   * @param credential the parsed credential
   * @return set of matching claim names
   */
  public static Set<String> extractMatchingClaimNames(DCQLQuery dcqlQuery, ParsedCredential credential) {
    if (dcqlQuery == null || dcqlQuery.getCredentials() == null || credential == null) {
      return Collections.emptySet();
    }

    Set<String> matchingClaims = new HashSet<>();
    Map<String, Object> allClaims = credential.getAllClaims();

    dcqlQuery.getCredentials().forEach(credentialQuery -> {
      if (credentialQuery.getClaims() == null) {
        matchingClaims.addAll(allClaims.keySet());
        log.info("credential.claims is null, including all claims: {}", allClaims.keySet());
        return;
      }

      if (credentialQuery.getClaims().isEmpty()) {
        log.info("credential.claims is empty, excluding claims");
        return;
      }

      credentialQuery.getClaims().forEach(claimQuery -> {
        List<Object> path = claimQuery.getPath();

        if (path == null || path.isEmpty()) {
          log.info("Claim path is empty or null");
          return;
        }

        processPathAndCollectClaims(allClaims, path, claimQuery, matchingClaims);
      });
    });

    return matchingClaims;
  }

  /**
   * Parses a raw credential string using the appropriate adapter.
   *
   * @param rawCredential the raw credential string
   * @param format the credential format (optional, will be auto-detected if null)
   * @return the parsed credential
   * @throws DCQLException if parsing fails
   */
  public static ParsedCredential parseCredential(String rawCredential, String format) throws DCQLException {
    Optional<CredentialAdapter> adapterOpt;

    if (format != null) {
      adapterOpt = registry.findAdapter(format);
    } else {
      adapterOpt = registry.detectAdapter(rawCredential);
    }

    if (adapterOpt.isEmpty()) {
      throw new DCQLException("No adapter found for credential format: " + format);
    }

    return adapterOpt.get().parse(rawCredential);
  }

  // ========== Private Helper Methods ==========

  private static void processPathAndCollectClaims(Map<String, Object> allClaims, List<Object> path,
      DCQLQuery.ClaimQuery claimQuery, Set<String> matchingClaims) {

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
    collectMatchingValuesFromPath(rootValue, remainingPath, claimQuery, topLevelClaim, matchingClaims);
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
        log.info("Wildcard path element (null) but current is not a list");
        return;
      }

      List<?> currentList = (List<?>) current;
      for (int i = 0; i < currentList.size(); i++) {
        Object item = currentList.get(i);
        String newClaimName = claimNamePrefix + "[" + i + "]";
        collectMatchingValuesFromPath(item, nextRemaining, claimQuery, newClaimName, matchingClaims);
      }
      return;
    }

    if (nextPathElement instanceof Integer) {
      if (!(current instanceof List)) {
        log.info("Integer index but current is not a list");
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
      collectMatchingValuesFromPath(element, nextRemaining, claimQuery, newClaimName, matchingClaims);
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
      collectMatchingValuesFromPath(nextValue, nextRemaining, claimQuery, newClaimName, matchingClaims);
    }
  }

  private static boolean meetsClaimConditions(DCQLQuery.ClaimQuery claimQuery, Object actualValue) {
    if (actualValue == null) {
      return false;
    }

    if (claimQuery.getValues() != null && !claimQuery.getValues().isEmpty()) {
      boolean valueMatches = claimQuery.getValues().contains(actualValue);
      log.info("  values condition: {} in {} = {}", actualValue, claimQuery.getValues(), valueMatches);
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
    if (actualValue instanceof Number && minValue instanceof Number) {
      double actual = ((Number) actualValue).doubleValue();
      double min = ((Number) minValue).doubleValue();
      return actual >= min;
    }

    if (actualValue instanceof String && minValue instanceof String) {
      return ((String) actualValue).compareTo((String) minValue) >= 0;
    }

    return true;
  }

  private static boolean checkMaxCondition(Object actualValue, Object maxValue) {
    if (actualValue instanceof Number && maxValue instanceof Number) {
      double actual = ((Number) actualValue).doubleValue();
      double max = ((Number) maxValue).doubleValue();
      return actual <= max;
    }

    if (actualValue instanceof String && maxValue instanceof String) {
      return ((String) actualValue).compareTo((String) maxValue) <= 0;
    }

    return true;
  }
}