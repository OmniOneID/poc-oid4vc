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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;
import org.omnione.did.sdk.sdjwt.crypto.JWSSigner;
import org.omnione.did.sdk.sdjwt.crypto.SignedJWT;
import org.omnione.did.sdk.sdjwt.crypto.impl.ECDSASigner;
import org.omnione.did.sdk.sdjwt.crypto.impl.RSASSASigner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyBindingJWTBuilder {

  private final PrivateKey holderKey;
  private String audience;
  private String nonce;
  private Instant issuedAt;
  private List<String> x5cChain;
  private final Map<String, Object> additionalClaims;

  public KeyBindingJWTBuilder(PrivateKey holderKey) {
    if (holderKey == null) {
      throw new SDJWTException("Holder private key cannot be null");
    }
    this.holderKey = holderKey;
    this.additionalClaims = new HashMap<>();
    this.issuedAt = Instant.now();
  }

  public KeyBindingJWTBuilder audience(String audience) {
    this.audience = audience;
    return this;
  }

  public KeyBindingJWTBuilder nonce(String nonce) {
    this.nonce = nonce;
    return this;
  }

  public KeyBindingJWTBuilder issuedAt(Instant issuedAt) {
    this.issuedAt = issuedAt;
    return this;
  }

  public KeyBindingJWTBuilder claim(String name, Object value) {
    additionalClaims.put(name, value);
    return this;
  }

  public KeyBindingJWTBuilder x5c(List<String> x5cChain) {
    this.x5cChain = x5cChain;
    return this;
  }

  public String build() throws SDJWTException {
    try {

      String algorithm;
      JWSSigner signer;

      if (holderKey instanceof RSAPrivateKey) {
        algorithm = "RS256";
        signer = new RSASSASigner((RSAPrivateKey) holderKey);
      } else if (holderKey instanceof ECPrivateKey) {
        algorithm = "ES256";
        signer = new ECDSASigner((ECPrivateKey) holderKey);
      } else {
        throw new SDJWTException("Unsupported private key type: " + holderKey.getClass());
      }

      Map<String, Object> header = new HashMap<>();
      header.put("alg", algorithm);
      header.put("typ", "kb+jwt");
      if (x5cChain != null && !x5cChain.isEmpty()) {
        header.put("x5c", x5cChain);
      }

      Map<String, Object> claims = new HashMap<>();

      if (audience != null) {
        claims.put("aud", audience);
      }

      if (nonce != null) {
        claims.put("nonce", nonce);
      }

      if (issuedAt != null) {
        claims.put("iat", issuedAt.getEpochSecond());
      }

      claims.putAll(additionalClaims);

      SignedJWT signedJWT = new SignedJWT(header, claims);
      signedJWT.sign(signer);

      return signedJWT.serialize();
    } catch (SDJWTException e) {
      throw e;
    }
  }

  public static String createKeyBindingJWT(PrivateKey holderKey, String audience, String nonce)
      throws SDJWTException {
    return new KeyBindingJWTBuilder(holderKey)
        .audience(audience)
        .nonce(nonce)
        .build();
  }

  public static String createKeyBindingJWT(PrivateKey holderKey, String audience, String nonce,
      String sdJwtString) throws SDJWTException {
    String sdHash = calculateSdHash(sdJwtString);

    return new KeyBindingJWTBuilder(holderKey)
        .audience(audience)
        .nonce(nonce)
        .claim("sd_hash", sdHash)
        .build();
  }

  public static String createKeyBindingJWT(PrivateKey holderKey, List<String> x5cChain,
      String audience, String nonce) throws SDJWTException {
    return new KeyBindingJWTBuilder(holderKey)
        .x5c(x5cChain)
        .audience(audience)
        .nonce(nonce)
        .build();
  }

  public static String createKeyBindingJWT(PrivateKey holderKey, List<String> x5cChain,
      String audience, String nonce, String sdJwtString) throws SDJWTException {
    String sdHash = calculateSdHash(sdJwtString);

    return new KeyBindingJWTBuilder(holderKey)
        .x5c(x5cChain)
        .audience(audience)
        .nonce(nonce)
        .claim("sd_hash", sdHash)
        .build();
  }

  private static String calculateSdHash(String sdJwtString) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(sdJwtString.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new SDJWTException("SHA-256 algorithm not available", e);
    }
  }
}
