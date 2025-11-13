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

package com.example.oid4vc.verifier.controller;

import com.example.oid4vc.verifier.dto.VerificationSession;
import com.example.oid4vc.verifier.util.OID4VPHelper;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import com.example.oid4vc.verifier.configuration.OID4VPProperties;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import com.example.oid4vc.verifier.dto.DCQLResult;
import com.example.oid4vc.verifier.service.ScopeToDCQLMapper;
import com.example.oid4vc.verifier.service.ClientMetadataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

import static com.example.oid4vc.verifier.util.OID4VPHelper.*;

@Slf4j
@Controller
@RequestMapping("/oid4vp")
@RequiredArgsConstructor
@Tag(name = "OID4VP Verifier", description = "OpenID for Verifiable Presentations 1.0 Verifier API")
public class OID4VPController {

  private static final String RESPONSE_TYPE_VP_TOKEN = "vp_token";

  private static final String QUERY_SOURCE_DIRECT = "direct";
  private static final String QUERY_SOURCE_SCOPE = "scope";

  private static final String METHOD_BY_REFERENCE = "by_reference";
  private static final String METHOD_BY_VALUE = "by_value";

  private static final String ERROR_INVALID_REQUEST = "invalid_request";
  private static final String ERROR_INVALID_REQUEST_URI = "invalid_request_uri";
  private static final String ERROR_INVALID_SCOPE = "invalid_scope";
  private static final String ERROR_SERVER_ERROR = "server_error";
  private static final String ERROR_PROCESSING_ERROR = "processing_error";

  private static final String PARAM_STATE = "state";

  private final ScopeToDCQLMapper scopeToDCQLMapper;
  private final OID4VPProperties oid4vpProperties;
  private final ObjectMapper objectMapper;
  private final ClientMetadataService clientMetadataService;
  private final OID4VPHelper oid4VPHelper;

  private final Map<String, VerificationSession> sessions = new ConcurrentHashMap<>();

