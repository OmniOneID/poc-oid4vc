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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;
import org.omnione.did.oid4vc.oid4vp.util.crypto.VPTokenEncryptor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SPHelperService {

    private static final String SESSION_STATUS_COMPLETED = "COMPLETED";

    private final SessionRepository sessionRepository;
    private final VPTokenEncryptor vpTokenEncryptor;
    private final ObjectMapper objectMapper;

    /**
     * Get decrypted VP Token by transaction ID.
     * Only returns VP Token for sessions with COMPLETED status.
     *
     * @param transactionId the transaction ID
     * @return Map containing decrypted VP Token and metadata
     * @throws OID4VPException if session not found, not completed, VP Token not found, or decryption/parsing fails
     */
    public Map<String, Object> getDecryptedVPToken(String transactionId) throws OID4VPException {
        // 1. Validate input
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER, "transactionId");
        }

        // 2. Find session by transactionId
        VerificationSession session = sessionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new OID4VPException(
                        OID4VPErrorCode.ERR_CODE_AUTH_INVALID_SESSION,
                        "Session not found for transactionId: " + transactionId));

        // 3. Check status is COMPLETED
        if (!SESSION_STATUS_COMPLETED.equals(session.getStatus())) {
            throw new OID4VPException(
                    OID4VPErrorCode.ERR_CODE_AUTH_SESSION_NOT_COMPLETED,
                    "Current status: " + session.getStatus());
        }

        // 4. Check vp_token exists
        String encryptedVpToken = session.getVpToken();
        if (encryptedVpToken == null || encryptedVpToken.isEmpty()) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_NULL, "VP Token not found for this session");
        }

        // 5. Decrypt VP Token
        String decryptedVpTokenJson;
        try {
            decryptedVpTokenJson = vpTokenEncryptor.decrypt(encryptedVpToken);
        } catch (RuntimeException e) {
            log.error("Failed to decrypt VP Token for transactionId: {}", transactionId, e);
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_DECRYPT_FAILED, e.getMessage(), e);
        }

        // 6. Parse to Map
        Map<String, List<String>> vpTokenMap;
        try {
            vpTokenMap = objectMapper.readValue(decryptedVpTokenJson,
                    objectMapper.getTypeFactory().constructMapType(
                            LinkedHashMap.class, String.class, List.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse decrypted VP Token JSON for transactionId: {}", transactionId, e);
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, e.getMessage(), e);
        }

        // 7. Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionId", transactionId);
        result.put("status", session.getStatus());
        result.put("vpToken", vpTokenMap);
        result.put("nonce", session.getNonce());
        result.put("createdAt", session.getCreatedAt());
        result.put("updatedAt", session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null);

        log.info("VP Token retrieved successfully for transactionId: {}", transactionId);
        return result;
    }

    /**
     * Get session status by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return Map containing session status and metadata
     * @throws OID4VPException if session not found
     */
    public Map<String, Object> getSessionStatus(String transactionId) throws OID4VPException {
        // 1. Validate input
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER, "transactionId");
        }

        // 2. Find session by transactionId
        VerificationSession session = sessionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new OID4VPException(
                        OID4VPErrorCode.ERR_CODE_AUTH_INVALID_SESSION,
                        "Session not found for transactionId: " + transactionId));

        // 3. Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionId", transactionId);
        result.put("status", session.getStatus());
        result.put("hasVpToken", session.getVpToken() != null && !session.getVpToken().isEmpty());
        result.put("createdAt", session.getCreatedAt());
        result.put("expiresAt", session.getExpiresAt());
        result.put("updatedAt", session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null);

        log.debug("Session status retrieved for transactionId: {}, status: {}", transactionId, session.getStatus());
        return result;
    }
}