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

package org.omnione.did.oid4vc.oid4vp.service;

import org.omnione.did.oid4vc.formatter.oid4vp.verifier.VPTokenVerifier;
import org.omnione.did.oid4vc.oid4vp.config.OID4VPConfig;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLResult;
import org.omnione.did.oid4vc.oid4vp.dto.ServiceResult;
import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.IdentifierResult;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.VerificationConfig;
import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.dcql.core.DCQLCredentialMatcher;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import org.omnione.did.oid4vc.dcql.core.credential.ParsedCredential;
import org.omnione.did.oid4vc.dcql.exception.DCQLException;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.oid4vp.util.crypto.VPTokenEncryptor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OID4VPHelperService {

  public static final String RESPONSE_MODE_DIRECT_POST = "direct_post";
  public static final String RESPONSE_MODE_QUERY = "query";

  public static final String PARAM_STATE = "state";
  public static final String PARAM_ERROR = "error";
  public static final String PARAM_ERROR_DESCRIPTION = "error_description";

  public static final String SESSION_STATUS_CREATED = "CREATED";
  public static final String SESSION_STATUS_REQUEST_FETCHED = "REQUEST_FETCHED";
  public static final String SESSION_STATUS_COMPLETED = "COMPLETED";
  public static final String SESSION_STATUS_FAILED = "FAILED";
  public static final String SESSION_STATUS_EXPIRED = "EXPIRED";

  private final VerifierConfigService configService;
  private final ObjectMapper objectMapper;
  private final List<VPTokenVerifier> credentialVerifiers;
  private final SessionRepository sessionRepository;
  private final VPTokenEncryptor vpTokenEncryptor;

  public static Map<String, Object> createErrorResponse(String errorCode,
      String errorDescription) {
    return Map.of(
        PARAM_ERROR, errorCode,
        PARAM_ERROR_DESCRIPTION, errorDescription
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
    return RESPONSE_MODE_DIRECT_POST.equals(responseMode)
        || RESPONSE_MODE_QUERY.equals(responseMode);
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
    if (value == null || value.isEmpty()) {
      return value;
    }

    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * Parses VP Token JSON string into a structured Map.
   * Uses LinkedHashMap to preserve the order of credential types.
   * Supports both String credentials (e.g., SD-JWT) and Object credentials (e.g., W3C VC JSON-LD).
   *
   * @param vpTokenJson the VP Token JSON string
   * @return Map where key is credential type (dcql_id) and value is list of credentials (String or Object)
   * @throws OID4VPException if JSON parsing fails
   */
  @SuppressWarnings("unchecked")
  public Map<String, List<Object>> parseVPToken(String vpTokenJson) throws OID4VPException {
    log.info("vpTokenJson : " + vpTokenJson);

    if (vpTokenJson == null || vpTokenJson.trim().isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_NULL);
    }

    try {
      return objectMapper.readValue(
          vpTokenJson,
          objectMapper.getTypeFactory().constructMapType(
              LinkedHashMap.class, String.class, List.class)
      );
    } catch (JsonProcessingException e) {
      log.error("Failed to parse VP Token JSON", e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Finds a suitable verifier for the given credential.
   */
  public VPTokenVerifier findVerifier(String credential) {
    for (VPTokenVerifier verifier : credentialVerifiers) {
      if (verifier.supports(credential)) {
        return verifier;
      }
    }
    return null;
  }

  /**
   * Extracts the issuer identifier from the credential.
   * Delegates to the appropriate VPTokenVerifier based on credential format.
   *
   * @param credential the credential string
   * @return IdentifierResult containing type and value, or null if not found
   * @throws OID4VPException if no suitable verifier is found or credential is invalid
   */
  public IdentifierResult extractIssuerIdentifier(String credential) throws OID4VPException {
    if (credential == null || credential.trim().isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL, "Credential cannot be null or empty");
    }

    VPTokenVerifier verifier = findVerifier(credential);
    if (verifier == null) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_NO_VERIFIER_FOUND);
    }

    try {
      return verifier.extractIssuerIdentifier(credential);
    } catch (FormatterException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, e.getErrorMsg(), e);
    }
  }

  /**
   * Extracts the holder identifier from the credential.
   * Delegates to the appropriate VPTokenVerifier based on credential format.
   *
   * @param credential the credential string
   * @return IdentifierResult containing type and value, or null if not found (e.g., no key binding)
   * @throws OID4VPException if no suitable verifier is found or credential is invalid
   */
  public IdentifierResult extractHolderIdentifier(String credential) throws OID4VPException {
    if (credential == null || credential.trim().isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL, "Credential cannot be null or empty");
    }

    VPTokenVerifier verifier = findVerifier(credential);
    if (verifier == null) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_NO_VERIFIER_FOUND);
    }

    try {
      return verifier.extractHolderIdentifier(credential);
    } catch (FormatterException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, e.getErrorMsg(), e);
    }
  }

  /**
   * Extracts all issuer identifiers from the VP Token Map in flat order.
   * Iterates through all credential types and their credentials, extracting
   * the issuer identifier from each credential in the order they appear.
   * Supports both String credentials (e.g., SD-JWT) and Object credentials (e.g., W3C VC JSON-LD).
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @return list of IdentifierResult in flat order matching the credential iteration order
   * @throws OID4VPException if credential format is invalid or serialization fails
   */
  public List<IdentifierResult> extractAllIssuerIdentifiers(Map<String, List<Object>> vpTokenMap) throws OID4VPException {
    if (vpTokenMap == null || vpTokenMap.isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_NULL, "VP Token Map cannot be null or empty");
    }

    List<IdentifierResult> issuerIdentifiers = new ArrayList<>();

    for (String credentialType : vpTokenMap.keySet()) {
      List<Object> credentials = vpTokenMap.get(credentialType);

      for (Object credentialObj : credentials) {
        String credential = convertToString(credentialObj);
        IdentifierResult issuerIdentifier = extractIssuerIdentifier(credential);
        issuerIdentifiers.add(issuerIdentifier);
        log.debug("Extracted issuer identifier: {} (type: {}) from credential type: {}",
            issuerIdentifier != null ? issuerIdentifier.getValue() : null,
            issuerIdentifier != null ? issuerIdentifier.getType() : null,
            credentialType);
      }
    }

    log.info("Extracted {} issuer identifiers from VP Token", issuerIdentifiers.size());
    return issuerIdentifiers;
  }

  /**
   * Extracts all holder identifiers from the VP Token Map in flat order.
   * Iterates through all credential types and their credentials, extracting
   * the holder identifier from each credential in the order they appear.
   * Supports both String credentials (e.g., SD-JWT) and Object credentials (e.g., W3C VC JSON-LD).
   * <p>
   * Note: Holder identifiers may be null for credentials without key binding.
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @return list of IdentifierResult in flat order matching the credential iteration order (may contain nulls)
   * @throws OID4VPException if credential format is invalid or serialization fails
   */
  public List<IdentifierResult> extractAllHolderIdentifiers(Map<String, List<Object>> vpTokenMap) throws OID4VPException {
    if (vpTokenMap == null || vpTokenMap.isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_NULL, "VP Token Map cannot be null or empty");
    }

    List<IdentifierResult> holderIdentifiers = new ArrayList<>();

    for (String credentialType : vpTokenMap.keySet()) {
      List<Object> credentials = vpTokenMap.get(credentialType);

      for (Object credentialObj : credentials) {
        String credential = convertToString(credentialObj);
        IdentifierResult holderIdentifier = extractHolderIdentifier(credential);
        holderIdentifiers.add(holderIdentifier); // may be null
        log.debug("Extracted holder identifier: {} (type: {}) from credential type: {}",
            holderIdentifier != null ? holderIdentifier.getValue() : null,
            holderIdentifier != null ? holderIdentifier.getType() : null,
            credentialType);
      }
    }

    log.info("Extracted {} holder identifiers from VP Token", holderIdentifiers.size());
    return holderIdentifiers;
  }

  /**
   * Converts a credential object to its String representation.
   * If the object is already a String (e.g., SD-JWT or JSON-serialized VP), returns it as-is.
   * If the object is a Map or other JSON structure (e.g., W3C VC JSON-LD), serializes it to JSON string.
   *
   * @param credentialObj the credential object (String or Map)
   * @return the credential as a String
   * @throws OID4VPException if serialization fails
   */
  public String convertToString(Object credentialObj) throws OID4VPException {
    if (credentialObj instanceof String) {
      return (String) credentialObj;
    }
    
    try {
      return objectMapper.writeValueAsString(credentialObj);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize credential object to JSON string", e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL, 
          "Failed to serialize credential: " + e.getMessage(), e);
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

  /**
   * Handles VP Token verification.
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @param issuerPublicKeys the Base64-encoded issuer public keys in flat order
   * @param holderPublicKeys the Base64-encoded holder public keys in flat order (can be null)
   * @param state the state parameter
   * @return ServiceResult containing verification result or error information
   */
  public ServiceResult<Map<String, Object>> handleVPToken(
      Map<String, List<Object>> vpTokenMap,
      List<String> issuerPublicKeys,
      List<String> holderPublicKeys,
      String state) {
    return handleVPToken(vpTokenMap, issuerPublicKeys, holderPublicKeys, null, state);
  }

  /**
   * Handles VP Token verification with X.509 certificate chain support.
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @param issuerPublicKeys the Base64-encoded issuer public keys in flat order (for kid-based verification)
   * @param holderPublicKeys the Base64-encoded holder public keys in flat order (can be null)
   * @param trustedRoots List of trusted root X.509 certificates for x5c-based verification (can be null)
   * @param state the state parameter
   * @return ServiceResult containing verification result or error information
   */
  public ServiceResult<Map<String, Object>> handleVPToken(
      Map<String, List<Object>> vpTokenMap,
      List<String> issuerPublicKeys,
      List<String> holderPublicKeys,
      List<X509Certificate> trustedRoots,
      String state) {

    try {
      if (state != null && !validateStateEntropy(state)) {
        log.warn("State parameter has insufficient entropy");
      }

      Optional<VerificationSession> sessionOpt = sessionRepository.findByState(state);
      if (sessionOpt.isEmpty()) {
        log.warn("No session found for state: {}", state);
        return ServiceResult.failure(400, "invalid_session",
            "No active verification session found", state != null ? state : "");
      }

      VerificationSession session = sessionOpt.get();

      // TTL Check
      if (isSessionExpired(session)) {
        log.warn("Session expired for state: {}", state);
        return ServiceResult.failure(400, "session_expired",
            "This session has expired", state);
      }

      // Session Check
      if (SESSION_STATUS_COMPLETED.equals(session.getStatus())) {
        log.warn("Session already completed for state: {}", state);
        return ServiceResult.failure(400, "session_already_completed",
            "This session has already been successfully processed", state);
      }

      if (SESSION_STATUS_FAILED.equals(session.getStatus())) {
        log.warn("Session already failed for state: {}", state);
        return ServiceResult.failure(400, "session_already_failed",
            "This session has already failed", state);
      }

      if (!SESSION_STATUS_CREATED.equals(session.getStatus())
          && !SESSION_STATUS_REQUEST_FETCHED.equals(session.getStatus())) {
        log.warn("Session not ready for VP Token. Current status: {}", session.getStatus());
        return ServiceResult.failure(400, "invalid_session_state",
            "Session is not ready for VP Token submission", state);
      }

      return processVPTokenWithDCQL(vpTokenMap, issuerPublicKeys, holderPublicKeys, trustedRoots, session);

    } catch (RuntimeException e) {
      log.error("Unexpected error processing VP Token", e);
      return ServiceResult.failure(500, "processing_error",
          "Failed to process VP Token: " + e.getMessage(), state != null ? state : "");
    }
  }

  /**
   * Checks if session has expired and updates status if expired.
   */
  public boolean isSessionExpired(VerificationSession session) {
    OID4VPConfig config = configService.getOID4VPConfig();
    boolean expired = System.currentTimeMillis() - session.getCreatedAt() >
        config.getSession().getSessionTtl();

    if (expired && !SESSION_STATUS_EXPIRED.equals(session.getStatus())) {
      session.setStatus(SESSION_STATUS_EXPIRED);
      session.setExpiresAt(System.currentTimeMillis());
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session expired. Status and expires_at updated for state: {}", session.getState());
    }

    return expired;
  }

  /**
   * Processes VP Token with DCQL verification.
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @param issuerPublicKeys the Base64-encoded issuer public keys in flat order
   * @param holderPublicKeys the Base64-encoded holder public keys in flat order (can be null)
   * @param session the verification session
   * @return ServiceResult containing verification result or error information
   */
  public ServiceResult<Map<String, Object>> processVPTokenWithDCQL(
      Map<String, List<Object>> vpTokenMap,
      List<String> issuerPublicKeys,
      List<String> holderPublicKeys,
      VerificationSession session) {
    return processVPTokenWithDCQL(vpTokenMap, issuerPublicKeys, holderPublicKeys, null, session);
  }

  /**
   * Processes VP Token with DCQL verification and X.509 certificate chain support.
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @param issuerPublicKeys the Base64-encoded issuer public keys in flat order (for kid-based verification)
   * @param holderPublicKeys the Base64-encoded holder public keys in flat order (can be null)
   * @param trustedRoots List of trusted root X.509 certificates for x5c-based verification (can be null)
   * @param session the verification session
   * @return ServiceResult containing verification result or error information
   */
  public ServiceResult<Map<String, Object>> processVPTokenWithDCQL(
      Map<String, List<Object>> vpTokenMap,
      List<String> issuerPublicKeys,
      List<String> holderPublicKeys,
      List<X509Certificate> trustedRoots,
      VerificationSession session) {

    try {
      log.info("Processing VP Token with DCQL for session: {}", session.getState());

      OID4VPConfig config = configService.getOID4VPConfig();

      // Parse DCQL Query for protocol-level validation
      DCQLQuery dcqlQuery = parseDcqlQuery(session.getDcqlQuery());

      List<DCQLQuery.CredentialQuery> dcqlCredentials =
          (dcqlQuery != null && dcqlQuery.getCredentials() != null)
              ? dcqlQuery.getCredentials()
              : List.of();

      int keyIndex = 0;
      int dcqlIndex = 0;

      // Iterate through each credential type
      for (String credentialType : vpTokenMap.keySet()) {

        // Protocol-level validation: Validate DCQL credential ID
        validateDcqlCredentialId(dcqlQuery, credentialType);
        
        List<Object> credentials = vpTokenMap.get(credentialType);

        for (Object credentialObj : credentials) {
          String credential = convertToString(credentialObj);
          log.debug("credential : {}", credential);

          // Find appropriate verifier
          VPTokenVerifier verifier = findVerifier(credential);
          if (verifier == null) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_NO_VERIFIER_FOUND);
          }
          log.debug("verifier format : {}", verifier.getFormat());

          // Check format
          if (dcqlIndex < dcqlCredentials.size()) {
            DCQLQuery.CredentialQuery expectedCredential = dcqlCredentials.get(dcqlIndex);

            if (!credentialType.equals(expectedCredential.getId())) {
              throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_DCQL_ID_MISMATCH,
                  "Position " + dcqlIndex + " - Expected ID: " + expectedCredential.getId() +
                      ", Actual: " + credentialType);
            }

            if (!verifier.getFormat().equals(expectedCredential.getFormat())) {
              throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_FORMAT_MISMATCH,
                  "Position " + dcqlIndex + " - Expected format: " + expectedCredential.getFormat() +
                      ", Actual: " + verifier.getFormat());
            }

            // 3. Meta condition validation
            if (expectedCredential.getMeta() != null && !expectedCredential.getMeta().isEmpty()) {
              try {
                ParsedCredential parsedCredential = DCQLCredentialMatcher.parseCredential(
                    credential, expectedCredential.getFormat());

                if (!DCQLCredentialMatcher.matchesMetadata(parsedCredential, expectedCredential.getMeta())) {
                  throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_META_MISMATCH,
                      "Position " + dcqlIndex + " - Meta condition not satisfied for credential: " + credentialType);
                }
                log.debug("Meta condition validated for credential: {}", credentialType);
              } catch (DCQLException e) {
                throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_META_MISMATCH,
                    "Position " + dcqlIndex + " - Failed to parse credential for meta validation: " + e.getMessage());
              }
            }
          }

          dcqlIndex++;

          // Extract issuer identifier to determine verification method (kid vs x5c)
          IdentifierResult issuerIdentifier = verifier.extractIssuerIdentifier(credential);
          boolean isX5cBased = issuerIdentifier != null
              && issuerIdentifier.getType() == IdentifierResult.Type.SD_JWT_X5C;

          log.debug("Issuer identifier type: {}, isX5cBased: {}",
              issuerIdentifier != null ? issuerIdentifier.getType() : "null", isX5cBased);

          String issuerPublicKey = null;
          String holderPublicKey = null;

          // For kid-based verification, validate and get public keys
          if (!isX5cBased) {
            // Validate key index
            if (issuerPublicKeys == null || keyIndex >= issuerPublicKeys.size()) {
              throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_INSUFFICIENT_KEYS,
                  "Expected at least " + (keyIndex + 1) + " keys, but got " + (issuerPublicKeys == null ? 0 : issuerPublicKeys.size()));
            }

            issuerPublicKey = issuerPublicKeys.get(keyIndex);

            // Get holder public key if available
            if (holderPublicKeys != null && keyIndex < holderPublicKeys.size()) {
              holderPublicKey = holderPublicKeys.get(keyIndex);
            }

            keyIndex++;

            log.info("kid-based verification - issuerPublicKey: {}", issuerPublicKey);
            log.info("kid-based verification - holderPublicKey: {}", holderPublicKey);
          } else {
            log.info("x5c-based verification - using trusted root certificates");
          }

          // 1. Presentation Binding Validation (clientId/nonce O)
          boolean bindingValid = verifier.validatePresentationBinding(credential, config.buildClientId(), session.getNonce());
          if (!bindingValid) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_VERIFICATION_FAILED,
                "Presentation binding validation failed for type: " + credentialType);
          }

          // 2. Signature Validation - branch by identifier type (kid vs x5c)
          boolean signatureValid;
          if (isX5cBased) {
            // X.509 certificate chain based verification
            if (trustedRoots == null || trustedRoots.isEmpty()) {
              throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_VERIFICATION_FAILED,
                  "Trusted root certificates required for x5c-based credential verification");
            }
            signatureValid = verifier.validateSignatureWithX5c(credential, trustedRoots);
          } else {
            // kid-based verification (existing logic)
            signatureValid = verifier.validateSignature(credential, issuerPublicKey, holderPublicKey);
          }

          if (!signatureValid) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_VERIFICATION_FAILED,
                "Signature verification failed for type: " + credentialType);
          }
        }
      }

      if (dcqlIndex != dcqlCredentials.size()) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_CREDENTIAL_COUNT_MISMATCH,
            "Expected " + dcqlCredentials.size() + " credentials, but got " + dcqlIndex);
      }

      log.info("VP Token verification completed successfully");

      // Encrypt and store VP Token
      try {
        String vpTokenJson = objectMapper.writeValueAsString(vpTokenMap);
        String encryptedVpToken = vpTokenEncryptor.encrypt(vpTokenJson);
        session.setVpToken(encryptedVpToken);
        log.debug("VP Token encrypted and stored for session: {}", session.getState());
      } catch (JsonProcessingException e) {
        log.warn("Failed to serialize VP Token, storing without encryption: {}", e.getMessage());
      } catch (RuntimeException e) {
        log.warn("Failed to encrypt VP Token, storing without encryption: {}", e.getMessage());
      }

      session.setStatus(SESSION_STATUS_COMPLETED);
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session status updated to COMPLETED for state: {}", session.getState());

      if (RESPONSE_MODE_DIRECT_POST.equals(session.getResponseMode())
          && session.getClientMetadata() != null) {
        Map<String, Object> metadata = null;
        try {
          metadata = objectMapper.readValue(
              session.getClientMetadata(), Map.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        String callbackUrl = (String) metadata.get("callback_url");
        if (callbackUrl != null) {
          String separator = callbackUrl.contains("?") ? "&" : "?";
          return ServiceResult.success(Map.of(
              "redirect_uri", callbackUrl + separator
                  + "response_code=" + session.getTransactionId()));
        }
      }
      return ServiceResult.success(Map.of());

    } catch (IllegalArgumentException e) {
      log.error("Error processing VP Token", e);

      session.setStatus(SESSION_STATUS_FAILED);
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session status updated to FAILED for state: {}", session.getState());

      return ServiceResult.failure(400, "vp_processing_error",
          "Failed to process VP Token: " + e.getMessage(), session.getState());
    } catch (FormatterException e) {
      log.error("VP Verifier error processing VP Token", e);

      session.setStatus(SESSION_STATUS_FAILED);
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session status updated to FAILED for state: {}", session.getState());

      return ServiceResult.failure(400, "vp_processing_error",
          "Failed to process VP Token: " + e.getErrorMsg(), session.getState());
    } catch (OID4VPException e) {
      log.error("OID4VP error processing VP Token", e);

      session.setStatus(SESSION_STATUS_FAILED);
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session status updated to FAILED for state: {}", session.getState());

      return ServiceResult.failure(400, "vp_processing_error",
          "Failed to process VP Token: " + e.getMessage(), session.getState());
    } catch (RuntimeException e) {
      log.error("Unexpected error processing VP Token", e);

      session.setStatus(SESSION_STATUS_FAILED);
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session status updated to FAILED for state: {}", session.getState());

      return ServiceResult.failure(500, "vp_processing_error",
          "Failed to process VP Token: " + e.getMessage(), session.getState());
    }
  }

  /**
   * Parses DCQL Query JSON string to DCQLQuery object.
   *
   * @param dcqlQueryJson the DCQL Query JSON string
   * @return parsed DCQLQuery object, or null if input is null/empty
   */
  public DCQLQuery parseDcqlQuery(String dcqlQueryJson) throws OID4VPException {
    if (dcqlQueryJson == null || dcqlQueryJson.trim().isEmpty()) {
      log.warn("DCQL Query is null or empty, skipping DCQL validation");
      return null;
    }

    try {
      return objectMapper.readValue(dcqlQueryJson, DCQLQuery.class);
    } catch (JsonProcessingException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_DCQL_PARSE_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Validates that the credential type (response key) exists in DCQL Query's credential IDs.
   * This is a protocol-level validation, separate from format-level credential verification.
   *
   * @param dcqlQuery the parsed DCQL Query
   * @param credentialType the credential type from VP Token response (dcql_id)
   * @throws RuntimeException if validation fails
   */
  public void validateDcqlCredentialId(DCQLQuery dcqlQuery, String credentialType)
      throws OID4VPException {
    if (dcqlQuery == null) {
      log.debug("Skipping DCQL ID validation - no DCQL Query provided");
      return;
    }

    if (dcqlQuery.getCredentials() == null || dcqlQuery.getCredentials().isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_DCQL_NO_CREDENTIALS);
    }

    boolean dcqlIdExists = dcqlQuery.getCredentials().stream()
        .map(DCQLQuery.CredentialQuery::getId)
        .anyMatch(id -> id.equals(credentialType));

    if (!dcqlIdExists) {
      List<String> validIds = dcqlQuery.getCredentials().stream()
          .map(DCQLQuery.CredentialQuery::getId)
          .toList();
      log.error("DCQL ID mismatch. Expected one of: {}, but got: {}", validIds, credentialType);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_DCQL_ID_MISMATCH,
          "DCQL ID '" + credentialType + "' not found. Valid IDs: " + validIds);
    }

    log.debug("DCQL ID validation passed: {}", credentialType);
  }
}