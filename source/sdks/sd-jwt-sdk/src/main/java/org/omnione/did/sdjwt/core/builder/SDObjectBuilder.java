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

package org.omnione.did.sdjwt.core.builder;

import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.util.HashUtils;
import org.omnione.did.sdjwt.util.SaltGenerator;

import java.util.*;

public class SDObjectBuilder {

  private final String hashAlgorithm;
  private final Map<String, Object> claims;
  private final List<String> sdArray;

  public SDObjectBuilder() {
    this(HashUtils.getDefaultHashAlgorithm());
  }

  public SDObjectBuilder(String hashAlgorithm) {
    if (!HashUtils.isSupportedHashAlgorithm(hashAlgorithm)) {
      throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlgorithm);
    }

    this.hashAlgorithm = hashAlgorithm;
    this.claims = new LinkedHashMap<>();
    this.sdArray = new ArrayList<>();
  }

  public SDObjectBuilder putClaim(String claimName, Object claimValue) {
    validateClaimName(claimName);
    claims.put(claimName, claimValue);
    return this;
  }

  public SDObjectBuilder putSDClaim(Disclosure disclosure) {
    if (disclosure == null) {
      throw new IllegalArgumentException("Disclosure cannot be null");
    }
    if (disclosure.isArrayElement()) {
      throw new IllegalArgumentException(
          "Array element disclosures cannot be used for object properties");
    }

    String digest = disclosure.digest(hashAlgorithm);
    sdArray.add(digest);
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
      throw new IllegalArgumentException("Count cannot be negative");
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
      throw new IllegalArgumentException("Claim name cannot be null or empty");
    }

    if ("_sd".equals(claimName) || "_sd_alg".equals(claimName)) {
      throw new IllegalArgumentException("Claim name '" + claimName + "' is reserved for SD-JWT");
    }
  }

  @Override
  public String toString() {
    return String.format("SDObjectBuilder{hashAlgorithm='%s', claims=%d, sdClaims=%d}",
        hashAlgorithm, claims.size(), sdArray.size());
  }
}