  @PostMapping("/initiate")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> initiateVerification(
      @RequestParam(required = false) String dcql_query,
      @RequestParam(required = false) String scope,
      @RequestParam(defaultValue = "direct_post") String response_mode,
      @RequestParam(required = false) String client_metadata,
      @RequestParam(defaultValue = "false") boolean use_request_uri) {
    try {
      if (areBothParamsMissing(dcql_query, scope)) {
        return ResponseEntity.badRequest().body(
            createErrorResponse(ERROR_INVALID_REQUEST,
                "Either dcql_query or scope parameter is required")
        );
      }

      if (areBothParamsProvided(dcql_query, scope)) {
        return ResponseEntity.badRequest().body(
            createErrorResponse(ERROR_INVALID_REQUEST,
                "Cannot specify both dcql_query and scope parameters")
        );
      }

      String parsedDcqlQuery = dcql_query;
      String querySource = QUERY_SOURCE_DIRECT;

      if (!isEmptyParam(scope)) {
        DCQLQuery dcqlFromScope = scopeToDCQLMapper.scopeToDCQL(scope);
        if (dcqlFromScope == null) {
          return ResponseEntity.badRequest().body(
              createErrorResponse(ERROR_INVALID_SCOPE, "No DCQL mapping found for scope: " + scope)
          );
        }

        try {
          parsedDcqlQuery = objectMapper.writeValueAsString(dcqlFromScope);
          querySource = QUERY_SOURCE_SCOPE;
        } catch (JsonProcessingException e) {
          log.error("Failed to serialize DCQL from scope", e);
          return ResponseEntity.badRequest().body(
              createErrorResponse(ERROR_SERVER_ERROR, "Failed to process scope mapping")
          );
        }
      }

      try {
        DCQLQuery dcqlQueryObj;
        try {
          dcqlQueryObj = objectMapper.readValue(parsedDcqlQuery, DCQLQuery.class);
        } catch (JsonProcessingException e) {
          return ResponseEntity.badRequest().body(
              createErrorResponse(ERROR_INVALID_REQUEST,
                  "Invalid DCQL JSON format: " + e.getMessage())
          );
        }

        DCQLQueryValidator.ValidationResult sdkValidation = DCQLQueryValidator.validate(
            dcqlQueryObj);

        if (!sdkValidation.isValid()) {
          String errorMessage = "Invalid DCQL query";
          if (!sdkValidation.getErrors().isEmpty()) {
            errorMessage += ": " + String.join(", ", sdkValidation.getErrors());
          }

          return ResponseEntity.badRequest().body(
              createErrorResponse(ERROR_INVALID_REQUEST, errorMessage)
          );
        }

        DCQLResult validation = oid4VPHelper.convertValidationResult(sdkValidation, dcqlQueryObj);

        String state = oid4VPHelper.generateSecureState();
        String nonce = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();

        VerificationSession session = new VerificationSession();
        session.setState(state);
        session.setNonce(nonce);
        session.setDcqlQuery(parsedDcqlQuery);
        session.setResponseMode(response_mode);
        session.setRequestId(requestId);
        session.setCreatedAt(System.currentTimeMillis());
        sessions.put(state, session);

        Map<String, Object> response = new LinkedHashMap<>();

        StringBuilder authUrl = new StringBuilder();
        authUrl.append("openid4vp://");

        if (use_request_uri) {
          String requestUri = oid4vpProperties.getBaseUrl() + "/oid4vp/request/" + requestId;
          authUrl.append("?request_uri=").append(oid4VPHelper.encodeValue(requestUri));
          authUrl.append("&client_id=")
              .append(oid4VPHelper.encodeValue(oid4vpProperties.buildClientId()));

          response.put("authorization_request_uri", authUrl.toString());
          response.put("request_uri", requestUri);
          response.put("method", METHOD_BY_REFERENCE);

        } else {
          authUrl.append("?response_type=").append(RESPONSE_TYPE_VP_TOKEN)
              .append("&client_id=")
              .append(oid4VPHelper.encodeValue(oid4vpProperties.buildClientId()))
              .append("&response_mode=").append(oid4VPHelper.encodeValue(response_mode))
              .append("&nonce=").append(oid4VPHelper.encodeValue(nonce))
              .append("&state=").append(oid4VPHelper.encodeValue(state));

          if (requiresResponseUri(response_mode)) {
            authUrl.append("&response_uri=")
                .append(oid4VPHelper.encodeValue(oid4vpProperties.getResponseUrl()));
          } else {
            authUrl.append("&redirect_uri=")
                .append(oid4VPHelper.encodeValue(oid4vpProperties.getResponseUrl()));
          }

          if (QUERY_SOURCE_SCOPE.equals(querySource)) {
            authUrl.append("&scope=").append(oid4VPHelper.encodeValue(scope));
          } else {
            authUrl.append("&dcql_query=")
                .append(oid4VPHelper.encodeValue(oid4VPHelper.compactJsonString(parsedDcqlQuery)));
          }

          String clientMetadataToAdd;
          if (client_metadata != null && !client_metadata.trim().isEmpty()) {
            clientMetadataToAdd = client_metadata;
          } else {
            try {
              clientMetadataToAdd = objectMapper.writeValueAsString(
                  clientMetadataService.createClientMetadata()
              );
              log.debug("Created default client metadata: {}", clientMetadataToAdd);
            } catch (JsonProcessingException e) {
              log.warn("Failed to create default client metadata: {}", e.getMessage());
              clientMetadataToAdd = "";
            }
          }
          authUrl.append("&client_metadata=").append(oid4VPHelper.encodeValue(clientMetadataToAdd));

          response.put("authorization_request_uri", authUrl.toString());
          response.put("method", METHOD_BY_VALUE);
          response.put("request_uri",
              oid4vpProperties.getBaseUrl() + "/oid4vp/request/" + requestId);
        }

        response.put("request_id", requestId);
        response.put(PARAM_STATE, state);
        response.put("nonce", nonce);
        response.put("response_mode", response_mode);
        response.put("dcql_validated", true);
        response.put("credential_count", validation.getCredentialCount());
        response.put("query_source", querySource);
        response.put("use_request_uri", use_request_uri);
        response.put("client_id", oid4vpProperties.buildClientId(response_mode));

        try {
          Map<String, Object> clientMetadataMap = objectMapper.convertValue(
              clientMetadataService.createClientMetadata(),
              Map.class
          );
          response.put("client_metadata", clientMetadataMap);
          log.debug("Added client metadata to response: {}", clientMetadataMap);
        } catch (IllegalArgumentException e) {
          log.warn("Failed to add client metadata to response: {}", e.getMessage());
          Map<String, Object> basicClientMetadata = new LinkedHashMap<>();
          response.put("client_metadata", basicClientMetadata);
        }

        if (QUERY_SOURCE_SCOPE.equals(querySource)) {
          response.put("scope", scope);
          response.put("mapped_dcql", oid4VPHelper.compactJsonString(parsedDcqlQuery));
        }

        return ResponseEntity.ok(response);

      } catch (RuntimeException e) {
        log.error("DCQL validation failed using sdjwt SDK", e);
        return ResponseEntity.badRequest().body(
            createErrorResponse(ERROR_INVALID_REQUEST, "DCQL validation error: " + e.getMessage())
        );
      }

    } catch (RuntimeException e) {
      log.error("Failed to initiate verification", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          createErrorResponse(ERROR_SERVER_ERROR,
              "Failed to initiate verification: " + e.getMessage())
      );
    }
  }

