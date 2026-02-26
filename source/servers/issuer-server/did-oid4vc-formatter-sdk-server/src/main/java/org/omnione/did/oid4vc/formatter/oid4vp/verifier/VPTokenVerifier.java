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

package org.omnione.did.oid4vc.formatter.oid4vp.verifier;

import java.security.cert.X509Certificate;
import java.util.List;
import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.IdentifierResult;
import org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto.VerificationConfig;

/**
 * Common interface for credential verification.
 * Implementations should handle specific credential formats (SD-JWT, mDoc, etc.)
 */
public interface VPTokenVerifier {

  /**
   * Verifies the credential and returns the claims.
   *
   * @param credential the credential to verify
   * @param verificationConfig verification configuration
   * @return verified boolean
   * @throws FormatterException if verification fails
   */
  boolean verifyVerifiablePresentation(String credential, VerificationConfig verificationConfig) throws FormatterException;

  /**
   * Checks if this verifier supports the given credential.
   */
  boolean supports(String credential);

  /**
   * Return format type
   */
  String getFormat();

  /**
   * Extracts the issuer identifier from the credential.
   * Implementation varies by credential format.
   * 
   * Extraction priority:
   * 1. kid (Key ID) from JWT header
   * 2. x5c (X.509 Certificate Chain) - returns SHA-256 thumbprint of first certificate
   *
   * @param credential the credential string
   * @return IdentifierResult containing type and value, or null if neither kid nor x5c is present
   * @throws FormatterException if the credential format is invalid
   */
  IdentifierResult extractIssuerIdentifier(String credential) throws FormatterException;

  /**
   * Extracts the holder identifier from the credential.
   * Implementation varies by credential format (e.g., cnf.kid or cnf.x5c for SD-JWT).
   * 
   * Extraction priority:
   * 1. cnf.kid (Key ID in confirmation claim)
   * 2. cnf.x5c (X.509 Certificate Chain) - returns SHA-256 thumbprint of first certificate
   * 
   * Note: For SD-JWT, the cnf claim is OPTIONAL per RFC 9901 (Key Binding is optional),
   * so this method may return null for valid credentials without key binding.
   *
   * @param credential the credential string
   * @return IdentifierResult containing type and value, or null if cnf is not present
   * @throws FormatterException if the credential format is invalid
   */
  IdentifierResult extractHolderIdentifier(String credential) throws FormatterException;

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
   * Validates that the credential's presentation binding matches the expected values.
   * This verifies the credential was created for a specific verifier and request.
   * <p>
   * Implementation varies by credential format:
   * <ul>
   *   <li>SD-JWT: Validates 'aud' and 'nonce' claims in Key Binding JWT</li>
   *   <li>W3C VC/VP: Validates 'domain' and 'challenge' fields in VP proof</li>
   *   <li>mDoc: Validates corresponding fields in SessionTranscript</li>
   * </ul>
   *
   * @param credential the credential string
   * @param expectedClientId the expected client ID (aud/domain) value
   * @param expectedNonce the expected nonce (nonce/challenge) value
   * @return true if both values match, false otherwise
   */
  boolean validatePresentationBinding(String credential, String expectedClientId, String expectedNonce);

  /**
   * Validates the cryptographic signatures of the credential without checking presentation binding.
   * This performs pure signature verification using the provided public keys.
   * <p>
   * Implementation varies by credential format:
   * <ul>
   *   <li>SD-JWT: Verifies Issuer JWT signature and Key Binding JWT signature (if present)</li>
   *   <li>W3C VC/VP: Verifies VP proof signature and VC proof signature(s)</li>
   *   <li>mDoc: Verifies IssuerAuth and DeviceAuth signatures</li>
   * </ul>
   *
   * @param credential the credential string
   * @param issuerPublicKey Base64-encoded compressed public key for issuer signature verification
   * @param holderPublicKey Base64-encoded compressed public key for holder signature verification (can be null if not applicable)
   * @return true if all signatures are valid
   * @throws FormatterException if signature verification fails or credential format is invalid
   */
  boolean validateSignature(String credential, String issuerPublicKey, String holderPublicKey) throws FormatterException;

  /**
   * Validates the cryptographic signatures of the credential using X.509 certificate chain validation.
   * This method verifies the issuer signature by validating the certificate chain in the x5c header
   * against the provided trusted root certificates.
   * <p>
   * Implementation varies by credential format:
   * <ul>
   *   <li>SD-JWT: Verifies Issuer JWT signature using x5c certificate chain and Key Binding JWT signature (if present)</li>
   *   <li>W3C VC/VP: Not supported - throws FormatterException</li>
   *   <li>mDoc: Not yet implemented</li>
   * </ul>
   *
   * @param credential the credential string
   * @param trustedRoots List of trusted root X.509 certificates for chain validation
   * @return true if all signatures are valid
   * @throws FormatterException if signature verification fails, credential format is invalid, or X.509 validation is not supported
   */
  boolean validateSignatureWithX5c(String credential, List<X509Certificate> trustedRoots) throws FormatterException;

  /**
   * Extracts the bound client ID from the credential.
   * This is the audience/domain value that identifies the intended verifier.
   * <p>
   * Implementation varies by credential format:
   * <ul>
   *   <li>SD-JWT: Returns 'aud' claim from Key Binding JWT</li>
   *   <li>W3C VC/VP: Returns 'domain' field from VP proof</li>
   *   <li>mDoc: Returns client ID from SessionTranscript</li>
   * </ul>
   *
   * @param credential the credential string
   * @return the bound client ID value, or null if not present
   */
  String extractBoundClientId(String credential);

  /**
   * Extracts the bound nonce from the credential.
   * This is the challenge value used for replay protection.
   * <p>
   * Implementation varies by credential format:
   * <ul>
   *   <li>SD-JWT: Returns 'nonce' claim from Key Binding JWT</li>
   *   <li>W3C VC/VP: Returns 'challenge' field from VP proof</li>
   *   <li>mDoc: Returns nonce from SessionTranscript</li>
   * </ul>
   *
   * @param credential the credential string
   * @return the bound nonce value, or null if not present
   */
  String extractBoundNonce(String credential);
}