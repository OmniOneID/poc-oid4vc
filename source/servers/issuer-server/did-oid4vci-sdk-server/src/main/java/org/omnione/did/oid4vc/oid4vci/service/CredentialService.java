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

import org.omnione.did.oid4vc.oid4vci.dto.credential.*;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIErrorCode;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIException;
import org.omnione.did.oid4vc.oid4vci.property.IssuerProperties;
import org.omnione.did.oid4vc.oid4vci.config.IssuerSdkProperties;
import org.omnione.did.oid4vc.oid4vci.dto.credentialoffer.*;
import org.omnione.did.oid4vc.oid4vci.api.dto.PreAuthorizeResponse;
import org.omnione.did.oid4vc.oid4vci.dto.metadata.IssuerMetadataResponse;
import org.omnione.did.oid4vc.oid4vci.dto.nonce.NonceResponse;
import org.omnione.did.oid4vc.oid4vci.dto.notification.NotificationRequest;
import org.omnione.did.oid4vc.oid4vci.service.store.CNonceStore;
import org.omnione.did.oid4vc.oid4vci.service.store.CredentialOfferStore;
import org.omnione.did.oid4vc.oid4vci.service.store.SessionStore;
import org.omnione.did.oid4vc.oid4vci.service.store.CNonceStore;
import org.omnione.did.oid4vc.oid4vci.spi.ProtocolIssuer;
import org.omnione.did.oid4vc.oid4vci.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.omnione.did.oid4vc.oid4vci.util.VerifyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import java.math.BigInteger;

@Service
public class CredentialService {

    private static final Logger logger = LoggerFactory.getLogger(CredentialService.class);

    private final IssuerProperties issuerProperties;
    private final IssuerSdkProperties issuerSdkProperties;
    private final List<ProtocolIssuer> protocolIssuers;
    private final UserDataProvider userDataProvider;
    private final KeyDataProvider keyDataProvider;
    private final SharedStateService sharedStateService;
    private final ObjectMapper objectMapper;
    private final CredentialIssuerFeignService credentialIssuerFeignService;
    private final CredentialOfferStore credentialOfferStore;
    private final SessionStore sessionStore;
    private final CNonceStore cNonceStore;

    public CredentialService(IssuerProperties issuerProperties,
                             IssuerSdkProperties issuerSdkProperties,
                             List<ProtocolIssuer> protocolIssuers,
                             UserDataProvider userDataProvider,
                             KeyDataProvider keyDataProvider,
                             SharedStateService sharedStateService,
                             ObjectMapper objectMapper,
                             CredentialIssuerFeignService credentialIssuerFeignService,
                             CredentialOfferStore credentialOfferStore,
                             SessionStore sessionStore,
                             CNonceStore cNonceStore) {
        this.issuerProperties = issuerProperties;
        this.issuerSdkProperties = issuerSdkProperties;
        this.protocolIssuers = protocolIssuers;
        this.userDataProvider = userDataProvider;
        this.keyDataProvider = keyDataProvider;
        this.sharedStateService = sharedStateService;
        this.objectMapper = objectMapper;
        this.credentialIssuerFeignService = credentialIssuerFeignService;
        this.credentialOfferStore = credentialOfferStore;
        this.sessionStore = sessionStore;
        this.cNonceStore = cNonceStore;
    }

