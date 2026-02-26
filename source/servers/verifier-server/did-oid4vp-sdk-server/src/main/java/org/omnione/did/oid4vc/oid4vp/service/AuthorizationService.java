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

import org.omnione.did.oid4vc.oid4vp.config.OID4VPConfig;
import org.omnione.did.oid4vc.oid4vp.dto.ServiceResult;
import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.CompactSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.impl.ECDSASigner;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.SignedJWT;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.JWSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.util.jwk.JwkUtils;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

  private static final String CONTENT_TYPE_JWT = "application/oauth-authz-req+jwt";
  private static final String RESPONSE_TYPE_VP_TOKEN = "vp_token";
  private static final String ERROR_INVALID_REQUEST = "invalid_request";
  private static final String ERROR_INVALID_REQUEST_URI = "invalid_request_uri";
  private static final String ERROR_PROCESSING_ERROR = "processing_error";
  private static final String PARAM_STATE = "state";

  private final VerifierConfigService configService;
  private final ObjectMapper objectMapper;
  private final ClientMetadataService clientMetadataService;
  private final OID4VPHelperService oid4VPHelperService;
  private final SessionRepository sessionRepository;

  /**
   * Retrieves authorization request as JWS format (RFC9101 compliant).
   *
   * @param requestId the request identifier
   * @param compactSigner the compact signer (optional)
   * @param verificationMethod verificationMethod
   * @param publicKeyMultibase the verifier's public key in multibase format for JWK header
   * @return ServiceResult containing JWS string or error information
   */
  public ServiceResult<String> getAuthorizationRequest(
      String requestId, CompactSigner compactSigner, String verificationMethod, String publicKeyMultibase) {

    log.info("Retrieving authorization request (JWS) for request_id: {}", requestId);

    try {
      Optional<VerificationSession> sessionOpt = sessionRepository.findByRequestId(requestId);

      if (sessionOpt.isEmpty()) {
        log.warn("Authorization request not found for request_id: {}", requestId);
        return ServiceResult.notFound(ERROR_INVALID_REQUEST_URI,
            "The request_uri is invalid or has expired");
      }

      VerificationSession session = sessionOpt.get();

      if (isSessionExpired(session)) {
        log.warn("Authorization request expired for request_id: {}", requestId);
        return ServiceResult.notFound(ERROR_INVALID_REQUEST_URI, "The request_uri has expired");
      }

      if (session.getRequestUriFetchedAt() != null) {
        log.warn("Authorization request already fetched for request_id: {}", requestId);
        return ServiceResult.badRequest(ERROR_INVALID_REQUEST_URI,
            "The request_uri has already been used");
      }

      Map<String, Object> authRequest = buildAuthorizationRequest(session);
      String jws = createSignedAuthorizationRequest(authRequest, compactSigner, verificationMethod, publicKeyMultibase);

      session.setRequestUriFetchedAt(System.currentTimeMillis());
      session.setStatus(OID4VPHelperService.SESSION_STATUS_REQUEST_FETCHED);
      sessionRepository.saveByState(session.getState(), session);

      log.info("Successfully retrieved authorization request (JWS) for request_id: {}", requestId);
      return ServiceResult.success(jws, CONTENT_TYPE_JWT);

    } catch (OID4VPException e) {
      log.error("Failed to sign authorization request", e);
      return ServiceResult.serverError(ERROR_INVALID_REQUEST, "Failed to sign authorization request");
    } catch (JsonProcessingException e) {
      log.error("Failed to process JSON", e);
      return ServiceResult.serverError(ERROR_PROCESSING_ERROR, "Failed to process authorization request");
    }
  }

  /**
   * Retrieves authorization request as JWS format (RFC9101 compliant).
   * Uses PrivateKey directly for signing.
   *
   * @param requestId the request identifier
   * @param privateKey the private key for signing
   * @param verificationMethod verificationMethod
   * @param publicKeyMultibase the verifier's public key in multibase format for JWK header
   * @return ServiceResult containing JWS string or error information
   */
  public ServiceResult<String> getAuthorizationRequest(
      String requestId, PrivateKey privateKey, String verificationMethod, String publicKeyMultibase) {

    log.info("Retrieving authorization request (JWS) for request_id: {} using PrivateKey", requestId);

    try {
      Optional<VerificationSession> sessionOpt = sessionRepository.findByRequestId(requestId);

      if (sessionOpt.isEmpty()) {
        log.warn("Authorization request not found for request_id: {}", requestId);
        return ServiceResult.notFound(ERROR_INVALID_REQUEST_URI,
            "The request_uri is invalid or has expired");
      }

      VerificationSession session = sessionOpt.get();

      if (isSessionExpired(session)) {
        log.warn("Authorization request expired for request_id: {}", requestId);
        return ServiceResult.notFound(ERROR_INVALID_REQUEST_URI, "The request_uri has expired");
      }

      if (session.getRequestUriFetchedAt() != null) {
        log.warn("Authorization request already fetched for request_id: {}", requestId);
        return ServiceResult.badRequest(ERROR_INVALID_REQUEST_URI,
            "The request_uri has already been used");
      }

      Map<String, Object> authRequest = buildAuthorizationRequest(session);
      String jws = createSignedAuthorizationRequest(authRequest, privateKey, verificationMethod, publicKeyMultibase);

      session.setRequestUriFetchedAt(System.currentTimeMillis());
      session.setStatus(OID4VPHelperService.SESSION_STATUS_REQUEST_FETCHED);
      sessionRepository.saveByState(session.getState(), session);

      log.info("Successfully retrieved authorization request (JWS) for request_id: {}", requestId);
      return ServiceResult.success(jws, CONTENT_TYPE_JWT);

    } catch (OID4VPException e) {
      log.error("Failed to sign authorization request", e);
      return ServiceResult.serverError(ERROR_INVALID_REQUEST, "Failed to sign authorization request");
    } catch (JsonProcessingException e) {
      log.error("Failed to process JSON", e);
      return ServiceResult.serverError(ERROR_PROCESSING_ERROR, "Failed to process authorization request");
    }
  }

  /**
   * Handles response from verifiable presentation.
   *
   * @param vpTokenMap the parsed VP Token Map (credential type -> list of credentials)
   * @param issuerPublicKeys the Base64-encoded issuer public keys in flat order
   * @param holderPublicKeys the Base64-encoded holder public keys in flat order (can be null)
   * @param state the state parameter
   * @param error the error code (if any)
   * @param errorDescription the error description (if any)
   * @param httpMethod the HTTP method used for the request
   * @return ServiceResult containing response data or error information
   */
  public ServiceResult<Map<String, Object>> receiveResponse(
      Map<String, List<Object>> vpTokenMap,
      List<String> issuerPublicKeys,
      List<String> holderPublicKeys,
      String state,
      String error,
      String errorDescription,
      String httpMethod) {

    log.info("Received {} request to /response endpoint", httpMethod);

    try {
      log.info("Received vpTokenMap: {}", vpTokenMap != null ? "present" : "null");
      log.info("Received response: vp_token={}, state={}, error={}",
          vpTokenMap != null ? "present" : "null", state, error);

      if (error != null) {
        log.error("Authorization error received: {} - {}", error, errorDescription);
        String escapedDescription = errorDescription != null ?
            escapeHtml(errorDescription) : "No description provided";
        return ServiceResult.failure(400, error, escapedDescription, state);
      }

      if (vpTokenMap != null && !vpTokenMap.isEmpty()) {
        return oid4VPHelperService.handleVPToken(vpTokenMap, issuerPublicKeys, holderPublicKeys, state);
      }

      return ServiceResult.failure(400, ERROR_INVALID_REQUEST, "vp_token is required", state);

    } catch (RuntimeException e) {
      log.error("Unexpected error processing response", e);
      String escapedMessage = escapeHtml(e.getMessage());
      return ServiceResult.failure(500, ERROR_PROCESSING_ERROR,
          "Failed to process request: " + escapedMessage, state);
    }
  }

  /**
   * Escapes HTML special characters to prevent XSS.
   */
  private String escapeHtml(String input) {
    if (input == null) {
      return null;
    }
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;");
  }

  /**
   * Builds authorization request map from session
   */
  public Map<String, Object> buildAuthorizationRequest(VerificationSession session)
      throws JsonProcessingException {
    OID4VPConfig config = configService.getOID4VPConfig();
    Map<String, Object> authRequest = new LinkedHashMap<>();

    authRequest.put("response_type", RESPONSE_TYPE_VP_TOKEN);
    authRequest.put("client_id", config.buildClientId());
    authRequest.put("response_mode", session.getResponseMode());
    authRequest.put("nonce", session.getNonce());
    authRequest.put(PARAM_STATE, session.getState());

    if (requiresResponseUri(session.getResponseMode())) {
      authRequest.put("response_uri", config.getResponseUrl());
    } else {
      authRequest.put("redirect_uri", config.getResponseUrl());
    }

    authRequest.put("dcql_query", new ObjectMapper().readValue(oid4VPHelperService.compactJsonString(session.getDcqlQuery()),
        Map.class));

    // Use session's merged client metadata if available, otherwise fallback to defaults
    if (session.getClientMetadata() != null && !session.getClientMetadata().isEmpty()) {
      Map<String, Object> clientMetadata = objectMapper.readValue(
          session.getClientMetadata(), Map.class);
      authRequest.put("client_metadata", clientMetadata);
      log.debug("Using session's merged client metadata");
    } else {
      addClientMetadata(authRequest);
      log.debug("Using default client metadata (session metadata not available)");
    }

    return authRequest;
  }

  /**
   * Creates a signed Authorization Request Object (JWS) as per RFC9101
   */
  public String createSignedAuthorizationRequest(Map<String, Object> authRequest, CompactSigner compactSigner, String verificationMethod, String publicKeyMultibase)
      throws OID4VPException {
    try {
      OID4VPConfig config = configService.getOID4VPConfig();

      if (compactSigner == null) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_AUTH_SIGNER_REQUIRED);
      }

      // Add iat claim
      Map<String, Object> payload = new LinkedHashMap<>(authRequest);
      payload.put("iat", System.currentTimeMillis() / 1000);

      String keyId = verificationMethod.split("#")[1];

      // Build JWK from multibase public key
      Map<String, Object> jwk = JwkUtils.buildJwkFromMultibase(publicKeyMultibase, verificationMethod);

      // Create JWS Header with typ parameter as required by RFC9101
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("alg", "ES256");
      header.put("typ", "oauth-authz-req+jwt");
      header.put("kid", verificationMethod);
      header.put("jwk", jwk);

      SignedJWT signedJWT = new SignedJWT(header, payload);
      signedJWT.sign(new ECDSASigner(compactSigner, keyId));

      log.debug("Created signed authorization request (JWS)");
      return signedJWT.serialize();

    } catch (OID4VPException e) {
      log.error("Failed to create signed authorization request", e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_AUTH_SIGN_FAILED, e.getErrorMsg(), e);
    } catch (Exception e) {
      log.error("Failed to create signed authorization request", e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_AUTH_SIGN_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Creates a signed Authorization Request Object (JWS) as per RFC9101 using PrivateKey
   */
  public String createSignedAuthorizationRequest(Map<String, Object> authRequest, PrivateKey privateKey, String verificationMethod, String publicKeyMultibase)
      throws OID4VPException {
    try {
      OID4VPConfig config = configService.getOID4VPConfig();

      if (privateKey == null) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_AUTH_SIGNER_REQUIRED, "PrivateKey cannot be null");
      }

      String keyId = verificationMethod.split("#")[1];

      // Add iat claim
      Map<String, Object> payload = new LinkedHashMap<>(authRequest);
      payload.put("iat", System.currentTimeMillis() / 1000);

      // Build JWK from multibase public key
      Map<String, Object> jwk = JwkUtils.buildJwkFromMultibase(publicKeyMultibase, keyId);

      // Create JWS Header with typ parameter as required by RFC9101
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("alg", "ES256");
      header.put("typ", "oauth-authz-req+jwt");
      header.put("kid", verificationMethod);
      header.put("jwk", jwk);

      SignedJWT signedJWT = new SignedJWT(header, payload);
      signedJWT.sign(new ECDSASigner(privateKey));

      log.debug("Created signed authorization request (JWS) using PrivateKey");
      return signedJWT.serialize();

    } catch (OID4VPException e) {
      log.error("Failed to create signed authorization request", e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_AUTH_SIGN_FAILED, e.getErrorMsg(), e);
    } catch (Exception e) {
      log.error("Failed to create signed authorization request", e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_AUTH_SIGN_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Adds client metadata to authorization request
   */
  private void addClientMetadata(Map<String, Object> authRequest) {
    try {
      Map<String, Object> clientMetadata = objectMapper.convertValue(
          clientMetadataService.createClientMetadata(),
          Map.class
      );
      authRequest.put("client_metadata", clientMetadata);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to create client metadata: {}", e.getMessage());
      authRequest.put("client_metadata", new LinkedHashMap<>());
    }
  }

  /**
   * Creates error response map
   */
  private Map<String, Object> createErrorResponse(String errorCode, String errorDescription) {
    return Map.of(
        "error", errorCode,
        "error_description", errorDescription
    );
  }

  /**
   * Checks if response_mode requires response_uri
   */
  private boolean requiresResponseUri(String responseMode) {
    return List.of("direct_post", "query").contains(responseMode);
  }

  /**
   * Checks if session has expired and updates status if expired
   */
  public boolean isSessionExpired(VerificationSession session) {
    OID4VPConfig config = configService.getOID4VPConfig();
    boolean expired = System.currentTimeMillis() - session.getCreatedAt() >
        config.getSession().getSessionTtl();

    if (expired && !OID4VPHelperService.SESSION_STATUS_EXPIRED.equals(session.getStatus())) {
      session.setStatus(OID4VPHelperService.SESSION_STATUS_EXPIRED);
      session.setExpiresAt(System.currentTimeMillis());
      sessionRepository.saveByState(session.getState(), session);
      log.info("Session expired. Status and expires_at updated for state: {}", session.getState());
    }

    return expired;
  }
}