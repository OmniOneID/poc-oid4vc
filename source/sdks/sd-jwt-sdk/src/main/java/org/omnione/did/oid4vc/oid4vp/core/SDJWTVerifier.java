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

import java.io.IOException;
import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.exception.SDJWTException;
import org.omnione.did.sdjwt.crypto.JWSVerifier;
import org.omnione.did.sdjwt.crypto.SignedJWT;
import org.omnione.did.sdjwt.crypto.impl.ECDSAVerifier;
import org.omnione.did.sdjwt.crypto.impl.RSASSAVerifier;
import org.omnione.did.sdjwt.util.HashUtils;
import org.omnione.did.sdjwt.core.validator.SDJWTValidator;
import org.omnione.did.oid4vc.exception.OID4VCException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.*;

public class SDJWTVerifier {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final PublicKey publicKey;
  private final SDJWTValidator validator;
  private boolean requireKeyBinding;
  private PublicKey holderPublicKey;

  public SDJWTVerifier(PublicKey publicKey) {
    if (publicKey == null) {
      throw new IllegalArgumentException("PublicKey key cannot be null");
    }
    this.publicKey = publicKey;
    this.validator = new SDJWTValidator();
    this.requireKeyBinding = false;
  }

  public SDJWTVerifier(PublicKey publicKey, PublicKey holderPublicKey) {
    if (publicKey == null) {
      throw new IllegalArgumentException("Public key cannot be null");
    }
    if (holderPublicKey == null) {
      throw new IllegalArgumentException("Holder Public key cannot be null");
    }

    this.publicKey = publicKey;
    this.holderPublicKey = holderPublicKey;
    this.validator = new SDJWTValidator();
    this.requireKeyBinding = false;
  }

  public SDJWTClaimsSet verify(String sdJwtString) throws OID4VCException {
    return verify(sdJwtString, null, null);
  }

