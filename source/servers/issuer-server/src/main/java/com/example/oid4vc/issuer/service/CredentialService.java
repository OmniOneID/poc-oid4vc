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

package com.example.oid4vc.issuer.service;

import com.example.oid4vc.issuer.property.IssuerProperties;
import com.example.oid4vc.issuer.dto.credential.Credential;
import com.example.oid4vc.issuer.dto.credential.CredentialRequest;
import com.example.oid4vc.issuer.dto.credential.CredentialResponse;
import com.example.oid4vc.issuer.dto.credential.DeferredIssuanceResponse;
import com.example.oid4vc.issuer.dto.credentialoffer.*;
import com.example.oid4vc.issuer.api.dto.PreAuthorizeResponse;
import com.example.oid4vc.issuer.dto.metadata.IssuerMetadataResponse;
import com.example.oid4vc.issuer.dto.nonce.NonceResponse;
import com.example.oid4vc.issuer.dto.notification.NotificationRequest;
import com.example.oid4vc.issuer.dto.token.TokenRequest;
import com.example.oid4vc.issuer.dto.token.TokenResponse;
import com.example.oid4vc.issuer.issuer.CredentialIssuer;
import com.example.oid4vc.issuer.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CredentialService {

    private static final Logger logger = LoggerFactory.getLogger(CredentialService.class);

    private final IssuerProperties issuerProperties;
    private final Map<String, String> issuerStates = new ConcurrentHashMap<>();
    private final Map<String, String> preAuthorizedCodes = new ConcurrentHashMap<>();
    private final Map<String, CredentialRequest> deferredCredentialStore = new ConcurrentHashMap<>();
    private final Map<String, CredentialIssuer> issuers;
    private final SharedStateService sharedStateService;
    private final ObjectMapper objectMapper;

    public CredentialService(IssuerProperties issuerProperties, Map<String, CredentialIssuer> issuers, SharedStateService sharedStateService, ObjectMapper objectMapper) {
        this.issuerProperties = issuerProperties;
        this.issuers = issuers;
        this.sharedStateService = sharedStateService;
        this.objectMapper = objectMapper;
    }

    public CredentialOfferResponse createCredentialOffer(CredentialOfferRequest request, PreAuthorizeResponse preAuthorizeResponse, String grantType) {
        GrantsDto grantsDto = new GrantsDto();
        if (grantType.equals(Constants.AUTHORIZATION_CODE_TYPE)) {
            String issuerState = generateIssuerState();
            issuerStates.put(issuerState, request.getUserId());

            AuthorizationCodeGrantDto authCodeGrant = new AuthorizationCodeGrantDto();
            authCodeGrant.setIssuerState(issuerState);

            grantsDto.setAuthorizationCode(authCodeGrant);
        }

        if (grantType.equals(Constants.PRE_AUTHORIZED_CODE_TYPE)) {
            String preAuthorizedCode = "";
            String userPin = "";
            if(preAuthorizeResponse != null) {
                preAuthorizedCode = preAuthorizeResponse.getPreAuthorizedCode();
                userPin = preAuthorizeResponse.getUserPin();
            }
            preAuthorizedCodes.put(preAuthorizedCode, request.getUserId());

            PreAuthorizedCodeGrantDto preAuthGrant = new PreAuthorizedCodeGrantDto();
            preAuthGrant.setPreAuthorizedCode(preAuthorizedCode);

            if (userPin != null && !userPin.isEmpty()) {
                TxCodeDto txCodeDto = new TxCodeDto();
                txCodeDto.setInputMode("direct");
                txCodeDto.setLength(userPin.length());
                txCodeDto.setDescription("Please enter the PIN.");
                preAuthGrant.setTxCode(txCodeDto);
            }

            grantsDto.setPreAuthorizedCode(preAuthGrant);
        }

        CredentialOfferResponse response = new CredentialOfferResponse();
        response.setCredentialIssuer(getIssuerMetadata().getCredentialIssuer());
        response.setCredentialConfigurationIds(getConfigurationIds());

        response.setGrants(grantsDto);
        return response;
    }

    public CredentialOfferResponse createCredentialOffer(CredentialOfferRequest request, String grantType) {
        if (!grantType.equals(Constants.AUTHORIZATION_CODE_TYPE)) {
            throw new IllegalArgumentException("This method is only for authorization_code grant type");
        }
        return createCredentialOffer(request, null, grantType);
    }

    //todo: admin?
    private Set<String> getConfigurationIds(){
        List<String> targetCredentialIds = List.of("VerifiableIdSD", "StudentID");
        Set<String> configurationIds = new HashSet<>();

        for (String id : targetCredentialIds) {
            if (getIssuerMetadata().getCredentialConfigurationsSupported().containsKey(id)) {
                configurationIds.add(id);
            } else {
                throw new IllegalStateException("Requested credential configuration ID not found: " + id);
            }
        }
        return configurationIds;

    }
    public IssuerMetadataResponse getIssuerMetadata() {
        String filePath = issuerProperties.getMetadataFilePath();
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("issuer.metadata-file-path is not configured in application.yml");
        }
        try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
            return objectMapper.readValue(is, IssuerMetadataResponse.class);
        } catch (IOException e) {
            logger.error("Failed to load issuer metadata from path: {}", filePath, e);
            throw new RuntimeException("Failed to load issuer metadata from path: " + filePath, e);
        }
    }

    public Object issueCredential(CredentialRequest request, Jwt accessToken) {
        boolean deferred = false; // todo: Figure out how to handle this

        if (deferred) {
            String transactionId = generateTransactionId();
            deferredCredentialStore.put(transactionId, request);
            DeferredIssuanceResponse response = new DeferredIssuanceResponse();
            response.setTransactionId(transactionId);
            return response;
        } else {
            return issueImmediateCredential(request, accessToken);
        }
    }

    public CredentialResponse getDeferredCredential(String transactionId, Jwt accessToken) {
        CredentialRequest request = deferredCredentialStore.remove(transactionId);
        if (request == null) {
            throw new IllegalArgumentException("Invalid transaction_id");
        }
        return issueImmediateCredential(request, accessToken);
    }

    public NonceResponse handleNonce() {
        NonceResponse response = new NonceResponse();
        response.setCNonce(generateCNonce());

        return response;
    }

    public void handleNotification(NotificationRequest request) {
        logger.info("Received notification: {}", request.getNotificationId());
        // todo: Figure out how to handle this
    }

    private CredentialResponse issueImmediateCredential(CredentialRequest request, Jwt accessToken) {
        // todo: Clarify if using credential_configuration_id or credentialIdentifier.
        try {
            final String credentialConfigurationId = request.getCredentialConfigurationId();
            final String credentialIdentifier = request.getCredentialIdentifier();

            if ((credentialConfigurationId == null || credentialConfigurationId.isBlank()) && (credentialIdentifier == null || credentialIdentifier.isBlank())) {
                throw new IllegalArgumentException("Either 'credential_configuration_id' or 'credential_identifier' MUST be present.");
            }
            if (credentialConfigurationId != null && credentialIdentifier != null) {
                throw new IllegalArgumentException("'credential_configuration_id' and 'credential_identifier' MUST NOT be used together.");
            }

            if (request.getProof() == null || !"jwt".equals(request.getProof().getProofType())) {
                throw new IllegalArgumentException("Unsupported or missing proof type. 'jwt' proof is required.");
            }

            // Conditional c_nonce verification
            String cNonceFromToken = accessToken.getClaimAsString("c_nonce");

            // Perform proof verification only when c_nonce is present in the Access Token.
            if (cNonceFromToken != null && !cNonceFromToken.isBlank()) {
                logger.info("c_nonce found in Access Token. Performing proof validation.");

                if (request.getProof() == null || !"jwt".equals(request.getProof().getProofType())) {
                    throw new IllegalArgumentException("Unsupported or missing proof type. 'jwt' proof is required when c_nonce is present.");
                }

                String proofJwtString = request.getProof().getJwt();
                SignedJWT proofJwt = SignedJWT.parse(proofJwtString);
                String nonceFromProof = proofJwt.getJWTClaimsSet().getStringClaim("nonce");

                logger.info("c_nonce from Access Token: [{}]", cNonceFromToken);
                logger.info("  nonce from Proof JWT   : [{}]", nonceFromProof);

                if (!cNonceFromToken.equals(nonceFromProof)) {
                    throw new IllegalArgumentException("Invalid c_nonce. Proof nonce does not match c_nonce from the access token.");
                }
                logger.info("c_nonce verification successful.");

            } else {
                // If c_nonce is absent, assume tx_code flow and skip proof validation.
                logger.info("No c_nonce in Access Token. Assuming tx_code flow, skipping proof validation.");
            }

            // Extract User ID from Access Token
            String userId = accessToken.getSubject();
            if (userId == null) {
                throw new IllegalArgumentException("User ID (sub) is missing from the access token.");
            }

            // Select appropriate Issuer and issue VC based on the request.
            // Claim information required for VC issuance is determined by the Access Token's scope or policies between AS and Issuer.
//            String credentialType = (format != null) ? format : credentialIdentifier;
//            logger.info("  credentialType   : [{}]", credentialType);
//            CredentialIssuer issuer = issuers.get(credentialType);
//            if (issuer == null) {
//                throw new IllegalArgumentException("No issuer found for: " + credentialType);
//            }

            String credentialType = null;

            if (credentialIdentifier != null && !credentialIdentifier.isBlank()) {
                credentialType = credentialIdentifier;

            } else if (credentialConfigurationId != null && !credentialConfigurationId.isBlank()) {
                final String requestedFormat = credentialIdentifier;

//                credentialType = issuerProperties.getCredentialConfigurations().entrySet().stream()
//                        .filter(entry -> requestedFormat.equals(entry.getValue().getFormat()))
//                        .map(Map.Entry::getKey)
//                        .findFirst()
//                        .orElse(null);
            }

            logger.info("  Determined credentialType for issuer lookup: [{}]", credentialType);
            CredentialIssuer issuer = issuers.get(credentialType);
            if (issuer == null) {
                throw new IllegalArgumentException("No issuer found for: " + credentialType);
            }
            Object rawCredential = issuer.issue(userId, credentialType, cNonceFromToken);

            //todo : draft
//            String credentialFormat = getCredentialFormat(credentialIdentifier);

            CredentialResponse response = new CredentialResponse();

            Credential credential = new Credential();
            credential.setCredential(rawCredential);
            List<Credential> credentialList = new ArrayList<>();
            credentialList.add(credential);
            response.setCredentials(credentialList);

            return response;

        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse proof JWT", e);
        }
    }

    private String generateIssuerState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCNonce() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }


    // The draft version used 'format'.
