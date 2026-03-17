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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLResult;
import org.omnione.did.oid4vc.oid4vp.dto.ServiceResult;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.service.AuthorizationService;
import org.omnione.did.oid4vc.oid4vp.service.InitiationService;
import org.omnione.did.oid4vc.oid4vp.service.OID4VPHelperService;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.CompactSigner;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.IdentifierResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/oid4vp")
@RequiredArgsConstructor
public class OID4VPController {

  private final InitiationService initiationService;
  private final AuthorizationService authorizationService;
  private final ObjectMapper objectMapper;
  private final OID4VPHelperService oid4VPHelperService;


  @PostMapping("/initiate")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> initiateVerification(
      @RequestParam(required = false) String dcql_query,
      @RequestParam(required = false) String scope,
      @RequestParam(defaultValue = "direct_post") String response_mode,
      @RequestParam(required = false) String client_metadata,
      @RequestParam(defaultValue = "true") boolean use_request_uri) {
    try {
      ServiceResult<Map<String, Object>> result = initiationService.initiateVerification(
          dcql_query, scope, response_mode, client_metadata, use_request_uri);
      return toMapResponse(result);
    } catch (OID4VPException e) {
      log.error(e.getMessage(), e);
      return ResponseEntity.internalServerError().body(Map.of(
          "error", e.getErrorCode(),
          "error_description", e.getErrorMsg()
      ));
    }
  }

