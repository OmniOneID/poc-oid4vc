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

package org.omnione.did.sdk.sdjwt.core.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.omnione.did.sdk.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;
import org.omnione.did.sdk.sdjwt.util.HashUtils;
import org.omnione.did.sdk.sdjwt.util.SaltGenerator;

public class SDObjectBuilder {

  private final String hashAlgorithm;
  private final Map<String, Object> claims;
  private final List<String> sdArray;
  private final Map<String, SDObjectBuilder> nestedBuilders;

  public SDObjectBuilder() {
    this(HashUtils.getDefaultHashAlgorithm());
  }

  public SDObjectBuilder(String hashAlgorithm) {
    if (!HashUtils.isSupportedHashAlgorithm(hashAlgorithm)) {
      throw new SDJWTException("Unsupported hash algorithm: " + hashAlgorithm);
    }

    this.hashAlgorithm = hashAlgorithm;
    this.claims = new LinkedHashMap<>();
    this.sdArray = new ArrayList<>();
    this.nestedBuilders = new LinkedHashMap<>();
  }

  public SDObjectBuilder putClaim(String claimName, Object claimValue) {
    validateClaimName(claimName);
    claims.put(claimName, claimValue);
    return this;
  }

  public SDObjectBuilder putSDClaim(Disclosure disclosure) {
    if (disclosure == null) {
      throw new SDJWTException("Disclosure cannot be null");
    }
    if (disclosure.isArrayElement()) {
      throw new SDJWTException("Array element disclosures cannot be used for object properties");
    }

    String digest = disclosure.digest(hashAlgorithm);
    sdArray.add(digest);
    return this;
  }

  public SDObjectBuilder putNestedObject(String fieldName, Map<String, Object> nestedClaims,
      List<Disclosure> nestedDisclosures) {
    if (fieldName == null || fieldName.trim().isEmpty()) {
      throw new SDJWTException("Field name cannot be null or empty");
    }
    validateClaimName(fieldName);
    if (nestedClaims == null) {
      throw new SDJWTException("Nested claims cannot be null");
    }

    SDObjectBuilder nestedBuilder = new SDObjectBuilder(hashAlgorithm);

    Set<String> disclosureFieldNames = new HashSet<>();
    if (nestedDisclosures != null) {
      for (Disclosure disclosure : nestedDisclosures) {
        if (disclosure.getClaimName() != null) {
          disclosureFieldNames.add(disclosure.getClaimName());
        }
      }
    }

    for (Map.Entry<String, Object> entry : nestedClaims.entrySet()) {
      if (!disclosureFieldNames.contains(entry.getKey())) {
        nestedBuilder.putClaim(entry.getKey(), entry.getValue());
      }
    }

    if (nestedDisclosures != null) {
      nestedDisclosures.forEach(nestedBuilder::putSDClaim);
    }

    nestedBuilders.put(fieldName, nestedBuilder);
    return this;
  }

  public SDObjectBuilder putDecoyDigest() {

    String decoySalt = SaltGenerator.generate();
    String decoyClaimName = "_decoy_" + System.nanoTime();
    String decoyClaimValue = "decoy_value_" + UUID.randomUUID().toString();

    Disclosure decoyDisclosure = new Disclosure(decoySalt, decoyClaimName, decoyClaimValue);
    String decoyDigest = decoyDisclosure.digest(hashAlgorithm);

    sdArray.add(decoyDigest);
    return this;
  }

  public SDObjectBuilder putDecoyDigests(int count) {
    if (count < 0) {
      throw new SDJWTException("Count cannot be negative");
    }

    for (int i = 0; i < count; i++) {
      putDecoyDigest();
    }
    return this;
  }

  public Map<String, Object> build() {
    return build(false);
  }

  public Map<String, Object> build(boolean includeHashAlg) {
    Map<String, Object> result = new LinkedHashMap<>(claims);

    for (Map.Entry<String, SDObjectBuilder> nestedEntry : nestedBuilders.entrySet()) {
      String fieldName = nestedEntry.getKey();
      SDObjectBuilder nestedBuilder = nestedEntry.getValue();
      result.put(fieldName, nestedBuilder.build(includeHashAlg));
    }

    if (!sdArray.isEmpty()) {
      List<String> shuffledSdArray = new ArrayList<>(sdArray);
      Collections.shuffle(shuffledSdArray);
      result.put("_sd", shuffledSdArray);
    }

    if (includeHashAlg) {
      result.put("_sd_alg", hashAlgorithm);
    }

    return result;
  }

  private void validateClaimName(String claimName) {
    if (claimName == null || claimName.trim().isEmpty()) {
      throw new SDJWTException("Claim name cannot be null or empty");
    }

    if ("_sd".equals(claimName) || "_sd_alg".equals(claimName)) {
      throw new SDJWTException("Claim name '" + claimName + "' is reserved for SD-JWT");
    }
  }

  @Override
  public String toString() {
    return String.format(
        "SDObjectBuilder{hashAlgorithm='%s', claims=%d, sdClaims=%d, nestedObjects=%d}",
        hashAlgorithm, claims.size(), sdArray.size(), nestedBuilders.size());
  }
}