  public SDJWTClaimsSet verify(String sdJwtString, String expectedAudience, String expectedNonce)
      throws OID4VCException {
    try {

      SDJWT sdJwt = SDJWT.parse(sdJwtString);

      SDJWTValidator.ValidationResult structureResult = validator.validate(sdJwt);
      if (!structureResult.isValid()) {
        throw new OID4VCException(
            "SD-JWT structure validation failed: " + structureResult.getErrors());
      }

      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());
      if (!verifyJWTSignature(credentialJwt, publicKey)) {
        throw new OID4VCException("Credential JWT signature verification failed");
      }

      if (sdJwt.hasKeyBindingJwt() || requireKeyBinding) {
        if (!sdJwt.hasKeyBindingJwt() && requireKeyBinding) {
          throw new OID4VCException("Key binding JWT is required but not present");
        }

        if (sdJwt.hasKeyBindingJwt()) {
          verifyKeyBindingJWT(sdJwt, expectedAudience, expectedNonce, sdJwtString);
        }
      }

      verifyDisclosureIntegrity(sdJwt);

      return buildClaimsSet(sdJwt);

    } catch (OID4VCException e) {
      throw e;
    } catch (ParseException e) {
      throw new OID4VCException("Failed to parse SD-JWT", e);
    }
  }

  private boolean verifyJWTSignature(SignedJWT jwt, PublicKey publicKey) throws OID4VCException {
    try {
      JWSVerifier verifier;

      if (publicKey instanceof RSAPublicKey) {
        verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
      } else if (publicKey instanceof ECPublicKey) {
        verifier = new ECDSAVerifier((ECPublicKey) publicKey);
      } else {
        throw new IllegalArgumentException("Unsupported public key type: " + publicKey.getClass());
      }

      return jwt.verify(verifier);
    } catch (OID4VCException e) {
      throw e;
    } catch (SDJWTException e) {
      throw new OID4VCException("Failed to verify JWT signature", e);
    }
  }

  private void verifyKeyBindingJWT(SDJWT sdJwt, String expectedAudience, String expectedNonce,
      String originalSdJwtString) throws OID4VCException {
    try {
      SignedJWT kbJwt = SignedJWT.parse(sdJwt.getKeyBindingJwt());

      PublicKey kbPublicKey = holderPublicKey != null ? holderPublicKey : extractHolderPublicKey();
      if (kbPublicKey == null) {
        throw new OID4VCException("No holder public key available for key binding verification");
      }

      if (!verifyJWTSignature(kbJwt, kbPublicKey)) {
        throw new OID4VCException("Key binding JWT signature verification failed");
      }

      Map<String, Object> kbClaims = kbJwt.getJWTClaimsSet();

      if (expectedAudience != null) {

        Object aud = kbClaims.get("aud");
        boolean audienceMatches = false;

        if (aud instanceof String) {
          audienceMatches = expectedAudience.equals(aud);
        } else if (aud instanceof List) {
          List<?> audList = (List<?>) aud;
          audienceMatches = audList.contains(expectedAudience);
        }

        if (!audienceMatches) {
          throw new OID4VCException(
              "Key binding JWT audience mismatch. Expected: " + expectedAudience + ", Got: " + aud);
        }
      }

      if (expectedNonce != null) {
        Object nonce = kbClaims.get("nonce");
        if (!expectedNonce.equals(nonce)) {
          throw new OID4VCException(
              "Key binding JWT nonce mismatch. Expected: " + expectedNonce + ", Got: " + nonce);
        }
      }

      verifySdHash(kbClaims, originalSdJwtString);

    } catch (OID4VCException e) {
      throw e;
    } catch (ParseException | IOException e) {
      throw new OID4VCException("Failed to parse key binding JWT", e);
    }
  }

  private void verifySdHash(Map<String, Object> kbClaims, String originalSdJwtString)
      throws OID4VCException {
    Object sdHashClaim = kbClaims.get("sd_hash");
    if (sdHashClaim == null) {
      throw new OID4VCException("Key binding JWT missing required sd_hash claim");
    }

    if (!(sdHashClaim instanceof String)) {
      throw new OID4VCException("sd_hash claim must be a string");
    }

    String expectedSdHash = (String) sdHashClaim;

    String[] parts = originalSdJwtString.split("~", -1);
    StringBuilder sdJwtForHash = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      sdJwtForHash.append(parts[i]).append("~");
    }

    String calculatedSdHash = calculateSHA256Hash(sdJwtForHash.toString());

    if (!expectedSdHash.equals(calculatedSdHash)) {
      throw new OID4VCException(
          String.format("sd_hash verification failed. Expected: %s, Calculated: %s",
              expectedSdHash, calculatedSdHash));
    }
  }

  private String calculateSHA256Hash(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private PublicKey extractHolderPublicKey() throws OID4VCException {

    return holderPublicKey;
  }

  private void verifyDisclosureIntegrity(SDJWT sdJwt) throws OID4VCException {
    try {
      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());
      Map<String, Object> payload = credentialJwt.getJWTClaimsSet();

      List<String> sdArray = extractSDArray(payload);
      String hashAlgorithm = extractHashAlgorithm(payload);

      for (Disclosure disclosure : sdJwt.getDisclosures()) {
        String computedDigest = disclosure.digest(hashAlgorithm);

        if (!sdArray.contains(computedDigest)) {
          throw new OID4VCException("Disclosure digest not found in _sd array: " + computedDigest);
        }
      }

    } catch (OID4VCException e) {
      throw e;
    } catch (IOException | ParseException e) {
      throw new OID4VCException("Failed to parse SDJWT", e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> extractSDArray(Map<String, Object> payload) {
    Object sdObj = payload.get("_sd");
    if (sdObj instanceof List) {
      return (List<String>) sdObj;
    }
    return new ArrayList<>();
  }

  private String extractHashAlgorithm(Map<String, Object> payload) {
    Object alg = payload.get("_sd_alg");
    return alg != null ? alg.toString() : HashUtils.getDefaultHashAlgorithm();
  }

  private SDJWTClaimsSet buildClaimsSet(SDJWT sdJwt) throws OID4VCException {
    try {
      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());
      Map<String, Object> baseClaims = new LinkedHashMap<>(credentialJwt.getJWTClaimsSet());

      baseClaims.remove("_sd");
      baseClaims.remove("_sd_alg");

      for (Disclosure disclosure : sdJwt.getDisclosures()) {
        if (!disclosure.isArrayElement()) {
          baseClaims.put(disclosure.getClaimName(), disclosure.getClaimValue());
        }
      }

      return new SDJWTClaimsSet(baseClaims, sdJwt.getDisclosures());

    } catch (OID4VCException e) {
      throw e;
    } catch (IOException | ParseException e) {
      throw new OID4VCException("Failed to parse SDJWT", e);
    }
  }

  public static class SDJWTClaimsSet {

    private final Map<String, Object> claims;
    private final List<Disclosure> disclosures;

    public SDJWTClaimsSet(Map<String, Object> claims, List<Disclosure> disclosures) {
      this.claims = Collections.unmodifiableMap(new LinkedHashMap<>(claims));
      this.disclosures = Collections.unmodifiableList(new ArrayList<>(disclosures));
    }

    public Map<String, Object> getClaims() {
      return claims;
    }

    public List<Disclosure> getDisclosures() {
      return disclosures;
    }

    public Object getClaim(String name) {
      return claims.get(name);
    }

    public String getStringClaim(String name) {
      Object value = getClaim(name);
      return value != null ? value.toString() : null;
    }

    public Long getLongClaim(String name) {
      Object value = getClaim(name);
      if (value instanceof Number) {
        return ((Number) value).longValue();
      }
      return null;
    }

    public Boolean getBooleanClaim(String name) {
      Object value = getClaim(name);
      if (value instanceof Boolean) {
        return (Boolean) value;
      }
      return null;
    }

    public Set<String> getClaimNames() {
      return claims.keySet();
    }

    public boolean hasClaim(String name) {
      return claims.containsKey(name);
    }

    @Override
    public String toString() {
      return "SDJWTClaimsSet{" +
          "claims=" + claims +
          ", disclosureCount=" + disclosures.size() +
          '}';
    }
  }
}