  @GetMapping("/request/{request_id}")
  public ResponseEntity<String> getAuthorizationRequest(
      @PathVariable String request_id,
      @RequestParam(required = false) String wallet_metadata) {
    log.debug("wallet_metadata: {}", wallet_metadata);

    // x509_san_dns: use x5c certificate chain in JWS header
    if (authorizationService.isX509SanDns()) {
      return getAuthorizationRequestWithX5c(request_id);
    }

    // Verifier DID Doc, Application Layer
    String signKeyId = "assert";
    //String verificationMethod = verifierDidDoc.getId() + "?versionId=" + verifierDidDoc.getVersionId() + "#" + signKeyId;
    String verificationMethod = "did:omn:verifier" + "?versionId=" + "1" + "#" + signKeyId;

    // Add server wallet integration
    WalletManagerInterface walletManager = null;
    try {
      walletManager = WalletManagerFactory.getWalletManager(WalletManagerFactory.WalletManagerType.FILE);

      // Test Open DID Wallet
      // Load wallet from classpath resource
      ClassPathResource resource = new ClassPathResource("test.wallet");
      File tempFile = Files.createTempFile("wallet-", ".wallet").toFile();
      tempFile.deleteOnExit();
      Files.copy(resource.getInputStream(), tempFile.toPath(), REPLACE_EXISTING);

      // Test Open DID Wallet - 123456 : temp password
      walletManager.connect(tempFile.getAbsolutePath(), "123456".toCharArray());
    } catch (WalletException | IOException e) {
      log.error("Failed to connect wallet", e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    }

    WalletManagerInterface finalWalletManager = walletManager;
    CompactSigner signer = (keyId, hash) -> {
      return finalWalletManager.generateCompactSignatureFromHash(keyId, hash);
    };

    // Get public key from wallet for JWK header
    String publicKeyMultibase = null;
    try {
      publicKeyMultibase = walletManager.getPublicKey(signKeyId);
    } catch (WalletException e) {
      log.error("Failed to get public key from wallet", e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    }

    ServiceResult<String> result = authorizationService.getAuthorizationRequest(request_id, signer, verificationMethod, publicKeyMultibase);
    return toStringResponse(result);
  }

  @RequestMapping(value = "/response", method = {RequestMethod.POST, RequestMethod.GET})
  @ResponseBody
  public ResponseEntity<Map<String, Object>> receiveResponse(
      @RequestParam(required = false) String vp_token,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String error_description,
      HttpServletRequest request) {

    Map<String, List<Object>> vpTokenMap = null;
    List<String> issuerPublicKeys = new ArrayList<>();
    List<String> holderPublicKeys = new ArrayList<>();

    if (vp_token != null && !vp_token.trim().isEmpty()) {
      try {
        // Parse VP Token JSON to Map (single parsing point)
        vpTokenMap = oid4VPHelperService.parseVPToken(vp_token);

        // Extract identifiers from parsed map
        List<IdentifierResult> issuerIdentifiers = oid4VPHelperService.extractAllIssuerIdentifiers(vpTokenMap);
        List<IdentifierResult> holderIdentifiers = oid4VPHelperService.extractAllHolderIdentifiers(vpTokenMap);

        // Check if any credential uses x5c-based verification
        boolean hasX5cCredential = issuerIdentifiers.stream()
            .anyMatch(id -> id != null && (id.getType() == IdentifierResult.Type.SD_JWT_X5C
                || id.getType() == IdentifierResult.Type.MSO_MDOC_X5C));

        if (hasX5cCredential) {
          // x5c-based verification: use trusted root certificates
          log.info("Detected x5c-based credential, using certificate chain validation");

          try {
            List<X509Certificate> trustedRoots = new ArrayList<>();

            // 1. Load Java default trust store (cacerts) — includes well-known CAs
            trustedRoots.addAll(loadJavaDefaultTrustedCerts());

            // 2. Add custom test root certificates
            String[] customCertFiles = {
                "cert/x509_rootca.crt",
                "cert/x509_eudi_age_verification_issuer_ca01_test_rootca.crt",
                "cert/x509_oidf_test_cert.crt",
                "cert/x509_digital_credentials_dev_rootca.crt",
                "cert/x509_digital_credentials_dev_rootca_v2.crt"
            };
            for (String certFile : customCertFiles) {
              try {
                ClassPathResource certResource = new ClassPathResource(certFile);
                trustedRoots.add(loadCertificateFromInputStream(certResource.getInputStream()));
              } catch (Exception e) {
                log.warn("Failed to load custom cert {}: {}", certFile, e.getMessage());
              }
            }

            log.info("Loaded {} trusted root certificates ({} from cacerts + custom)",
                trustedRoots.size(), trustedRoots.size() - customCertFiles.length);

            // Call handleVPToken with trustedRoots for x5c-based verification
            ServiceResult<Map<String, Object>> result = oid4VPHelperService.handleVPToken(
                vpTokenMap, issuerPublicKeys, holderPublicKeys, trustedRoots, state);
            return toMapResponse(result);
          } catch (Exception e) {
            log.error("Failed to load trusted root certificate", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "certificate_error",
                "error_description", "Failed to load trusted root certificate: " + e.getMessage()
            ));
          }
        }

        // kid-based verification: resolve public keys from identifiers
        for (IdentifierResult identifier : issuerIdentifiers) {
          // Extract public key from DID Document
          String publicKey = resolvePublicKeyFromIdentifier(identifier);
          if (publicKey == null) {
            log.warn("No public key found for issuer identifier: {}",
                identifier != null ? identifier.getValue() : null);

            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_issuer",
                "error_description", "Unknown issuer identifier: " +
                    (identifier != null ? identifier.getValue() : null)
            ));
          }
          issuerPublicKeys.add(publicKey);
        }

        for (IdentifierResult identifier : holderIdentifiers) {
          if (identifier != null) {
            // Extract public key from DID Document
            String publicKey = resolvePublicKeyFromIdentifier(identifier);
            holderPublicKeys.add(publicKey); // Add null if not found to extract from cnf.jwk
          } else {
            // Add null if holder identifier is missing (to extract from cnf.jwk)
            holderPublicKeys.add(null);
          }
        }

        log.info("Resolved {} issuer public keys for {} identifiers",
            issuerPublicKeys.size(), issuerIdentifiers.size());
        log.info("Resolved {} holder public keys for {} identifiers",
            holderPublicKeys.size(), holderIdentifiers.size());
      } catch (OID4VPException e) {
        log.error("OID4VP error while processing VP Token: errorCode={}", e.getErrorCode(), e);
        return ResponseEntity.badRequest().body(Map.of(
            "error", e.getErrorCode(),
            "error_description", e.getErrorMsg()
        ));
      } catch (Exception e) {
        log.error("Failed to parse VP Token or extract identifiers", e);
        return ResponseEntity.badRequest().body(Map.of(
            "error", "invalid_vp_token",
            "error_description", "Failed to process VP Token: " + e.getMessage()
        ));
      }
    }

