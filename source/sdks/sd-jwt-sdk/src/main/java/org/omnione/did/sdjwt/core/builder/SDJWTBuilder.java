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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.exception.SDJWTException;
import org.omnione.did.sdjwt.util.HashUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

public class SDJWTBuilder {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Map<String, Object> claims;
  private final List<Disclosure> disclosures;
  private String hashAlgorithm;
  private boolean includeHashAlgorithm;
  private int decoyCount;

  public SDJWTBuilder() {
    this.claims = new LinkedHashMap<>();
    this.disclosures = new ArrayList<>();
    this.hashAlgorithm = HashUtils.getDefaultHashAlgorithm();
    this.includeHashAlgorithm = false;
    this.decoyCount = 0;
  }

  public SDJWTBuilder(String hashAlgorithm) {
    this();
    this.hashAlgorithm = hashAlgorithm;
  }

  public SDJWTBuilder claim(String name, Object value) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Claim name cannot be null or empty");
    }
    claims.put(name, value);
    return this;
  }

  public SDJWTBuilder claims(Map<String, Object> claims) {
    if (claims != null) {
      claims.forEach(this::claim);
    }
    return this;
  }

  public SDJWTBuilder selectivelyDisclosableClaim(String name, Object value) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Claim name cannot be null or empty");
    }
    Disclosure disclosure = Disclosure.forObjectProperty(name, value);
    disclosures.add(disclosure);
    return this;
  }

  public SDJWTBuilder selectivelyDisclosableClaim(String salt, String name, Object value) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Claim name cannot be null or empty");
    }
    Disclosure disclosure = new Disclosure(salt, name, value);
    disclosures.add(disclosure);
    return this;
  }

  public SDJWTBuilder selectivelyDisclosableClaims(Map<String, Object> claims) {
    if (claims != null) {
      claims.forEach(this::selectivelyDisclosableClaim);
    }
    return this;
  }

  public SDJWTBuilder disclosure(Disclosure disclosure) {
    if (disclosure == null) {
      throw new IllegalArgumentException("Disclosure cannot be null");
    }
    if (disclosure.isArrayElement()) {
      throw new IllegalArgumentException(
          "Array element disclosures are not supported in this context");
    }
    disclosures.add(disclosure);
    return this;
  }

  public SDJWTBuilder includeHashAlgorithm(boolean include) {
    this.includeHashAlgorithm = include;
    return this;
  }

  public SDJWTBuilder decoyDigests(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("Decoy count cannot be negative");
    }
    this.decoyCount = count;
    return this;
  }

  public SDJWTBuilder issuer(String issuer) {
    return claim("iss", issuer);
  }

  public SDJWTBuilder subject(String subject) {
    return claim("sub", subject);
  }

  public SDJWTBuilder audience(String audience) {
    return claim("aud", audience);
  }

  public SDJWTBuilder issuedAtNow() {
    return claim("iat", Instant.now().getEpochSecond());
  }

  public SDJWTBuilder issuedAt(Instant issuedAt) {
    return claim("iat", issuedAt.getEpochSecond());
  }

  public SDJWTBuilder expirationTime(Instant expirationTime) {
    return claim("exp", expirationTime.getEpochSecond());
  }

  public SDJWTBuilder expiresIn(long amount, ChronoUnit unit) {
    Instant exp = Instant.now().plus(amount, unit);
    return claim("exp", exp.getEpochSecond());
  }

  public SDJWTBuilder notBefore(Instant notBefore) {
    return claim("nbf", notBefore.getEpochSecond());
  }

  public SDJWTBuilder jwtId(String jwtId) {
    return claim("jti", jwtId);
  }

  public SDJWTBuilder verifiableCredentialType(String vct) {
    return claim("vct", vct);
  }

  public SDJWTBuilder confirmation(Map<String, Object> cnf) {
    return claim("cnf", cnf);
  }

  public SDJWT build(Function<String, String> jwtSigner) {
    if (jwtSigner == null) {
      throw new IllegalArgumentException("JWT signer function cannot be null");
    }

    try {
      SDObjectBuilder builder = new SDObjectBuilder(hashAlgorithm);

      claims.forEach(builder::putClaim);
      disclosures.forEach(builder::putSDClaim);

      if (decoyCount > 0) {
        builder.putDecoyDigests(decoyCount);
      }

      Map<String, Object> payload = builder.build(includeHashAlgorithm);
      String payloadJson = OBJECT_MAPPER.writeValueAsString(payload);
      String credentialJwt = jwtSigner.apply(payloadJson);

      return new SDJWT(credentialJwt, disclosures);

    } catch (SDJWTException e) {
      throw e;
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to build SD-JWT", e);
    }
  }

  public Map<String, Object> buildPayload() {
    SDObjectBuilder builder = new SDObjectBuilder(hashAlgorithm);

    claims.forEach(builder::putClaim);
    disclosures.forEach(builder::putSDClaim);

    if (decoyCount > 0) {
      builder.putDecoyDigests(decoyCount);
    }

    return builder.build(includeHashAlgorithm);
  }

  public List<Disclosure> getDisclosures() {
    return Collections.unmodifiableList(disclosures);
  }

  public Map<String, Object> getClaims() {
    return Collections.unmodifiableMap(claims);
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }
}