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

package com.example.oid4vc.verifier.util;

import com.example.oid4vc.verifier.configuration.OID4VPProperties;
import com.example.oid4vc.verifier.dto.DCQLResult;
import com.example.oid4vc.verifier.dto.VerificationSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Security;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.oid4vp.core.SDJWTVerifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OID4VPHelper {
  public static final String RESPONSE_MODE_DIRECT_POST = "direct_post";
  public static final String RESPONSE_MODE_QUERY = "query";
  //public static final String RESPONSE_MODE_FRAGMENT = "fragment";

  public static final String PARAM_STATE = "state";
  public static final String PARAM_ERROR = "error";
  public static final String PARAM_ERROR_DESCRIPTION = "error_description";

  private final OID4VPProperties oid4vpProperties;
  private final ObjectMapper objectMapper;

  public static Map<String, Object> createErrorResponse(String errorCode, String errorDescription) {
    return Map.of(
        PARAM_ERROR, errorCode,
        PARAM_ERROR_DESCRIPTION, errorDescription
    );
  }

  public Map<String, Object> createErrorResponseWithState(String errorCode, String errorDescription,
      String state) {
    return Map.of(
        PARAM_ERROR, errorCode,
        PARAM_ERROR_DESCRIPTION, errorDescription,
        PARAM_STATE, state != null ? state : ""
    );
  }

  public static boolean isEmptyParam(String param) {
    return param == null || param.trim().isEmpty();
  }

  public static boolean areBothParamsMissing(String dcql_query, String scope) {
    return isEmptyParam(dcql_query) && isEmptyParam(scope);
  }

  public static boolean areBothParamsProvided(String dcql_query, String scope) {
    return !isEmptyParam(dcql_query) && !isEmptyParam(scope);
  }

  public static boolean requiresResponseUri(String responseMode) {
    return RESPONSE_MODE_DIRECT_POST.equals(responseMode) || RESPONSE_MODE_QUERY.equals(
        responseMode);
  }

  public String compactJsonString(String jsonString) {
    if (jsonString == null || jsonString.trim().isEmpty()) {
      return jsonString;
    }

    try {
      Object jsonObject = objectMapper.readValue(jsonString, Object.class);
      return objectMapper.writeValueAsString(jsonObject);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Failed to compact JSON string, returning original: {}", e.getMessage());
      return jsonString;
    }
  }

  public String generateSecureState() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] randomBytes = new byte[16];
    secureRandom.nextBytes(randomBytes);

    String secureState = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(randomBytes);

    log.debug("Generated secure state with {} bits entropy", randomBytes.length * 8);
    return secureState;
  }

  public boolean validateStateEntropy(String state) {
    if (state == null || state.trim().isEmpty()) {
      return false;
    }

    if (state.length() < 22) {
      log.warn("State parameter has insufficient entropy: length={}", state.length());
      return false;
    }

    return true;
  }

  public String encodeValue(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }

  public ResponseEntity<Map<String, Object>> processVPTokenWithDCQL(
      String vpTokenMap,
      VerificationSession session) {

    try {
      log.info("Processing VP Token with DCQL for session: {}", session.getState());

      String publicKeyIssuerStr = oid4vpProperties.getCrypto().getIssuerPublicKey();
      String publicKeyHolderStr = oid4vpProperties.getCrypto().getIssuerPublicKey();

      String audience = oid4vpProperties.getCrypto().getDefaultAudience();
      String nonce = oid4vpProperties.getCrypto().getDefaultNonce();

      PublicKey issuerPublicKey = getPublicKeyObject(
          unCompressPublicKey(Base64.getDecoder().decode(publicKeyIssuerStr))
      );
      PublicKey holderPublicKey = getPublicKeyObject(
          unCompressPublicKey(Base64.getDecoder().decode(publicKeyHolderStr))
      );

      Map<String, Object> vpTokenMapObj = objectMapper.readValue(vpTokenMap, Map.class);

      for (String credentialType : vpTokenMapObj.keySet()) {
        List<String> credentials = (List<String>) vpTokenMapObj.get(credentialType);

        for (String vpToken : credentials) {
          if (isSDJWTFormat(vpToken)) {
            SDJWTVerifier verifier = new SDJWTVerifier(issuerPublicKey, holderPublicKey);
            SDJWTVerifier.SDJWTClaimsSet claims = verifier.verify(
                vpToken,
                audience,
                nonce
            );

            log.info("VP Verification Result:");
            claims.getClaims().forEach((key, value) -> {
              if (!key.startsWith("_") && !key.equals("iss") && !key.equals("iat") &&
                  !key.equals("exp") && !key.equals("vct") && !key.equals("cnf")) {
                log.info("     " + key + ": " + value);
              }
            });

            // TODO: The validation time check must be implemented.
          } else {
            throw new RuntimeException("Invalid VP Token: " + vpToken);
          }
        }
      }

      Map<String, Object> result = Map.of(
          "status", "success",
          "vp_token_validated", true
      );

      session.setVerificationResult(result);

      return ResponseEntity.ok(Map.of(
          //"status", "success",
          //"state", session.getState(),
          //"redirect_uri", oid4vpProperties.getSuccessUrl() + "?session_id=" + session.getState()
      ));

    } catch (JsonProcessingException | IllegalArgumentException e) {
      log.error("Error processing VP Token", e);
      return ResponseEntity.ok(Map.of(
          "error", "vp_processing_error",
          "error_description", "Failed to process VP Token: " + e.getMessage(),
          "state", session.getState()
      ));
    }
  }

  public DCQLResult convertValidationResult(
      DCQLQueryValidator.ValidationResult validationResult,
      DCQLQuery dcqlQuery) {

    int credentialCount = 0;
    if (dcqlQuery.getCredentials() != null) {
      credentialCount = dcqlQuery.getCredentials().size();
    }

    String message;
    if (validationResult.isValid()) {
      message = validationResult.hasWarnings()
          ? "DCQL validation successful with warnings: " + String.join(", ",
          validationResult.getWarnings())
          : "DCQL validation successful";
    } else {
      message = "DCQL validation failed: " + String.join(", ", validationResult.getErrors());
    }

    return DCQLResult.builder()
        .valid(validationResult.isValid())
        .success(validationResult.isValid())
        .message(message)
        .credentialCount(credentialCount)
        .build();
  }

  public boolean isSDJWTFormat(String input) {
    if (input == null || input.trim().isEmpty()) {
      return false;
    }

    return input.contains("~");
  }

  public ResponseEntity<Map<String, Object>> handleVPToken(
      String vpToken,
      String state,
      Map<String, VerificationSession> sessions) {

    try {
      if (state != null && !validateStateEntropy(state)) {
        log.warn("State parameter has insufficient entropy");
        return ResponseEntity.ok(Map.of(
            "error", "insufficient_entropy",
            "error_description", "State parameter has insufficient entropy for replay protection",
            "state", state
        ));
      }

      VerificationSession session = sessions.get(state);
      if (session == null) {
        log.warn("No session found for state: {}", state);
        return ResponseEntity.ok(Map.of(
            "error", "invalid_session",
            "error_description", "No active verification session found",
            "state", state != null ? state : ""
        ));
      }

      return processVPTokenWithDCQL(vpToken, session);

    } catch (RuntimeException e) {
      log.error("Unexpected error processing VP Token", e);
      return ResponseEntity.ok(Map.of(
          "error", "processing_error",
          "error_description", "Failed to process VP Token: " + e.getMessage(),
          "state", state != null ? state : ""
      ));
    }
  }

  public static PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
      return keyFactory.generatePublic(publicKeySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to generate PublicKey object", e);
    }
  }

  public static byte[] unCompressPublicKey(byte[] compressedPublicKey) {
    try {
      ECNamedCurveParameterSpec ecParams = ECNamedCurveTable.getParameterSpec("Secp256r1");
      ECPoint uncompressedPoint = ecParams.getCurve().decodePoint(compressedPublicKey);
      ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(uncompressedPoint, ecParams);

      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return keyFactory.generatePublic(pubKeySpec).getEncoded();
    } catch (java.security.spec.InvalidKeySpecException |
             java.security.NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to uncompress public key", e);
    }
  }

  static {
    Security.addProvider(new BouncyCastleProvider());
  }
}