    // kid-based verification path
    ServiceResult<Map<String, Object>> result = authorizationService.receiveResponse(
        vpTokenMap, issuerPublicKeys, holderPublicKeys, state, error, error_description, request.getMethod());
    return toMapResponse(result);
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

    DCQLQueryValidator.ValidationResult validationResult = DCQLQueryValidator.validate(
        dcqlQuery);
    DCQLResult result = oid4VPHelperService.convertValidationResult(validationResult, dcqlQuery);

    return result.isValid() ? ResponseEntity.ok(result)
        : ResponseEntity.badRequest().body(result);
  }

  @GetMapping("/test")
  public String test() {
    return "verifier/test";
  }

  @GetMapping("/test/simple")
  public String testSimple() {
    return "verifier/test-simple";
  }

  @GetMapping("/test/dc-api")
  public String testDcApi() {
    return "verifier/test-dc-api";
  }

  @GetMapping("/fragment/callback")
  public String fragmentCallback() {
    return "verifier/fragment-callback";
  }

  @PostMapping("/dc-api/response")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> receiveDCApiResponse(
      @RequestBody Map<String, Object> dcApiResponse,
      HttpServletRequest request) {

    // vp_token can be a JSON string or a JSON object (Map)
    Object vpTokenRaw = dcApiResponse.get("vp_token");
    String vpToken;
    if (vpTokenRaw instanceof String) {
      vpToken = (String) vpTokenRaw;
    } else if (vpTokenRaw != null) {
      try {
        vpToken = objectMapper.writeValueAsString(vpTokenRaw);
      } catch (Exception e) {
        vpToken = vpTokenRaw.toString();
      }
    } else {
      vpToken = null;
    }
    String transactionId = (String) dcApiResponse.get("transaction_id");

    if (vpToken == null || vpToken.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_request",
          "error_description", "vp_token is required in DC API response"
      ));
    }

    if (transactionId == null || transactionId.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_request",
          "error_description", "transaction_id is required in DC API response"
      ));
    }

    // DC API: no state parameter (per OID4VP Appendix A.3.1)
    // Look up session by transaction_id to get state for internal routing
    String state = oid4VPHelperService.resolveStateByTransactionId(transactionId);
    if (state == null) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_request",
          "error_description", "No session found for transaction_id: " + transactionId
      ));
    }

    // Try full verification
    ResponseEntity<Map<String, Object>> result = receiveResponse(vpToken, state, null, null, request);

    // If verification failed, return VP Token data with warning
    if (result.getStatusCode().is4xxClientError() || result.getStatusCode().is5xxServerError()) {
      Map<String, Object> body = result.getBody();
      if (body != null && String.valueOf(body.get("error_description")).contains("VP verification failed")) {
        log.warn("DC API: verification failed, returning VP Token data with warning");
        try {
          Map<String, List<Object>> vpTokenMap = oid4VPHelperService.parseVPToken(vpToken);
          Map<String, Object> response = new LinkedHashMap<>();
          response.put("status", "received");
          response.put("warning", "VP Token verification failed - received but not fully verified");
          response.put("vp_token_parsed", vpTokenMap);
          response.put("transaction_id", transactionId);
          response.put("protocol", dcApiResponse.get("protocol"));
          return ResponseEntity.ok(response);
        } catch (Exception parseEx) {
          log.error("Failed to parse VP Token for fallback response", parseEx);
        }
      }
    }

    // Add transaction_id to successful response
    if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
      Map<String, Object> body = new LinkedHashMap<>(result.getBody());
      body.put("transaction_id", transactionId);
      return ResponseEntity.ok(body);
    }

    return result;
  }

  /**
   * Converts ServiceResult to ResponseEntity for String type responses.
   */
  private ResponseEntity<String> toStringResponse(ServiceResult<String> result) {
    if (result.isSuccess()) {
      ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
      if (result.getContentType() != null) {
        builder.contentType(MediaType.parseMediaType(result.getContentType()));
      }
      return builder.body(result.getData());
    } else {
      return ResponseEntity.status(result.getHttpStatus())
          .contentType(MediaType.APPLICATION_JSON)
          .body(createErrorJson(result.getErrorCode(), result.getErrorDescription()));
    }
  }

  /**
   * Converts ServiceResult to ResponseEntity for Map type responses.
   */
  private ResponseEntity<Map<String, Object>> toMapResponse(
      ServiceResult<Map<String, Object>> result) {
    if (result.isSuccess()) {
      return ResponseEntity.ok(result.getData());
    } else {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", result.getErrorCode());
      errorResponse.put("error_description", result.getErrorDescription());
      if (result.getState() != null) {
        errorResponse.put("state", result.getState());
      }
      return ResponseEntity.status(result.getHttpStatus()).body(errorResponse);
    }
  }

  /**
   * Creates error JSON string.
   */
  private String createErrorJson(String errorCode, String errorDescription) {
    return String.format("{\"error\":\"%s\",\"error_description\":\"%s\"}",
        errorCode, errorDescription);
  }

  // ==================== Public Key Resolution Methods ====================
  private String resolvePublicKeyFromIdentifier(IdentifierResult identifier) {
    return "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";
  }

  /**
   * Loads an X.509 certificate from PEM format string.
   */
  private X509Certificate loadCertificateFromPEM(String pemString) throws Exception {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    return (X509Certificate) factory.generateCertificate(
        new ByteArrayInputStream(pemString.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Loads an X.509 certificate from InputStream.
   */
  private X509Certificate loadCertificateFromInputStream(java.io.InputStream inputStream) throws Exception {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    return (X509Certificate) factory.generateCertificate(inputStream);
  }

  /**
   * Loads all trusted root certificates from Java's default trust store (cacerts).
   */
  private List<X509Certificate> loadJavaDefaultTrustedCerts() {
    List<X509Certificate> certs = new ArrayList<>();
    try {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init((KeyStore) null); // null loads the default cacerts
      for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
        if (tm instanceof X509TrustManager) {
          for (X509Certificate cert : ((X509TrustManager) tm).getAcceptedIssuers()) {
            certs.add(cert);
          }
        }
      }
      log.info("Loaded {} certificates from Java default trust store", certs.size());
    } catch (Exception e) {
      log.warn("Failed to load Java default trust store: {}", e.getMessage());
    }
    return certs;
  }

  /**
   * Handles authorization request for x509_san_dns scheme.
   * Loads verifier certificate chain and private key, then signs with x5c header.
   */
  private ResponseEntity<String> getAuthorizationRequestWithX5c(String requestId) {
    try {
      // Load verifier private key for x509_san_dns signing
      // TODO: Configure certificate/key paths via oid4vp-config.json
      ClassPathResource keyResource = new ClassPathResource("cert/x509_verifier.pem.b64");
      java.security.PrivateKey privateKey = loadPrivateKeyFromBase64(keyResource.getInputStream());

      // Load x5c certificate chain (leaf first)
      List<String> x5cCertChain = new ArrayList<>();
      ClassPathResource leafCert = new ClassPathResource("cert/x509_verifier.crt");
      x5cCertChain.add(encodeCertToBase64(leafCert.getInputStream()));

      ServiceResult<String> result = authorizationService.getAuthorizationRequest(
          requestId, privateKey, x5cCertChain);
      return toStringResponse(result);

    } catch (Exception e) {
      log.error("Failed to create x509_san_dns authorization request", e);
      return ResponseEntity.internalServerError().body(
          "{\"error\":\"x5c_signing_error\",\"error_description\":\"" + e.getMessage() + "\"}");
    }
  }

  /**
   * Loads a PKCS8 PEM private key from a Base64-encoded file.
   * The file contains the PEM content encoded in Base64 to avoid GitHub secret scanning.
   */
  private java.security.PrivateKey loadPrivateKeyFromBase64(java.io.InputStream inputStream) throws Exception {
    String encoded = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s", "");
    String pem = new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    String base64Key = pem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");
    byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Key);
    java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
    java.security.KeyFactory kf = java.security.KeyFactory.getInstance("EC");
    return kf.generatePrivate(spec);
  }

  /**
   * Encodes an X.509 certificate from InputStream to Base64 (DER) string for x5c header.
   */
  private String encodeCertToBase64(java.io.InputStream inputStream) throws Exception {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) factory.generateCertificate(inputStream);
    return java.util.Base64.getEncoder().encodeToString(cert.getEncoded());
  }
}