//    private String getCredentialFormat(String credentialIdentifier){
//        Map<String, Object> credentialConfigurationsSupported = getIssuerMetadata().getCredentialConfigurationsSupported();
//        Object credentialConfigurationObject = credentialConfigurationsSupported.get(credentialIdentifier);
//        String format = "";
//        if (credentialConfigurationObject instanceof Map) {
//            @SuppressWarnings("unchecked")
//            Map<String, Object> configMap = (Map<String, Object>) credentialConfigurationObject;
//            format = (String) configMap.get("format");
//
//            System.out.println("Format for " + credentialIdentifier + " is: " + format);
//
//        } else {
//            throw new IllegalStateException("Credential configuration for " + credentialIdentifier + " is not a valid Map.");
//        }
//
//        return format;
//    }

    //todo : When acting as an Authorization Server substitute
//    public TokenResponse handleTokenRequest(TokenRequest request) {
//        String userId = null;
//        if ("authorization_code".equals(request.getGrant_type())) {
//            userId = issuerStates.remove(request.getCode());
//        } else if ("pre-authorized_code".equals(request.getGrant_type())) {
//            userId = preAuthorizedCodes.remove(request.getPre_authorized_code());
//        }
//
//        if (userId == null) {
//            throw new IllegalArgumentException("Invalid code or grant_type");
//        }
//
//        try {
//            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
//                    .subject(userId)
//                    .issuer(getIssuerMetadata().getCredentialIssuer())
//                    .expirationTime(new Date(new Date().getTime() + 60 * 1000))
//                    .build();
//
//            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
//            signedJWT.sign(sharedStateService.getSigner());
//
//            String accessToken = signedJWT.serialize();
//            String cNonce = generateCNonce();
//            sharedStateService.getCNonceStore().put(accessToken, cNonce);
//
//            TokenResponse response = new TokenResponse();
//            response.setAccessToken(accessToken);
//            response.setTokenType("bearer");
//            response.setExpiresIn(60);
//            response.setCNonce(cNonce);
//            return response;
//        } catch (JOSEException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private String generatePreAuthorizedCode() {
//        SecureRandom random = new SecureRandom();
//        byte[] bytes = new byte[32];
//        random.nextBytes(bytes);
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
//    }
}
