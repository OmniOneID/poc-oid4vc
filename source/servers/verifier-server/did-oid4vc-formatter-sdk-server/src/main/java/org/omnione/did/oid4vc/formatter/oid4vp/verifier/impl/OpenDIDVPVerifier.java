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

package org.omnione.did.oid4vc.formatter.oid4vp.verifier.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.oid4vc.formatter.exception.FormatterErrorCode;
import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.VPTokenVerifier;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.IdentifierResult;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.VerificationConfig;
import org.omnione.did.oid4vc.formatter.util.KeyUtil;
import org.omnione.did.opendidvc.core.verifier.OpenDIDVCVerifier;
import org.omnione.did.opendidvc.datamodel.VerifiablePresentation;
import org.omnione.did.opendidvc.exception.OpenDIDVCException;
import org.springframework.stereotype.Component;

/**
 * OpenDID Verifiable Presentation Verifier.
 * Implements VPTokenVerifier interface to verify OpenDID VC/VP format.
 */
@Slf4j
@Component
public class OpenDIDVPVerifier implements VPTokenVerifier {
  public static final String FORMAT_TYPE = "opendid_vc";

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Checks if the credential is an OpenDID VP format.
   * OpenDID VP is JSON-LD based with specific context and type.
   *
   * @param credential the credential string (JSON format)
   * @return true if this is an OpenDID VP, false otherwise
   */
  @Override
  public boolean supports(String credential) {
    if (credential == null || credential.trim().isEmpty()) {
      return false;
    }

    try {
      // Parse JSON using Jackson ObjectMapper
      Map<String, Object> credentialMap = objectMapper.readValue(
          credential, new TypeReference<Map<String, Object>>() {});

      // Check for @context field
      Object contextObj = credentialMap.get("@context");
      if (contextObj == null) {
        return false;
      }

      // Check for type field containing "VerifiablePresentation"
      Object typeObj = credentialMap.get("type");
      if (typeObj instanceof List) {
        List<?> types = (List<?>) typeObj;
        boolean hasVPType = types.stream()
            .anyMatch(t -> "VerifiablePresentation".equals(t.toString()));
        if (!hasVPType) {
          return false;
        }
      } else {
        return false;
      }

      // Check for W3C credentials context
      if (contextObj instanceof List) {
        List<?> contexts = (List<?>) contextObj;
        return contexts.stream()
            .anyMatch(c -> c.toString().contains("www.w3.org/ns/credentials"));
      }

      return false;

    } catch (Exception e) {
      log.debug("Failed to parse credential for support check: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public String getFormat() {
    return FORMAT_TYPE;
  }

  /**
   * Verifies an OpenDID Verifiable Presentation.
   *
   * @param credential the VP credential string (JSON format)
   * @param config verification configuration containing public keys and nonce
   * @return true if verification succeeds, false otherwise
   * @throws FormatterException if verification fails
   */
  @Override
  public boolean verifyVerifiablePresentation(String credential, VerificationConfig config) throws FormatterException {
    try {
      // Step 1: Parse VP from JSON string
      VerifiablePresentation vp = objectMapper.readValue(credential,
          VerifiablePresentation.class);

      log.info("=== VP Proof Info ===");
      log.info("VP proof.verificationMethod: {}", vp.getProof().getVerificationMethod());
      log.info("VP proof.proofValue: {}", vp.getProof().getProofValue());

      if (vp.getVerifiableCredential() != null && !vp.getVerifiableCredential().isEmpty()) {
        var vcProof = vp.getVerifiableCredential().get(0).getProof();
        log.info("=== VC Proof Info ===");
        log.info("VC proof.verificationMethod: {}", vcProof.getVerificationMethod());
        log.info("VC proof.proofValue: {}", vcProof.getProofValue());
      }

      log.info("=== Input Public Keys ===");
      log.info("config.holderPublicKey: {}", config.getHolderPublicKey());
      log.info("config.issuerPublicKey: {}", config.getIssuerPublicKey());
      log.info("config.nonce: {}", config.getNonce());
      log.info("config.clientId: {}", config.getClientId());

      log.debug("Parsed VP - ID: {}, Holder: {}", vp.getId(), vp.getHolder());

      // Step 2: Build public keys from config
      PublicKey holderPublicKey = KeyUtil.getPublicKeyObject(
          KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(config.getHolderPublicKey()))
      );

      PublicKey issuerPublicKey = KeyUtil.getPublicKeyObject(
          KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(config.getIssuerPublicKey()))
      );

      log.debug("Public keys loaded - Holder and Issuer keys ready");

      // Step 3: Create OpenDIDVCVerifier and verify
      OpenDIDVCVerifier verifier = new OpenDIDVCVerifier(holderPublicKey, issuerPublicKey);

      // Extract domain from clientId if needed
      // clientId format can be a URL, need to match with VP proof domain
      String expectedDomain = config.getClientId();

      OpenDIDVCVerifier.VerificationResult result = verifier.verify(
          vp,
          config.getNonce(),
          expectedDomain
      );

      log.info("OpenDID VP Verification Result: {}", result);
      log.info("VP Holder: {}", vp.getHolder());
      log.info("VP ID: {}", vp.getId());

      return result.isValid();

    } catch (OpenDIDVCException e) {
      log.error("OpenDID VP verification failed: {}", e.getMessage(), e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Failed to verify OpenDID VP", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Extracts the issuer verification method from the first VC in the VP.
   *
   * @param credential the VP credential string (JSON format)
   * @return IdentifierResult containing LD_VC_DID type and issuer verification method value
   * @throws FormatterException if extraction fails
   */
  @Override
  public IdentifierResult extractIssuerIdentifier(String credential) throws FormatterException {
    if (credential == null || credential.trim().isEmpty()) {
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_NULL);
    }

    try {
      // Parse VP
      VerifiablePresentation vp = objectMapper.readValue(credential,
          VerifiablePresentation.class);

      // Extract issuer verification method from the first VC
      if (vp.getVerifiableCredential() == null || vp.getVerifiableCredential().isEmpty()) {
        throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL,
            "VP does not contain any Verifiable Credentials");
      }

      String verificationMethod = vp.getVerifiableCredential().get(0).getProof().getVerificationMethod();

      if (verificationMethod == null || verificationMethod.trim().isEmpty()) {
        throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL,
            "Verification method not found in VC proof");
      }

      log.debug("Extracted issuer verification method: {}", verificationMethod);
      return new IdentifierResult(IdentifierResult.Type.LD_VC_DID, verificationMethod);

    } catch (FormatterException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to extract issuer identifier: {}", e.getMessage());
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, e.getMessage(), e);
    }
  }

  /**
   * Extracts the holder verification method from the VP.
   *
   * @param credential the VP credential string (JSON format)
   * @return IdentifierResult containing LD_VC_DID type and holder verification method value
   * @throws FormatterException if extraction fails
   */
  @Override
  public IdentifierResult extractHolderIdentifier(String credential) throws FormatterException {
    if (credential == null || credential.trim().isEmpty()) {
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_NULL);
    }

    try {
      // Parse VP
      VerifiablePresentation vp = objectMapper.readValue(credential,
          VerifiablePresentation.class);

      // Extract holder verification method from VP proof
      String verificationMethod = vp.getProof().getVerificationMethod();

      if (verificationMethod == null || verificationMethod.trim().isEmpty()) {
        throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_INVALID_CREDENTIAL,
            "Verification method not found in VP proof");
      }

      log.debug("Extracted holder verification method: {}", verificationMethod);
      return new IdentifierResult(IdentifierResult.Type.LD_VC_DID, verificationMethod);

    } catch (FormatterException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to extract holder identifier: {}", e.getMessage());
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_TOKEN_PARSE_FAILED, e.getMessage(), e);
    }
  }



  // ========================================================================================
  // Fine-grained verification methods
  // ========================================================================================
  // The following methods provide fine-grained control over the verification process,
  // allowing third-party implementations to separate signature verification from
  // presentation binding validation when needed.
  //
  // While verifyVerifiablePresentation() performs complete OID4VP VP Token verification
  // in a single call, some environments may require these steps to be separated.
  // ========================================================================================

  /**
   * Extracts the bound client ID (domain) from the VP proof.
   * In Open DID VC/VP, the domain field corresponds to the intended audience/verifier.
   *
   * @param credential the VP credential string (JSON format)
   * @return the domain value from VP proof, or null if not present
   */
  @Override
  public String extractBoundClientId(String credential) {
    if (credential == null || credential.trim().isEmpty()) {
      return null;
    }

    try {
      VerifiablePresentation vp = objectMapper.readValue(credential,
          VerifiablePresentation.class);

      if (vp.getProof() == null) {
        log.debug("VP proof not found");
        return null;
      }

      String domain = vp.getProof().getDomain();
      log.debug("Extracted domain from VP proof: {}", domain);
      return domain;

    } catch (Exception e) {
      log.debug("Failed to extract domain from VP: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Extracts the bound nonce (challenge) from the VP proof.
   * In Open DID VC/VP, the challenge field corresponds to the nonce for replay protection.
   *
   * @param credential the VP credential string (JSON format)
   * @return the challenge value from VP proof, or null if not present
   */
  @Override
  public String extractBoundNonce(String credential) {
    if (credential == null || credential.trim().isEmpty()) {
      return null;
    }

    try {
      VerifiablePresentation vp = objectMapper.readValue(credential,
          VerifiablePresentation.class);

      if (vp.getProof() == null) {
        log.debug("VP proof not found");
        return null;
      }

      String challenge = vp.getProof().getChallenge();
      log.debug("Extracted challenge from VP proof: {}", challenge);
      return challenge;

    } catch (Exception e) {
      log.debug("Failed to extract challenge from VP: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Validates that the VP's proof contains the expected domain (client ID) and challenge (nonce).
   *
   * @param credential the VP credential string (JSON format)
   * @param expectedClientId the expected domain value
   * @param expectedNonce the expected challenge value
   * @return true if both values match, false otherwise
   */
  @Override
  public boolean validatePresentationBinding(String credential, String expectedClientId, String expectedNonce) {
    String boundClientId = extractBoundClientId(credential);
    String boundNonce = extractBoundNonce(credential);

    if (boundClientId == null || boundNonce == null) {
      log.debug("VP proof not found or missing domain/challenge");
      return false;
    }

    boolean clientIdMatch = expectedClientId != null && expectedClientId.equals(boundClientId);
    boolean nonceMatch = expectedNonce != null && expectedNonce.equals(boundNonce);

    if (!clientIdMatch) {
      log.debug("Domain mismatch: expected={}, actual={}", expectedClientId, boundClientId);
    }
    if (!nonceMatch) {
      log.debug("Challenge mismatch: expected={}, actual={}", expectedNonce, boundNonce);
    }

    return clientIdMatch && nonceMatch;
  }

  /**
   * Validates the signatures of the OpenDID VP without checking presentation binding (domain/challenge).
   * This method verifies:
   * 1. VP proof signature using the provided holder public key
   * 2. VC proof signature(s) using the provided issuer public key
   *
   * @param credential the VP credential string (JSON format)
   * @param issuerPublicKey Base64-encoded compressed issuer public key
   * @param holderPublicKey Base64-encoded compressed holder public key
   * @return true if all signatures are valid
   * @throws FormatterException if signature verification fails
   */
  @Override
  public boolean validateSignature(String credential, String issuerPublicKey, String holderPublicKey) throws FormatterException {
    try {
      VerifiablePresentation vp = objectMapper.readValue(credential,
          VerifiablePresentation.class);

      log.debug("Parsed VP for signature validation - ID: {}, Holder: {}", vp.getId(), vp.getHolder());

      PublicKey holderPubKey = KeyUtil.getPublicKeyObject(
          KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(holderPublicKey))
      );

      PublicKey issuerPubKey = KeyUtil.getPublicKeyObject(
          KeyUtil.unCompressPublicKey(Base64.getDecoder().decode(issuerPublicKey))
      );

      OpenDIDVCVerifier verifier = new OpenDIDVCVerifier(holderPubKey, issuerPubKey);

      // Verify with null challenge/domain to skip presentation binding check
      OpenDIDVCVerifier.VerificationResult result = verifier.verify(vp, null, null);

      log.debug("OpenDID VP signature validation result: {}", result.isValid());
      return result.isValid();

    } catch (OpenDIDVCException e) {
      log.error("OpenDID VP signature validation failed: {}", e.getMessage(), e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Failed to validate OpenDID VP signature", e);
      throw new FormatterException(FormatterErrorCode.ERR_CODE_VP_VERIFICATION_FAILED, e.getMessage(), e);
    }
  }

  /**
   * X.509 certificate chain validation is not supported for OpenDID VP format.
   * OpenDID VP uses DID-based verification, not X.509 certificate chains.
   *
   * @param credential the VP credential string (JSON format)
   * @param trustedRoots List of trusted root X.509 certificates (not used)
   * @return never returns normally
   * @throws FormatterException always throws with ERR_CODE_X5C_VALIDATION_NOT_SUPPORTED
   */
  @Override
  public boolean validateSignatureWithX5c(String credential, List<X509Certificate> trustedRoots) throws FormatterException {
    throw new FormatterException(FormatterErrorCode.ERR_CODE_X5C_VALIDATION_NOT_SUPPORTED,
        "OpenDID VP format uses DID-based verification, not X.509 certificate chain validation");
  }
}