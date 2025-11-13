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

package org.omnione.did.oid4vc.oid4vp.core;

import org.omnione.did.sdjwt.datamodel.Disclosure;

import java.util.*;
import java.util.stream.Collectors;

public class SelectiveDisclosureProcessor {

  public static List<Disclosure> filterDisclosures(List<Disclosure> disclosures,
      Set<String> requestedClaims) {
    if (disclosures == null || disclosures.isEmpty()) {
      return Collections.emptyList();
    }

    if (requestedClaims == null || requestedClaims.isEmpty()) {
      return Collections.emptyList();
    }

    List<Disclosure> filteredDisclosures = disclosures.stream()
        .filter(disclosure -> {
          String claimName = disclosure.getClaimName();
          boolean isRequested = isClaimRequested(claimName, requestedClaims);

          return isRequested;
        })
        .collect(Collectors.toList());

    return filteredDisclosures;
  }

  private static boolean isClaimRequested(String claimName, Set<String> requestedClaims) {
    if (claimName == null || requestedClaims == null) {
      return false;
    }

    if (requestedClaims.contains(claimName)) {
      return true;
    }

    return requestedClaims.stream()
        .anyMatch(requested -> matchesPattern(claimName, requested));
  }

  private static boolean matchesPattern(String claimName, String pattern) {
    if (pattern.contains("*")) {
      String regex = pattern.replace("*", ".*");
      return claimName.matches(regex);
    }
    return claimName.equals(pattern);
  }
}