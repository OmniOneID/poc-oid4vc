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

package com.example.did.oid4vc.verifier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.service.SPHelperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sp")
@RequiredArgsConstructor
public class SPController {

    private final SPHelperService spHelperService;

    /**
     * Get decrypted VP Token by transaction ID.
     * Only returns VP Token for sessions with COMPLETED status.
     *
     * @param transactionId the transaction ID
     * @return decrypted VP Token with metadata or error response
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/vp-info/{transactionId}")
    public ResponseEntity<Map<String, Object>> getVPToken(@PathVariable String transactionId) {
        try {
            Map<String, Object> result = spHelperService.getDecryptedVPToken(transactionId);
            
            // 1. Extract vpTokenMap
            Map<String, Object> vpTokenMap = (Map<String, Object>) result.get("vpToken");
            Object firstEntry = vpTokenMap.values().iterator().next();
            
            // 2. Get the first VP (handle both List and direct Map cases)
            Map<String, Object> vp;
            if (firstEntry instanceof List) {
                List<?> vpList = (List<?>) firstEntry;
                Object firstVp = vpList.get(0);
                if (firstVp instanceof String) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    vp = objectMapper.readValue((String) firstVp, Map.class);
                } else {
                    vp = (Map<String, Object>) firstVp;
                }
            } else if (firstEntry instanceof Map) {
                vp = (Map<String, Object>) firstEntry;
            } else if (firstEntry instanceof String) {
                ObjectMapper objectMapper = new ObjectMapper();
                vp = objectMapper.readValue((String) firstEntry, Map.class);
            } else {
                throw new IllegalStateException("Unexpected vpToken structure: " + firstEntry.getClass().getName());
            }
            
            // 3. Extract verifiableCredential[0].credentialSubject
            List<Map<String, Object>> vcList = (List<Map<String, Object>>) vp.get("verifiableCredential");
            Map<String, Object> credentialSubject = (Map<String, Object>) vcList.get(0).get("credentialSubject");
            
            // 4. Build response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("result", true);
            response.put("credentialSubject", credentialSubject);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for VP Token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to get VP Token for transactionId: {}", transactionId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "result", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get parsed VP Token with claims for Multiple Credentials by transaction ID.
     * Returns parsed claims from each credential (mdoc, SD-JWT, etc.) in a unified format.
     *
     * @param transactionId the transaction ID
     * @return parsed credentials with claims or error response
     */
    @GetMapping("/dc/vp-info/{transactionId}")
    public ResponseEntity<Map<String, Object>> getVPTokenInfo(@PathVariable String transactionId) {
        try {
            Map<String, Object> result = spHelperService.getDecryptedVPTokenWithParsedClaims(transactionId);
            result.put("result", true);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for DC VP Token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to get DC VP Token for transactionId: {}", transactionId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "result", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get session status by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return session status with metadata or error response
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable String transactionId) {
        try {
            Map<String, Object> result = spHelperService.getSessionStatus(transactionId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for session status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to get session status for transactionId: {}", transactionId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "server_error",
                    "error_description", e.getMessage()
            ));
        }
    }
}