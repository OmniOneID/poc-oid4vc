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
import org.omnione.did.oid4vc.oid4vp.dto.DCQLResult;
import org.omnione.did.oid4vc.oid4vp.dto.ServiceResult;
import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitiationService {

  private static final String RESPONSE_TYPE_VP_TOKEN = "vp_token";
  private static final String QUERY_SOURCE_DIRECT = "direct";
  private static final String QUERY_SOURCE_SCOPE = "scope";
  private static final String METHOD_BY_REFERENCE = "by_reference";
  private static final String METHOD_BY_VALUE = "by_value";
  private static final String ERROR_INVALID_REQUEST = "invalid_request";
  private static final String ERROR_INVALID_SCOPE = "invalid_scope";
  private static final String ERROR_SERVER_ERROR = "server_error";

  private final ScopeToDCQLMapperService scopeToDCQLMapperService;
  private final VerifierConfigService configService;
  private final ObjectMapper objectMapper;
  private final ClientMetadataService clientMetadataService;
  private final OID4VPHelperService oid4VPHelperService;
  private final SessionRepository sessionRepository;

  /**
   * Initiates verification by processing DCQL query or scope parameter
   *
   * @param dcqlQuery      the DCQL query JSON string
   * @param scope          the scope parameter
   * @param responseMode   the response mode
   * @param clientMetadata the client metadata
   * @param useRequestUri  whether to use request URI
   * @return ServiceResult containing initiation response data or error information
   */
  public ServiceResult<Map<String, Object>> initiateVerification(
      String dcqlQuery,
      String scope,
      String responseMode,
      String clientMetadata,
      boolean useRequestUri) throws OID4VPException {

    OID4VPConfig config = configService.getOID4VPConfig();

    // Validate that either dcql_query or scope is provided, but not both
    if (areBothParamsMissing(dcqlQuery, scope)) {
      return ServiceResult.badRequest(ERROR_INVALID_REQUEST,
          "Either dcql_query or scope parameter is required");
    }

    // Process DCQL query
    String parsedDcqlQuery = dcqlQuery;
    String querySource = QUERY_SOURCE_DIRECT;

    if (!isEmptyParam(scope)) {
      DCQLQuery dcqlFromScope = scopeToDCQLMapperService.scopeToDCQL(scope);
      if (dcqlFromScope == null) {
        return ServiceResult.badRequest(ERROR_INVALID_SCOPE,
            "No DCQL mapping found for scope: " + scope);
      }

      try {
        parsedDcqlQuery = objectMapper.writeValueAsString(dcqlFromScope);
        querySource = QUERY_SOURCE_SCOPE;
      } catch (JsonProcessingException e) {
        log.error("Failed to serialize DCQL from scope", e);
        return ServiceResult.serverError(ERROR_SERVER_ERROR, "Failed to process scope mapping");
      }
    }

    // Validate DCQL query
    DCQLQuery dcqlQueryObj;
    try {
      dcqlQueryObj = objectMapper.readValue(parsedDcqlQuery, DCQLQuery.class);
    } catch (JsonProcessingException e) {
      return ServiceResult.badRequest(ERROR_INVALID_REQUEST,
          "Invalid DCQL JSON format: " + e.getMessage());
    }

    DCQLQueryValidator.ValidationResult sdkValidation = DCQLQueryValidator.validate(dcqlQueryObj);

    if (!sdkValidation.isValid()) {
      String errorMessage = "Invalid DCQL query";
      if (!sdkValidation.getErrors().isEmpty()) {
        errorMessage += ": " + String.join(", ", sdkValidation.getErrors());
      }
      return ServiceResult.badRequest(ERROR_INVALID_REQUEST, errorMessage);
    }

    // Convert validation result
    DCQLResult validation = oid4VPHelperService.convertValidationResult(sdkValidation,
        dcqlQueryObj);

    // Generate session identifiers
    String transactionId = UUID.randomUUID().toString();
    String state = oid4VPHelperService.generateSecureState();
    String nonce = UUID.randomUUID().toString();
    String requestId = UUID.randomUUID().toString();

    // Merge client metadata before session creation
    Map<String, Object> mergedMetadata = null;
    String mergedClientMetadataJson = null;
    try {
      mergedMetadata = objectMapper.convertValue(
          clientMetadataService.createClientMetadata(),
          Map.class
      );
      if (clientMetadata != null && !clientMetadata.trim().isEmpty()) {
        Map<String, Object> injectedMetadata = objectMapper.readValue(clientMetadata, Map.class);
        deepMerge(mergedMetadata, injectedMetadata);
        log.debug("Merged injected client metadata fields: {}", injectedMetadata.keySet());
      }
      mergedClientMetadataJson = objectMapper.writeValueAsString(mergedMetadata);
    } catch (IllegalArgumentException | JsonProcessingException e) {
      log.warn("Failed to merge client metadata: {}", e.getMessage());
      mergedMetadata = new LinkedHashMap<>();
    }

    // Create and store session using repository
    VerificationSession session = new VerificationSession();
    session.setTransactionId(transactionId);
    session.setState(state);
    session.setNonce(nonce);
    session.setDcqlQuery(parsedDcqlQuery);
    session.setResponseMode(responseMode);
    session.setRequestId(requestId);
    session.setClientMetadata(mergedClientMetadataJson);
    session.setCreatedAt(System.currentTimeMillis());
    sessionRepository.saveByState(state, session);

    // Build response
    Map<String, Object> response = new LinkedHashMap<>();

    if (useRequestUri) {
      buildRequestUriResponse(response, requestId, responseMode, config);
    } else {
      buildDirectResponse(response, requestId, responseMode, nonce, state,
          parsedDcqlQuery, mergedClientMetadataJson, config);
    }

    // Add common response fields
    response.put("transaction_id", transactionId);
    response.put("request_id", requestId);
    response.put("state", state);
    response.put("nonce", nonce);
    response.put("response_mode", responseMode);
    response.put("dcql_validated", true);
    response.put("credential_count", validation.getCredentialCount());
    response.put("query_source", querySource);
    response.put("use_request_uri", useRequestUri);
    response.put("client_id", config.buildClientId());

    // Add pre-merged client metadata to response
    response.put("client_metadata", mergedMetadata);
    log.debug("Added client metadata to response: {}", mergedMetadata);

    // Add scope-specific fields
    if (QUERY_SOURCE_SCOPE.equals(querySource)) {
      response.put("scope", scope);
      response.put("mapped_dcql", oid4VPHelperService.compactJsonString(parsedDcqlQuery));
    }

    return ServiceResult.success(response);
  }

  /**
   * Builds response for by_reference method (using request_uri)
   */
  public void buildRequestUriResponse(Map<String, Object> response, String requestId,
      String responseMode, OID4VPConfig config) {
    StringBuilder authUrl = new StringBuilder();
    authUrl.append(config.getInvocationScheme());

    String requestUri = config.getRequestUrl() + "/" + requestId;
    authUrl.append("?request_uri=").append(oid4VPHelperService.encodeValue(requestUri));
    authUrl.append("&client_id=")
        .append(oid4VPHelperService.encodeValue(config.buildClientId()));

    response.put("authorization_request_uri", authUrl.toString());
    response.put("request_uri", requestUri);
    response.put("method", METHOD_BY_REFERENCE);
  }

  /**
   * Builds response for by_value method (direct parameters)
   */
  public void buildDirectResponse(Map<String, Object> response, String requestId,
      String responseMode, String nonce, String state, String parsedDcqlQuery,
      String mergedClientMetadataJson, OID4VPConfig config) {

    StringBuilder authUrl = new StringBuilder();
    authUrl.append(config.getInvocationScheme());
    authUrl.append("?response_type=").append(RESPONSE_TYPE_VP_TOKEN)
        .append("&client_id=")
        .append(oid4VPHelperService.encodeValue(config.buildClientId()))
        .append("&response_mode=").append(oid4VPHelperService.encodeValue(responseMode))
        .append("&nonce=").append(oid4VPHelperService.encodeValue(nonce))
        .append("&state=").append(oid4VPHelperService.encodeValue(state));

    // Add response_uri or redirect_uri based on response_mode
    if (requiresResponseUri(responseMode)) {
      authUrl.append("&response_uri=")
          .append(oid4VPHelperService.encodeValue(config.getResponseUrl()));
    } else {
      authUrl.append("&redirect_uri=")
          .append(oid4VPHelperService.encodeValue(config.getResponseUrl()));
    }

    // Always use dcql_query (scope is converted to dcql_query before this point)
    authUrl.append("&dcql_query=")
        .append(oid4VPHelperService.encodeValue(
            oid4VPHelperService.compactJsonString(parsedDcqlQuery)));

    // Add pre-merged client metadata
    authUrl.append("&client_metadata=")
        .append(oid4VPHelperService.encodeValue(
            mergedClientMetadataJson != null ? mergedClientMetadataJson : "{}"));

    response.put("authorization_request_uri", authUrl.toString());
    response.put("method", METHOD_BY_VALUE);
    response.put("request_uri", config.getBaseUrl() + config.getRequestUrl() + "/" + requestId);
  }

  /**
   * Helper method to check if both parameters are missing
   */
  private boolean areBothParamsMissing(String dcqlQuery, String scope) {
    return isEmptyParam(dcqlQuery) && isEmptyParam(scope);
  }

  /**
   * Helper method to check if parameter is empty
   */
  private boolean isEmptyParam(String param) {
    return param == null || param.trim().isEmpty();
  }

  /**
   * Helper method to check if response_mode requires response_uri
   */
  private boolean requiresResponseUri(String responseMode) {
    return "direct_post".equals(responseMode);
  }

  /**
   * Deep merges source map into target map. For nested maps, recursively merges instead of
   * replacing. For other values, source overwrites target.
   *
   * @param target the target map to merge into
   * @param source the source map to merge from
   */
  @SuppressWarnings("unchecked")
  public void deepMerge(Map<String, Object> target, Map<String, Object> source) {
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      Object sourceValue = entry.getValue();
      Object targetValue = target.get(key);

      if (sourceValue instanceof Map && targetValue instanceof Map) {
        deepMerge((Map<String, Object>) targetValue, (Map<String, Object>) sourceValue);
      } else {
        target.put(key, sourceValue);
      }
    }
  }

}