  // TODO: This must be returned as a JWS.
  @GetMapping("/request/{request_id}")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> getAuthorizationRequest(
      @PathVariable String request_id) {

    log.info("Retrieving authorization request for request_id: {}", request_id);

    VerificationSession session = sessions.values().stream()
        .filter(s -> request_id.equals(s.getRequestId()))
        .findFirst()
        .orElse(null);

    if (session == null) {
      log.warn("Authorization request not found for request_id: {}", request_id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          createErrorResponse(ERROR_INVALID_REQUEST_URI,
              "The request_uri is invalid or has expired")
      );
    }

    if (System.currentTimeMillis() - session.getCreatedAt() > oid4vpProperties.getSession()
        .getRequestUriTtl()) {
      log.warn("Authorization request expired for request_id: {}", request_id);
      sessions.entrySet().removeIf(entry -> request_id.equals(entry.getValue().getRequestId()));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          createErrorResponse(ERROR_INVALID_REQUEST_URI, "The request_uri has expired")
      );
    }

    Map<String, Object> authRequest = new LinkedHashMap<>();
    authRequest.put("response_type", RESPONSE_TYPE_VP_TOKEN);
    authRequest.put("client_id", oid4vpProperties.buildClientId(session.getResponseMode()));
    authRequest.put("response_mode", session.getResponseMode());
    authRequest.put("nonce", session.getNonce());
    authRequest.put(PARAM_STATE, session.getState());

    if (requiresResponseUri(session.getResponseMode())) {
      authRequest.put("response_uri", oid4vpProperties.getResponseUrl());
    } else {
      authRequest.put("redirect_uri", oid4vpProperties.getResponseUrl());
    }

    authRequest.put("dcql_query", oid4VPHelper.compactJsonString(session.getDcqlQuery()));

    try {
      Map<String, Object> clientMetadata = objectMapper.convertValue(
          clientMetadataService.createClientMetadata(),
          Map.class
      );
      authRequest.put("client_metadata", clientMetadata);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to create client metadata: {}", e.getMessage());

      Map<String, Object> clientMetadata = new LinkedHashMap<>(); // Empty
      authRequest.put("client_metadata", clientMetadata);
    }

    log.info("Successfully retrieved authorization request for request_id: {}", request_id);
    return ResponseEntity.ok(authRequest);
  }


  @RequestMapping(value = "/response", method = {RequestMethod.POST, RequestMethod.GET})
  @ResponseBody
  public ResponseEntity<Map<String, Object>> receiveResponse(
      @RequestParam(required = false) String vp_token,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String error_description,
      HttpServletRequest request) {
    log.info("Received {} request to /response endpoint", request.getMethod());

    try {
      log.info("Received vpToken : {}", vp_token);

      log.info("Received response: vp_token={}, state={}, error={}",
          vp_token != null ? "present" : "null",
          state, error);

      if (error != null) {
        log.error("Authorization error received: {} - {}", error, error_description);
        String escapedDescription =
            error_description != null ? HtmlUtils.htmlEscape(error_description)
                : "No description provided";
        return ResponseEntity.ok(
            oid4VPHelper.createErrorResponseWithState(error, escapedDescription, state)
        );
      }

      if (!isEmptyParam(vp_token)) {
        return oid4VPHelper.handleVPToken(vp_token, state, sessions);
      }

      return ResponseEntity.ok(
          oid4VPHelper.createErrorResponseWithState(ERROR_INVALID_REQUEST, "vp_token is required",
              state)
      );
    } catch (RuntimeException e) {
      log.error("Unexpected error processing response", e);
      String escapedMessage = HtmlUtils.htmlEscape(e.getMessage());
      return ResponseEntity.ok(
          oid4VPHelper.createErrorResponseWithState(ERROR_PROCESSING_ERROR,
              "Failed to process request: " + escapedMessage, state)
      );
    }
  }

  @PostMapping("/validate-dcql")
  @ResponseBody
  public ResponseEntity<DCQLResult> validateDcql(@RequestBody String dcqlJson) {
    DCQLQuery dcqlQuery;
    try {
      dcqlQuery = objectMapper.readValue(dcqlJson, DCQLQuery.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse DCQL JSON", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
          DCQLResult.builder()
              .valid(false)
              .success(false)
              .message("Invalid DCQL JSON format: " + e.getMessage())
              .credentialCount(0)
              .build());
    }

    DCQLQueryValidator.ValidationResult validationResult = DCQLQueryValidator.validate(dcqlQuery);

    DCQLResult result = oid4VPHelper.convertValidationResult(validationResult, dcqlQuery);

    return result.isValid() ? ResponseEntity.ok(result)
        : ResponseEntity.badRequest().body(result);
  }

  @GetMapping("/test")
  @Hidden
  public String test() {
    return "verifier/test";
  }
}