    public CredentialOfferResponse processCredentialOffer(String requestId, CredentialOfferRequest request) throws OID4VCIException {
        CredentialOfferResponse response = new CredentialOfferResponse();
        if (requestId.startsWith("p")) {
            PreAuthorizeResponse storedPreAuthorizeResponse = credentialOfferStore.consume(requestId);

            if (storedPreAuthorizeResponse == null) {
                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_OFFER_NOT_FOUND, "Invalid or expired offer request.");
            }
            logger.info("Retrieved from session: requestId={}, preAuthorizedCode={}", requestId,
                    storedPreAuthorizeResponse.getPreAuthorizedCode());
            response = createCredentialOffer(request, storedPreAuthorizeResponse, Constants.PRE_AUTHORIZED_CODE_TYPE);
        } else if (requestId.startsWith("a")) {
            response = createCredentialOffer(request, Constants.AUTHORIZATION_CODE_TYPE);
        }
        return response;
    }

    public TestCredentialOfferResponse createTestCredentialOffer(String userId) throws OID4VCIException {
        logger.info("createCredentialOfferForTest request");
        PreAuthorizeResponse preAuthorizeResponse = credentialIssuerFeignService.getPreAuthorizedCode(userId);
        CredentialOfferRequest request = new CredentialOfferRequest();
        request.setUserId(userId);
        CredentialOfferResponse response = createCredentialOffer(request, preAuthorizeResponse,
                Constants.PRE_AUTHORIZED_CODE_TYPE);
        TestCredentialOfferResponse testResponse = new TestCredentialOfferResponse();
        testResponse.setCredentialIssuer(response.getCredentialIssuer());
        testResponse.setGrants(response.getGrants());
        testResponse.setCredentialIssuer(response.getCredentialIssuer());
        if (preAuthorizeResponse != null)
            testResponse.setTxCode(preAuthorizeResponse.getUserPin());

        return testResponse;
    }

    public CredentialOfferResponse createCredentialOffer(CredentialOfferRequest request,
                                                         PreAuthorizeResponse preAuthorizeResponse, String grantType) throws OID4VCIException {
        GrantsDto grantsDto = new GrantsDto();

        // Add Authorization Code Flow if requested
        if (Constants.AUTHORIZATION_CODE_TYPE.equals(grantType) || "authorization_code".equals(grantType)) {
            String issuerState = generateIssuerState();
            sessionStore.saveIssuerState(issuerState, request.getUserId());

            AuthorizationCodeGrantDto authCodeGrant = new AuthorizationCodeGrantDto();
            authCodeGrant.setIssuerState(issuerState);

            grantsDto.setAuthorizationCode(authCodeGrant);
        }

        // Add Pre-Authorized Code Flow if requested AND preAuthorizeResponse is provided
        if (("pre-authorized_code".equals(grantType) || Constants.PRE_AUTHORIZED_CODE_TYPE.equals(grantType)) 
                && preAuthorizeResponse != null) {
            String preAuthorizedCode = preAuthorizeResponse.getPreAuthorizedCode();
            String userPin = preAuthorizeResponse.getUserPin();
            String issuerState = generateIssuerState();

            sessionStore.saveIssuerState(issuerState, request.getUserId());
            sessionStore.savePreAuthorizedCode(preAuthorizedCode, request.getUserId());

            PreAuthorizedCodeGrantDto preAuthGrant = new PreAuthorizedCodeGrantDto();
            preAuthGrant.setPreAuthorizedCode(preAuthorizedCode);
            preAuthGrant.setIssuerState(issuerState);

            if (userPin != null && !userPin.isEmpty()) {
                TxCodeDto txCodeDto = new TxCodeDto();
                txCodeDto.setInputMode("numeric");
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

    public CredentialOfferResponse createCredentialOffer(CredentialOfferRequest request, String grantType) throws OID4VCIException {
        if (!grantType.equals(Constants.AUTHORIZATION_CODE_TYPE)) {
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_GENERAL_INVALID_PARAMETER, "This method is only for authorization_code grant type");
        }
        return createCredentialOffer(request, null, grantType);
    }

    private Set<String> getConfigurationIds() throws OID4VCIException {
        IssuerMetadataResponse metadata = getIssuerMetadata();
        Map<String, Object> configurationsSupported = metadata.getCredentialConfigurationsSupported();
        
        if (configurationsSupported == null || configurationsSupported.isEmpty()) {
            return Collections.emptySet();
        }

        return configurationsSupported.keySet();
    }

    public IssuerMetadataResponse getIssuerMetadata() throws OID4VCIException {
        String dataDir = issuerProperties.getDataDir();
        String metadataFilePath = issuerProperties.getMetadataFilePath();
        if (metadataFilePath == null || metadataFilePath.isBlank()) {
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_METADATA_LOAD_FAILED, "issuer.metadata-file-path is not configured in application.yml");
        }

        Path fullPath = Paths.get(dataDir, metadataFilePath);

        try (InputStream is = Files.newInputStream(fullPath)) {
            return objectMapper.readValue(is, IssuerMetadataResponse.class);
        } catch (IOException e) {
            logger.error("Failed to load issuer metadata from path: {}", fullPath, e);
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_METADATA_LOAD_FAILED, "Failed to load issuer metadata from path: " + fullPath, e);
        }
    }

    public Object issueCredential(CredentialRequest request, Jwt accessToken) throws OID4VCIException {
        boolean deferred = false; // todo: Figure out how to handle this

        if (deferred) {
            String transactionId = generateTransactionId();
            sessionStore.saveDeferredCredentialRequest(transactionId, request);
            DeferredIssuanceResponse response = new DeferredIssuanceResponse();
            response.setTransactionId(transactionId);
            return response;
        } else {
            return issueImmediateCredential(request, accessToken);
        }
    }

    public CredentialResponse getDeferredCredential(String transactionId, Jwt accessToken) throws OID4VCIException {
        CredentialRequest request = sessionStore.consumeDeferredCredentialRequest(transactionId);
        if (request == null) {
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_ISSUE_DEFERRED_TRANSACTION_NOT_FOUND, "Invalid transaction_id");
        }
        return issueImmediateCredential(request, accessToken);
    }

    public NonceResponse handleNonce() {
        NonceResponse response = new NonceResponse();
        String cNonce = generateCNonce();
        response.setCNonce(cNonce);

        cNonceStore.save(cNonce);

        return response;
    }

    public void handleNotification(NotificationRequest request) {
        logger.info("Received notification: {}", request.getNotificationId());
        // todo: Figure out how to handle this
    }

    private CredentialResponse issueImmediateCredential(CredentialRequest request, Jwt accessToken) throws OID4VCIException {
        // todo: Clarify if using credential_configuration_id or credentialIdentifier.
        try {
            final String credentialConfigurationId = request.getCredentialConfigurationId();
            final String credentialIdentifier = request.getCredentialIdentifier();

            if ((credentialConfigurationId == null || credentialConfigurationId.isBlank())
                    && (credentialIdentifier == null || credentialIdentifier.isBlank())) {
                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_GENERAL_INVALID_PARAMETER,
                        "Either 'credential_configuration_id' or 'credential_identifier' MUST be present.");
            }
            if (credentialConfigurationId != null && credentialIdentifier != null) {
                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_GENERAL_INVALID_PARAMETER,
                        "'credential_configuration_id' and 'credential_identifier' MUST NOT be used together.");
            }

            if (request.getProofs() != null) {
                Proofs proofs = request.getProofs();
                boolean hasDiVp = proofs.getDiVp() != null && !proofs.getDiVp().isEmpty();
                boolean hasJwt = proofs.getJwt() != null && !proofs.getJwt().isEmpty();
                boolean hasAttestation = proofs.getAttestation() != null && !proofs.getAttestation().isEmpty();

                int proofCount = (hasDiVp ? 1 : 0) + (hasJwt ? 1 : 0) + (hasAttestation ? 1 : 0);

                if (proofCount == 0) {
                    throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_MISSING, "The 'proofs' object is empty.");
                }
                if (proofCount > 1) {
                    throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID,
                            "Multiple proof types detected. Provide only one of 'di_vp', 'jwt', or 'attestation'.");
                }

                if (hasJwt) {
                    for (String jwtStr : proofs.getJwt()) {
                        SignedJWT signedJWT = SignedJWT.parse(jwtStr);
                        
                        // Extract Wallet Public Key (JWK) from Header
                        if (signedJWT.getHeader().getJWK() != null) {
                            Map<String, Object> jwk = signedJWT.getHeader().getJWK().toJSONObject();
                            logger.info("Extracted JWK from proof header: {}", jwk);
                            // keyInfo는 나중에 ProtocolIssuer에서 사용됨
                        }

                        String nonceFromProof = signedJWT.getJWTClaimsSet().getStringClaim("nonce");
                        if (nonceFromProof != null && !nonceFromProof.isEmpty()) {
                            if (cNonceStore.contains(nonceFromProof)) {
                                logger.info("c_nonce verification successful. Removing from store: {}", nonceFromProof);
                                cNonceStore.remove(nonceFromProof);
                            } else {
                                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID_NONCE, "c_nonce verification failed.");
                            }
                        }

                        handleJwtSignatureVerification(jwtStr, signedJWT);
                    }
                }
                else if (hasDiVp) {
                    for (String vpStr : proofs.getDiVp()) {
                        // TODO: VP 파싱 로직에서 challenge 추출 필요
                        // String challengeFromProof = parseChallengeFromVp(vpStr); 
                        String challengeFromProof = null; // 임시

                        if (challengeFromProof != null && !challengeFromProof.isEmpty()) {
                            if (cNonceStore.contains(challengeFromProof)) {
                                logger.info("c_nonce(challenge) verification successful. Removing from store: {}", challengeFromProof);
                                cNonceStore.remove(challengeFromProof);
                            } else {
                                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID_NONCE, "c_nonce verification failed.");
                            }
                        }
                        // TODO: VP 서명 검증 호출 (domain 필드 검증 포함)
                    }
                }
                else if (hasAttestation) {
                    for (String attStr : proofs.getAttestation()) {
                        SignedJWT attestationJWT = SignedJWT.parse(attStr);

                        // c_nonce 검증 (Attestation은 JWT 형식이므로 'nonce' 파라미터 사용)
                        String nonceFromAtt = attestationJWT.getJWTClaimsSet().getStringClaim("nonce");
                        if (nonceFromAtt != null && !nonceFromAtt.isEmpty()) {
                            if (cNonceStore.contains(nonceFromAtt)) {
                                logger.info("c_nonce(attestation) verification successful. Removing from store: {}", nonceFromAtt);
                                cNonceStore.remove(nonceFromAtt);
                            } else {
                                logger.info("c_nonce(attestation) not found in store. Skipping validation.");
                            }
                        }
                        // TODO: Attestation 서명 및 alg 검증 호출
                    }
                }
            } else {
                logger.info("No proofs provided. Skipping proof validation.");
            }

            // Extract User ID from Access Token
            String userId = accessToken.getSubject();
            if (userId == null) {
                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_ISSUE_USER_NOT_FOUND, "User ID (sub) is missing from the access token.");
            }

            String credentialType = null;

            if (credentialIdentifier != null && !credentialIdentifier.isBlank()) {
                credentialType = credentialIdentifier;

            } else if (credentialConfigurationId != null && !credentialConfigurationId.isBlank()) {
                Set<String> identifiers = issuerSdkProperties.getIdentifiersByConfigId(credentialConfigurationId);
                if (identifiers != null && !identifiers.isEmpty()) {
                    credentialType = identifiers.iterator().next(); // Use the first identifier
                } else {
                    credentialType = credentialConfigurationId; // Fallback
                }
            }

            logger.info("  Determined credentialType for issuer lookup: [{}]", credentialType);

            Map<String, Object> proofJwk = null;
            if (request.getProofs() != null && request.getProofs().getJwt() != null && !request.getProofs().getJwt().isEmpty()) {
                try {
                    SignedJWT signedJWT = SignedJWT.parse(request.getProofs().getJwt().get(0));
                    if (signedJWT.getHeader().getJWK() != null) {
                        proofJwk = signedJWT.getHeader().getJWK().toJSONObject();
                    }
                } catch (ParseException e) {
                    logger.warn("Failed to parse JWT proof for JWK extraction", e);
                }
            }

            Object rawCredential = null;

            // ProtocolIssuer
            if (credentialType != null) {
                final String type = credentialType;
                Optional<ProtocolIssuer> protocolIssuer = protocolIssuers.stream()
                        .filter(issuer -> issuer.supports(type))
                        .findFirst();

                if (protocolIssuer.isPresent()) {
                    logger.info("Found ProtocolIssuer for type: {}", credentialType);
                    Map<String, Object> claims = userDataProvider.getUserClaims(userId, credentialType);
                    Map<String, Object> keyInfo = keyDataProvider.getKeyInfo(userId, credentialType);
                    
                    // Add Wallet's public key from proof to keyInfo
                    if (proofJwk != null) {
                        keyInfo.put("proof_jwk", proofJwk);
                    }
                    
                    rawCredential = protocolIssuer.get().issueCredential(claims, keyInfo);
                }
            }

            CredentialResponse response = new CredentialResponse();

            Credential credential = new Credential();
            credential.setCredential(rawCredential);
            List<Credential> credentialList = new ArrayList<>();
            credentialList.add(credential);
            response.setCredentials(credentialList);

            return response;

        } catch (ParseException e) {
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_PARSE_FAILED, "Failed to parse proof JWT", e);
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

    private void handleJwtSignatureVerification(String jwt, SignedJWT signedJWT) throws OID4VCIException {
        JWSHeader header = signedJWT.getHeader();
        if (header.getX509CertChain() != null && !header.getX509CertChain().isEmpty()) {
            verifyProofWithX5c(jwt, header, signedJWT);
        } else if (header.getKeyID() != null) {
            // TODO: DID(KeyID)를 이용한 검증 로직 수행
        }
    }
    private void verifyProofWithX5c(String jwtString, JWSHeader header, SignedJWT signedJWT) throws OID4VCIException {
        try {
            List<com.nimbusds.jose.util.Base64> x5c = header.getX509CertChain();
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            List<X509Certificate> certificates = new ArrayList<>();

            for (com.nimbusds.jose.util.Base64 certBase64 : x5c) {
                byte[] certBytes = certBase64.decode();
                certificates.add((X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes)));
            }

            // Chain Validation (including Root CA)
            X509Certificate rootCert = null;
            String rootCaFileName = issuerProperties.getRootCaFileName() != null ? issuerProperties.getRootCaFileName() : "rootCA.crt";
            Path rootCaPath = Paths.get(issuerProperties.getDataDir(), rootCaFileName);
            try (InputStream is = Files.newInputStream(rootCaPath)) {
                rootCert = (X509Certificate) certFactory.generateCertificate(is);
            } catch (IOException e) {
                 throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID, "Failed to load Root CA for verification from: " + rootCaPath);
            }

            for (int i = 0; i < certificates.size(); i++) {
                X509Certificate cert = certificates.get(i);
                cert.checkValidity(); // Check dates

                if (i < certificates.size() - 1) {
                    X509Certificate issuerCert = certificates.get(i + 1);
                    cert.verify(issuerCert.getPublicKey()); // Verify signature against issuer
                } else {
                    // Last cert in x5c, verify against Root CA
                    try {
                        cert.verify(rootCert.getPublicKey());
                    } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException |
                             NoSuchProviderException | SignatureException e) {
                         // Check if it IS the root (self-signed handling if x5c included root)
                         if (!Arrays.equals(cert.getEncoded(), rootCert.getEncoded())) {
                             throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID, "Certificate chain not trusted by local Root CA");
                         }
                    }
                }
            }
            logger.info("x5c chain verification successful");

            // Verify Signature using the leaf certificate's public key
            X509Certificate leafCert = certificates.get(0);
            java.security.PublicKey publicKey = leafCert.getPublicKey();

            if (publicKey instanceof ECPublicKey) {
                try {
                    // Compress Public Key
                    ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
                    java.security.spec.ECPoint w = ecPublicKey.getW();
                    BigInteger x = w.getAffineX();
                    BigInteger y = w.getAffineY();

                    byte[] xBytes = x.toByteArray();
                    if (xBytes.length > 32) {
                        byte[] tmp = new byte[32];
                        System.arraycopy(xBytes, xBytes.length - 32, tmp, 0, 32);
                        xBytes = tmp;
                    } else if (xBytes.length < 32) {
                        byte[] tmp = new byte[32];
                        System.arraycopy(xBytes, 0, tmp, 32 - xBytes.length, xBytes.length);
                        xBytes = tmp;
                    }

                    byte[] compressed = new byte[33];
                    compressed[0] = y.testBit(0) ? (byte) 0x03 : (byte) 0x02;
                    System.arraycopy(xBytes, 0, compressed, 1, 32);

                    String compressedPubKeyMultibase = MultiBaseUtils.encode(compressed, MultiBaseType.base64);

                    String signingInput = jwtString.substring(0, jwtString.lastIndexOf('.'));
                    String signature = signedJWT.getSignature().toString();

                    VerifyUtil.verifySignature(signingInput, signature, compressedPubKeyMultibase);
                } catch (CryptoException e) {
                    logger.error("Proof signature verification failed via KeyUtil: {}", e.getMessage());
                    throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID, "Proof signature verification failed");
                }
            } else {
                throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID, "Unsupported public key type in x5c leaf certificate");
            }

        } catch (NullPointerException | CertificateException | NoSuchAlgorithmException | InvalidKeyException |
                 NoSuchProviderException | SignatureException e) {
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_PROOF_INVALID, "x5c verification failed: " + e.getMessage(), e);
        }
    }
}
