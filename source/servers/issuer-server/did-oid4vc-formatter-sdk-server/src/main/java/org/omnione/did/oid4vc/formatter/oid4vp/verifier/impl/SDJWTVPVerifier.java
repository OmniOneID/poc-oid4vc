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

package org.omnione.did.oid4vc.formatter.oid4vp.verifier.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.oid4vc.formatter.exception.FormatterErrorCode;
import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.VPTokenVerifier;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.IdentifierResult;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.VerificationConfig;
import org.omnione.did.oid4vc.formatter.util.KeyUtil;
import org.omnione.did.sdjwt.core.oid4vp.SDJWTVerifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SDJWTVPVerifier implements VPTokenVerifier {
  public static final String FORMAT_TYPE = "dc+sd-jwt";

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean supports(String credential) {
    if (credential == null || credential.trim().isEmpty()) {
      return false;
    }

    // SD-JWT must contain '~' separator
    if (!credential.contains("~")) {
      return false;
    }

    // Extract the Issuer-signed JWT part (before first '~')
    String[] parts = credential.split("~", 2);
    String issuerJwt = parts[0];

    // Issuer-signed JWT must be valid JWT format (3 parts separated by '.')
    String[] jwtParts = issuerJwt.split("\\.");
    if (jwtParts.length != 3) {
      return false;
    }

    // Validate JWT header contains SD-JWT type
    try {
      String headerJson = new String(Base64.getUrlDecoder().decode(jwtParts[0]),
          StandardCharsets.UTF_8);

      // Check if header contains "sd-jwt" type indicator
      // typ: "dc+sd-jwt" or "sd-jwt" or contains "_sd_alg"
      return headerJson.contains("dc+sd-jwt")
          || headerJson.contains("vc+sd-jwt")
          || headerJson.contains("\"sd-jwt\"")  // generic
          || headerJson.contains("_sd_alg");
    } catch (IllegalArgumentException e) {
      log.debug("Failed to parse JWT header: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public String getFormat() {
    return FORMAT_TYPE;
  }

  @Override
  public boolean verifyVerifiablePresentation(String credential, VerificationConfig config) throws FormatterException {
    try {
      PublicKey issuerPublicKey = KeyUtil.getPublicKeyObject(
          KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(config.getIssuerPublicKey()))
      );

      // Build holder public key if provided externally
      PublicKey holderPublicKey = null;
      if (config.getHolderPublicKey() != null
          && !config.getHolderPublicKey().trim().isEmpty()) {
        holderPublicKey = KeyUtil.getPublicKeyObject(
            KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(config.getHolderPublicKey()))
        );
        log.debug("Using externally provided holder public key for Key Binding verification");
      }

      SDJWTVerifier verifier = new SDJWTVerifier(issuerPublicKey, holderPublicKey)
          .requireExpiration(true)
          .requireIssuedAt(true)
          .requireKeyBinding(true);

      SDJWTVerifier.SDJWTClaimsSet claimsSet = verifier.verify(
          credential,
          config.getClientId(),
          config.getNonce()
      );

      Map<String, Object> claims = claimsSet.getClaims();

      // Log verified claims (excluding internal fields)
      log.info("SD-JWT Verification Result:");
      claims.forEach((key, value) -> {
        if (!key.startsWith("_") && !key.equals("iss") && !key.equals("iat") &&
            !key.equals("exp") && !key.equals("vct") && !key.equals("cnf")) {
          log.info("     {}: {}", key, value);
        }
      });

      return true;

    } catch (IllegalArgumentException e) {
      log.error("SD-JWT verification failed: invalid input", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    } catch (IllegalStateException e) {
      log.error("SD-JWT verification failed: algorithm not available", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    } catch (RuntimeException e) {
      log.error("SD-JWT verification failed", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Extracts the issuer identifier from the SD-JWT credential.
   * The identifier is extracted from the header of the Issuer-signed JWT (first part before '~').
   * 
   * Extraction priority:
   * 1. kid (Key ID) from JWT header
   * 2. x5c (X.509 Certificate Chain) - returns SHA-256 thumbprint of first certificate
   *
   * @param credential the SD-JWT credential string
   * @return IdentifierResult containing type and value, or null if neither kid nor x5c is present
   * @throws FormatterException if the credential format is invalid
   */
  @Override
  public IdentifierResult extractIssuerIdentifier(String credential) throws FormatterException {
    if (credential == null || credential.trim().isEmpty()) {
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_NULL, "Credential cannot be null or empty");
    }

    // Extract the Issuer-signed JWT part (before first '~')
    String issuerJwt = credential.split("~", 2)[0];

    // Split JWT into header, payload, signature
    String[] jwtParts = issuerJwt.split("\\.");
    if (jwtParts.length != 3) {
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL, "Invalid JWT format in SD-JWT credential");
    }

    try {
      String headerJson = new String(
          Base64.getUrlDecoder().decode(jwtParts[0]),
          StandardCharsets.UTF_8
      );

      Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);

      // 1. kid first
      Object kid = header.get("kid");
      if (kid != null) {
        return new IdentifierResult(IdentifierResult.Type.SD_JWT_KID, kid.toString());
      }

      // 2. x5c fallback → calculate SHA-256 thumbprint
      Object x5c = header.get("x5c");
      if (x5c instanceof List && !((List<?>) x5c).isEmpty()) {
        String firstCert = ((List<?>) x5c).get(0).toString();
        String thumbprint = KeyUtil.calculateX5tS256(firstCert);
        return new IdentifierResult(IdentifierResult.Type.SD_JWT_X5C, thumbprint);
      }

      return null;

    } catch (IllegalArgumentException e) {
      log.error("Failed to decode Base64 JWT header: {}", e.getMessage());
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, "Failed to decode JWT header: " + e.getMessage(), e);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JWT header JSON: {}", e.getMessage());
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, "Failed to parse JWT header: " + e.getMessage(), e);
    }
  }

  /**
   * Extracts the holder identifier from the SD-JWT credential.
   * The holder identifier is extracted from the cnf (confirmation) claim in the JWT payload.
   * 
   * Extraction priority:
   * 1. cnf.kid (Key ID in confirmation claim)
   * 2. cnf.x5c (X.509 Certificate Chain) - returns SHA-256 thumbprint of first certificate
   * <p>
   * Note: Per RFC 9901 (SD-JWT specification), Key Binding is OPTIONAL.
   * The cnf claim is only present when the Issuer wants to enable Key Binding.
   * Therefore, this method may return null for valid SD-JWT credentials without key binding.
   *
   * @param credential the SD-JWT credential string
   * @return IdentifierResult containing type and value, or null if cnf is not present
   * @throws FormatterException if the credential format is invalid
   */
  @Override
  public IdentifierResult extractHolderIdentifier(String credential) throws FormatterException {
    if (credential == null || credential.trim().isEmpty()) {
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_NULL, "Credential cannot be null or empty");
    }

    // Extract the Issuer-signed JWT part (before first '~')
    String issuerJwt = credential.split("~", 2)[0];

    // Split JWT into header, payload, signature
    String[] jwtParts = issuerJwt.split("\\.");
    if (jwtParts.length != 3) {
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL, "Invalid JWT format in SD-JWT credential");
    }

    try {
      String payloadJson = new String(
          Base64.getUrlDecoder().decode(jwtParts[1]),
          StandardCharsets.UTF_8
      );

      Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
      Object cnf = payload.get("cnf");

      if (cnf instanceof Map) {
        Map<String, Object> cnfMap = (Map<String, Object>) cnf;

        // 1. cnf.kid first
        Object kid = cnfMap.get("kid");
        if (kid != null) {
          return new IdentifierResult(IdentifierResult.Type.SD_JWT_KID, kid.toString());
        }

        // 2. cnf.x5c fallback → calculate SHA-256 thumbprint
        Object x5c = cnfMap.get("x5c");
        if (x5c instanceof List && !((List<?>) x5c).isEmpty()) {
          String firstCert = ((List<?>) x5c).get(0).toString();
          String thumbprint = KeyUtil.calculateX5tS256(firstCert);
          return new IdentifierResult(IdentifierResult.Type.SD_JWT_X5C, thumbprint);
        }
      }

      return null;

    } catch (IllegalArgumentException e) {
      log.error("Failed to decode Base64 JWT payload: {}", e.getMessage());
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, "Failed to decode JWT payload: " + e.getMessage(), e);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JWT payload JSON: {}", e.getMessage());
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, "Failed to parse JWT payload: " + e.getMessage(), e);
    }
  }

  // ========================================================================================
  // Fine-grained verification methods
  // ========================================================================================
  // The following methods provide fine-grained control over the verification process,
  // allowing third-party implementations to separate signature verification from
  // presentation binding validation when needed.
  //
  // While verifyVerifiablePresentation() performs complete OID4VP VP Token verification
  // in a single call, some environments may require these steps to be separated.
  // ========================================================================================

  /**
   * Extracts the bound client ID (audience) from the Key Binding JWT in the SD-JWT credential.
   * The Key Binding JWT is the last part after the final '~' separator.
   *
   * @param credential the SD-JWT credential string
   * @return the aud claim value from Key Binding JWT, or null if not present
   */
  //@Override
  public String extractBoundClientId(String credential) {
    if (credential == null || credential.trim().isEmpty()) {
      return null;
    }

    // SD-JWT format: <issuer-jwt>~<disclosure1>~<disclosure2>~...~<kb-jwt>
    int lastTildeIndex = credential.lastIndexOf('~');
    if (lastTildeIndex == -1 || lastTildeIndex == credential.length() - 1) {
      log.debug("No Key Binding JWT found in SD-JWT credential");
      return null;
    }

    String kbJwt = credential.substring(lastTildeIndex + 1);
    if (kbJwt.isEmpty()) {
      log.debug("Empty Key Binding JWT");
      return null;
    }

    String[] jwtParts = kbJwt.split("\\.");
    if (jwtParts.length != 3) {
      log.debug("Invalid Key Binding JWT format: expected 3 parts, got {}", jwtParts.length);
      return null;
    }

    try {
      String payloadJson = new String(
          Base64.getUrlDecoder().decode(jwtParts[1]),
          StandardCharsets.UTF_8
      );
      Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
      Object aud = payload.get("aud");
      return aud != null ? aud.toString() : null;
    } catch (IllegalArgumentException e) {
      log.debug("Failed to decode Key Binding JWT payload: {}", e.getMessage());
      return null;
    } catch (JsonProcessingException e) {
      log.debug("Failed to parse Key Binding JWT payload JSON: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Extracts the bound nonce from the Key Binding JWT in the SD-JWT credential.
   * The Key Binding JWT is the last part after the final '~' separator.
   *
   * @param credential the SD-JWT credential string
   * @return the nonce claim value from Key Binding JWT, or null if not present
   */
  //@Override
  public String extractBoundNonce(String credential) {
    if (credential == null || credential.trim().isEmpty()) {
      return null;
    }

    // SD-JWT format: <issuer-jwt>~<disclosure1>~<disclosure2>~...~<kb-jwt>
    int lastTildeIndex = credential.lastIndexOf('~');
    if (lastTildeIndex == -1 || lastTildeIndex == credential.length() - 1) {
      log.debug("No Key Binding JWT found in SD-JWT credential");
      return null;
    }

    String kbJwt = credential.substring(lastTildeIndex + 1);
    if (kbJwt.isEmpty()) {
      log.debug("Empty Key Binding JWT");
      return null;
    }

    String[] jwtParts = kbJwt.split("\\.");
    if (jwtParts.length != 3) {
      log.debug("Invalid Key Binding JWT format: expected 3 parts, got {}", jwtParts.length);
      return null;
    }

    try {
      String payloadJson = new String(
          Base64.getUrlDecoder().decode(jwtParts[1]),
          StandardCharsets.UTF_8
      );
      Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
      Object nonce = payload.get("nonce");
      return nonce != null ? nonce.toString() : null;
    } catch (IllegalArgumentException e) {
      log.debug("Failed to decode Key Binding JWT payload: {}", e.getMessage());
      return null;
    } catch (JsonProcessingException e) {
      log.debug("Failed to parse Key Binding JWT payload JSON: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Validates that the SD-JWT credential's Key Binding JWT contains the expected client ID and nonce.
   *
   * @param credential the SD-JWT credential string
   * @param expectedClientId the expected aud claim value
   * @param expectedNonce the expected nonce claim value
   * @return true if both values match, false otherwise
   */
  //@Override
  public boolean validatePresentationBinding(String credential, String expectedClientId, String expectedNonce) {
    String boundClientId = extractBoundClientId(credential);
    String boundNonce = extractBoundNonce(credential);

    if (boundClientId == null || boundNonce == null) {
      log.debug("Key Binding JWT not found or missing aud/nonce claims");
      return false;
    }

    boolean clientIdMatch = expectedClientId != null && expectedClientId.equals(boundClientId);
    boolean nonceMatch = expectedNonce != null && expectedNonce.equals(boundNonce);

    if (!clientIdMatch) {
      log.debug("Client ID mismatch: expected={}, actual={}", expectedClientId, boundClientId);
    }
    if (!nonceMatch) {
      log.debug("Nonce mismatch: expected={}, actual={}", expectedNonce, boundNonce);
    }

    return clientIdMatch && nonceMatch;
  }

  /**
   * Validates the signatures of the SD-JWT credential without checking presentation binding (aud/nonce).
   * This method verifies:
   * 1. Issuer-signed JWT signature using the provided issuer public key
   * 2. Key Binding JWT signature using the provided holder public key (if KB-JWT exists)
   *
   * @param credential the SD-JWT credential string
   * @param issuerPublicKey Base64-encoded compressed issuer public key
   * @param holderPublicKey Base64-encoded compressed holder public key (can be null if no KB-JWT)
   * @return true if all signatures are valid
   * @throws FormatterException if signature verification fails
   */
  //@Override
  public boolean validateSignature(String credential, String issuerPublicKey, String holderPublicKey) throws FormatterException {
    try {
      PublicKey issuerPubKey = KeyUtil.getPublicKeyObject(
          KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(issuerPublicKey))
      );

      PublicKey holderPubKey = null;
      if (holderPublicKey != null && !holderPublicKey.trim().isEmpty()) {
        holderPubKey = KeyUtil.getPublicKeyObject(
            KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(holderPublicKey))
        );
      }

      SDJWTVerifier verifier = new SDJWTVerifier(issuerPubKey, holderPubKey)
          .requireExpiration(true)
          .requireIssuedAt(true)
          .requireKeyBinding(holderPubKey != null);

      // Verify with null aud/nonce to skip presentation binding check
      verifier.verify(credential, null, null);

      log.debug("SD-JWT signature validation successful");
      return true;

    } catch (IllegalArgumentException e) {
      log.error("SD-JWT signature validation failed: invalid input", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    } catch (IllegalStateException e) {
      log.error("SD-JWT signature validation failed: algorithm not available", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    } catch (RuntimeException e) {
      log.error("SD-JWT signature validation failed", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Validates the signatures of the SD-JWT credential using X.509 certificate chain validation.
   * This method verifies:
   * 1. Issuer-signed JWT signature by validating the x5c certificate chain against trusted roots
   * 2. Key Binding JWT signature (automatically handled by SDK)
   *
   * @param credential the SD-JWT credential string
   * @param trustedRoots List of trusted root X.509 certificates for chain validation
   * @return true if all signatures are valid
   * @throws FormatterException if signature verification fails
   */
  @Override
  public boolean validateSignatureWithX5c(String credential, List<X509Certificate> trustedRoots) throws FormatterException {
    try {
      // verifyWithX5c is a static method that validates:
      // - Issuer JWT signature using x5c certificate chain against trusted roots
      // - Key Binding JWT signature (automatically handled)
      SDJWTVerifier.verifyWithX5c(credential, trustedRoots);

      log.debug("SD-JWT X.509 signature validation successful");
      return true;

    } catch (Exception e) {
      log.error("SD-JWT X.509 signature validation failed", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    }
  }

}