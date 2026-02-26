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

package org.omnione.did.oid4vc.oid4vci.service;

import org.omnione.did.oid4vc.oid4vci.api.dto.PreAuthorizeResponse;
import org.omnione.did.oid4vc.oid4vci.config.IssuerSdkProperties;
import org.omnione.did.oid4vc.oid4vci.dto.credentialoffer.CredentialOfferRequest;
import org.omnione.did.oid4vc.oid4vci.dto.credentialoffer.CredentialOfferResponse;
import org.omnione.did.oid4vc.oid4vci.dto.qr.QrImageData;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIErrorCode;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIException;
import org.omnione.did.oid4vc.oid4vci.property.IssuerProperties;
import org.omnione.did.oid4vc.oid4vci.service.store.CredentialOfferStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.omnione.did.oid4vc.oid4vci.util.QrMaker.makeQrImage;

@Slf4j
@Service
public class IssuanceGatewayService {

    private final CredentialIssuerFeignService credentialIssuerFeignService;
    private final CredentialOfferStore credentialOfferStore;
    private final IssuerProperties issuerProperties;
    private final IssuerSdkProperties issuerSdkProperties;
    private final String issuerBaseUrl;
    private final CredentialService credentialService;
    private final ObjectMapper objectMapper;

    public IssuanceGatewayService(CredentialIssuerFeignService credentialIssuerFeignService,
                                  CredentialOfferStore credentialOfferStore,
                                  IssuerProperties issuerProperties,
                                  IssuerSdkProperties issuerSdkProperties,
                                  @Value("${issuer.base-url}") String issuerBaseUrl,
                                  CredentialService credentialService,
                                  ObjectMapper objectMapper) {
        this.credentialIssuerFeignService = credentialIssuerFeignService;
        this.credentialOfferStore = credentialOfferStore;
        this.issuerProperties = issuerProperties;
        this.issuerSdkProperties = issuerSdkProperties;
        this.issuerBaseUrl = issuerBaseUrl;
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> generateCredentialOfferUri(String userId, String grantType, String offerType, String scheme)
            throws IOException, WriterException, NoSuchAlgorithmException, OID4VCIException {

        String requestId = generateOfferId(userId, grantType);
        Map<String, Object> response = new HashMap<>();
        PreAuthorizeResponse preAuthorizeResponse = null;

        // Fetch pre-authorized code if requested
        if ("pre-authorized_code".equals(grantType)) {
            // --- Pre-Authorized Code Flow ---
            try {
                preAuthorizeResponse = credentialIssuerFeignService.getPreAuthorizedCode(userId);
                if (preAuthorizeResponse == null) {
                    log.error(
                            "Failed to get pre-authorized code. The response from the Authorization Server was null.");
                    throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_OFFER_GENERATE_FAILED,
                            "Failed to get pre-authorized code. The response from the Authorization Server was null.");
                }
                credentialOfferStore.save(requestId, preAuthorizeResponse);
                response.put("txCode", preAuthorizeResponse.getUserPin());
            } catch (FeignException e) {
                log.error("Failed to get pre-authorized code from Authorization Server", e);
                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_OFFER_GENERATE_FAILED, "Failed to get pre-authorized code from Authorization Server", e);
            }
        }

        if (scheme == null || scheme.isBlank()) {
            scheme = "openid-credential-offer://";
        } else if (!scheme.endsWith("//")) {
            if (scheme.endsWith(":")) {
                scheme += "//";
            } else {
                scheme += "://";
            }
        }

        String authUrl;
        if ("value".equals(offerType)) {
            CredentialOfferRequest offerRequest = new CredentialOfferRequest();
            offerRequest.setUserId(userId);
            // Strictly follow the selected grantType
            CredentialOfferResponse offerResponse = credentialService.createCredentialOffer(offerRequest, preAuthorizeResponse, grantType);
            String offerJson = objectMapper.writeValueAsString(offerResponse);
            authUrl = scheme + "credential_offer?credential_offer=" + URLEncoder.encode(offerJson, StandardCharsets.UTF_8);
        } else {
            String requestUri = issuerBaseUrl + "/credential-offer/" + requestId;
            authUrl = scheme + "?credential_offer_uri=" + requestUri;
        }

        QrImageData qrImageData = makeQrImage(authUrl);

        response.put("qrImage", qrImageData.getQrImage());
        response.put("qrData", authUrl);

        return response;
    }

    private String generateOfferId(String userId, String grantType) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // Add randomness using UUID to ensure unique offerId per request
        String salt = UUID.randomUUID().toString();
        md.update(salt.getBytes());
        md.update(userId.getBytes());
        
        byte[] offerIdBytes = md.digest();
        String offerId = "";
        if ("pre-authorized_code".equals(grantType)) {
            offerId = "p" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(offerIdBytes);
        } else if ("authorization_code".equals(grantType)) {
            offerId = "a" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(offerIdBytes);
        }
        return offerId;
    }

    public List<String> getCredentialIdentifier(String credentialConfigurationId) {
        log.info("getCredentialIdentifier request for: {}", credentialConfigurationId);
        
        Set<String> credentialIdentifiers = issuerSdkProperties.getIdentifiersByConfigId(credentialConfigurationId);
        if (credentialIdentifiers.isEmpty()) {
            log.warn("No identifiers found for credential configuration ID: {}", credentialConfigurationId);
            return Collections.emptyList();
        }
        
        return new ArrayList<>(credentialIdentifiers);
    }
}
