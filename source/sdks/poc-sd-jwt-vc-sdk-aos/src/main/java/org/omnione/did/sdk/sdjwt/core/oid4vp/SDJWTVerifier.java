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

package org.omnione.did.sdk.sdjwt.core.oid4vp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import org.omnione.did.sdk.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdk.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;
import org.omnione.did.sdk.sdjwt.crypto.JWSVerifier;
import org.omnione.did.sdk.sdjwt.crypto.SignedJWT;
import org.omnione.did.sdk.sdjwt.crypto.impl.ECDSAVerifier;
import org.omnione.did.sdk.sdjwt.crypto.impl.RSASSAVerifier;
import org.omnione.did.sdk.sdjwt.util.HashUtils;
import org.omnione.did.sdk.sdjwt.core.validator.SDJWTValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class SDJWTVerifier {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);
  private static final Duration DEFAULT_MAX_KB_JWT_AGE = Duration.ofMinutes(5);

  private final PublicKey issuerPublicKey;
  private final PublicKey holderPublicKey;
  private final SDJWTValidator validator;
  private final boolean extractIssuerKeyFromX5c;
  private final List<X509Certificate> trustedRootCerts;
  private boolean requireKeyBinding;
  private Duration clockSkew;
  private Duration maxKeyBindingJwtAge;
  private boolean requireExp;
  private boolean requireIat;
  private boolean requireNbf;

  /**
   * Creates an SDJWTVerifier with only the issuer's public key. Holder's public key will be
   * extracted from the cnf claim in the credential JWT.
   *
   * @param issuerPublicKey the issuer's public key for verifying the credential JWT signature
   */
  public SDJWTVerifier(PublicKey issuerPublicKey) {
    this(issuerPublicKey, null, false);
  }

  /**
   * Creates an SDJWTVerifier with both issuer's and holder's public keys. When holderPublicKey is
   * provided, it will be used for Key Binding JWT verification instead of extracting from the cnf
   * claim.
   * <p>
   * This is useful when: - The cnf claim contains a key reference (e.g., cnf.kid) instead of the
   * full JWK - The holder's public key is resolved externally (e.g., from DID Document)
   *
   * @param issuerPublicKey the issuer's public key for verifying the credential JWT signature
   * @param holderPublicKey the holder's public key for verifying the Key Binding JWT signature, or
   *                        null to extract from cnf.jwk
   */
  public SDJWTVerifier(PublicKey issuerPublicKey, PublicKey holderPublicKey) {
    this(issuerPublicKey, holderPublicKey, false);
  }

  /**
   * Verifies an SD-JWT by extracting the issuer's public key from the x5c header.
   * Use this method when the SD-JWT contains x5c header instead of kid.
   * Root CA validation is not performed.
   *
   * @param sdJwtString the SD-JWT string to verify
   * @return the verified claims set
   * @throws SDJWTException if verification fails
   */
  public static SDJWTClaimsSet verifyWithX5c(String sdJwtString) throws SDJWTException {
    return new SDJWTVerifier(null, null, true, null).verify(sdJwtString);
  }

  /**
   * Verifies an SD-JWT by extracting the issuer's public key from the x5c header,
   * with Root CA validation against the provided trusted root certificates.
   *
   * @param sdJwtString the SD-JWT string to verify
   * @param trustedRootCerts the list of trusted root CA certificates for chain validation
   * @return the verified claims set
   * @throws SDJWTException if verification fails or chain doesn't terminate at a trusted root
   */
  public static SDJWTClaimsSet verifyWithX5c(String sdJwtString, List<X509Certificate> trustedRootCerts) throws SDJWTException {
    return new SDJWTVerifier(null, null, true, trustedRootCerts).verify(sdJwtString);
  }

  /**
   * Verifies an SD-JWT by extracting the issuer's public key from the x5c header,
   * with expected audience and nonce for Key Binding JWT verification.
   * Root CA validation is not performed.
   *
   * @param sdJwtString the SD-JWT string to verify
   * @param expectedAudience the expected audience claim in KB-JWT
   * @param expectedNonce the expected nonce claim in KB-JWT
   * @return the verified claims set
   * @throws SDJWTException if verification fails
   */
  public static SDJWTClaimsSet verifyWithX5c(String sdJwtString, String expectedAudience, String expectedNonce) throws SDJWTException {
    return new SDJWTVerifier(null, null, true, null).verify(sdJwtString, expectedAudience, expectedNonce);
  }

  /**
   * Verifies an SD-JWT by extracting the issuer's public key from the x5c header,
   * with Root CA validation and expected audience/nonce for Key Binding JWT verification.
   *
   * @param sdJwtString the SD-JWT string to verify
   * @param trustedRootCerts the list of trusted root CA certificates for chain validation
   * @param expectedAudience the expected audience claim in KB-JWT
   * @param expectedNonce the expected nonce claim in KB-JWT
   * @return the verified claims set
   * @throws SDJWTException if verification fails or chain doesn't terminate at a trusted root
   */
  public static SDJWTClaimsSet verifyWithX5c(String sdJwtString, List<X509Certificate> trustedRootCerts,
      String expectedAudience, String expectedNonce) throws SDJWTException {
    return new SDJWTVerifier(null, null, true, trustedRootCerts).verify(sdJwtString, expectedAudience, expectedNonce);
  }

  /**
   * Internal constructor with all configuration options.
   *
   * @param issuerPublicKey the issuer's public key (can be null if extractFromX5c is true)
   * @param holderPublicKey the holder's public key (can be null)
   * @param extractFromX5c whether to extract issuer's public key from x5c header
   */
  private SDJWTVerifier(PublicKey issuerPublicKey, PublicKey holderPublicKey, boolean extractFromX5c) {
    this(issuerPublicKey, holderPublicKey, extractFromX5c, null);
  }

  /**
   * Internal constructor with all configuration options including trusted root certificates.
   *
   * @param issuerPublicKey the issuer's public key (can be null if extractFromX5c is true)
   * @param holderPublicKey the holder's public key (can be null)
   * @param extractFromX5c whether to extract issuer's public key from x5c header
   * @param trustedRootCerts the list of trusted root CA certificates (can be null to skip root validation)
   */
  private SDJWTVerifier(PublicKey issuerPublicKey, PublicKey holderPublicKey, boolean extractFromX5c,
      List<X509Certificate> trustedRootCerts) {
    if (issuerPublicKey == null && !extractFromX5c) {
      throw new IllegalArgumentException("Issuer public key cannot be null unless extractFromX5c is enabled");
    }
    this.issuerPublicKey = issuerPublicKey;
    this.holderPublicKey = holderPublicKey;
    this.extractIssuerKeyFromX5c = extractFromX5c;
    this.trustedRootCerts = trustedRootCerts;
    this.validator = new SDJWTValidator();
    this.requireKeyBinding = false;
    this.clockSkew = DEFAULT_CLOCK_SKEW;
    this.maxKeyBindingJwtAge = DEFAULT_MAX_KB_JWT_AGE;
    this.requireExp = true;
    this.requireIat = false;
    this.requireNbf = false;
  }

  /**
   * Sets the clock skew tolerance for time-based validations. Default is 60 seconds.
   *
   * @param clockSkew the allowed clock skew duration
   * @return this verifier for method chaining
   */
  public SDJWTVerifier clockSkew(Duration clockSkew) {
    if (clockSkew == null || clockSkew.isNegative()) {
      throw new IllegalArgumentException("Clock skew must be a non-negative duration");
    }
    this.clockSkew = clockSkew;
    return this;
  }

  /**
   * Sets the maximum age allowed for Key Binding JWT. The iat claim in KB-JWT must not be older
   * than this duration. Default is 5 minutes.
   *
   * @param maxAge the maximum age duration
   * @return this verifier for method chaining
   */
  public SDJWTVerifier maxKeyBindingJwtAge(Duration maxAge) {
    if (maxAge == null || maxAge.isNegative()) {
      throw new IllegalArgumentException("Max KB-JWT age must be a non-negative duration");
    }
    this.maxKeyBindingJwtAge = maxAge;
    return this;
  }

  /**
   * Sets whether the exp (expiration) claim is required in the credential JWT. Default is true.
   *
   * @param require true to require exp claim
   * @return this verifier for method chaining
   */
  public SDJWTVerifier requireExpiration(boolean require) {
    this.requireExp = require;
    return this;
  }

  /**
   * Sets whether the iat (issued at) claim is required in the credential JWT. Default is false.
   *
   * @param require true to require iat claim
   * @return this verifier for method chaining
   */
  public SDJWTVerifier requireIssuedAt(boolean require) {
    this.requireIat = require;
    return this;
  }

  /**
   * Sets whether the nbf (not before) claim is required in the credential JWT. Default is false.
   *
   * @param require true to require nbf claim
   * @return this verifier for method chaining
   */
  public SDJWTVerifier requireNotBefore(boolean require) {
    this.requireNbf = require;
    return this;
  }

  /**
   * Sets whether Key Binding JWT is required.
   *
   * @param require true to require Key Binding JWT
   * @return this verifier for method chaining
   */
  public SDJWTVerifier requireKeyBinding(boolean require) {
    this.requireKeyBinding = require;
    return this;
  }

  public SDJWTClaimsSet verify(String sdJwtString) throws SDJWTException {
    return verify(sdJwtString, null, null);
  }

  public SDJWTClaimsSet verify(String sdJwtString, String expectedAudience, String expectedNonce)
      throws SDJWTException {
    try {

      SDJWT sdJwt = SDJWT.parse(sdJwtString);

      SDJWTValidator.ValidationResult structureResult = validator.validate(sdJwt);
      if (!structureResult.isValid()) {
        throw new SDJWTException(
            "SD-JWT structure validation failed: " + structureResult.getErrors());
      }

      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());

      // Resolve issuer public key (from constructor or x5c header)
      PublicKey resolvedIssuerKey = resolveIssuerPublicKey(credentialJwt);

      if (!verifyJWTSignature(credentialJwt, resolvedIssuerKey)) {
        throw new SDJWTException("Credential JWT signature verification failed");
      }

      // Verify temporal claims (exp, iat, nbf) in credential JWT
      verifyCredentialTemporalClaims(credentialJwt.getJWTClaimsSet());

      if (sdJwt.hasKeyBindingJwt() || requireKeyBinding) {
        if (!sdJwt.hasKeyBindingJwt() && requireKeyBinding) {
          throw new SDJWTException("Key binding JWT is required but not present");
        }

        if (sdJwt.hasKeyBindingJwt()) {
          verifyKeyBindingJWT(sdJwt, expectedAudience, expectedNonce, sdJwtString);
        }
      }

      verifyDisclosureIntegrity(sdJwt);

      return buildClaimsSet(sdJwt);

    } catch (SDJWTException e) {
      throw e;
    }
  }

  private boolean verifyJWTSignature(SignedJWT jwt, PublicKey publicKey) throws SDJWTException {
    try {
      JWSVerifier verifier;

      if (publicKey instanceof RSAPublicKey) {
        verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
      } else if (publicKey instanceof ECPublicKey) {
        verifier = new ECDSAVerifier((ECPublicKey) publicKey);
      } else {
        throw new IllegalArgumentException("Unsupported public key type: " + publicKey.getClass());
      }

      return jwt.verify(verifier);
    } catch (SDJWTException e) {
      throw new SDJWTException("Failed to verify JWT signature", e);
    }
  }

  /**
   * Resolves the issuer's public key for signature verification.
   * If extractIssuerKeyFromX5c is enabled, extracts from x5c header.
   * Otherwise, uses the pre-configured issuerPublicKey.
   *
   * @param credentialJwt the credential JWT
   * @return the resolved issuer public key
   * @throws SDJWTException if key resolution fails
   */
  private PublicKey resolveIssuerPublicKey(SignedJWT credentialJwt) throws SDJWTException {
    if (!extractIssuerKeyFromX5c) {
      return issuerPublicKey;
    }

    Map<String, Object> header = credentialJwt.getHeader();
    return extractPublicKeyFromX5c(header);
  }

  /**
   * Extracts the public key from the x5c header parameter and validates the certificate chain.
   * Validates according to RFC 7515 Section 4.1.6 and RFC 5280.
   *
   * The first certificate in the x5c array contains the signing key.
   * Each certificate in the chain must be signed by the next certificate.
   * All certificates must be within their validity period.
   *
   * @param header the JWT header map
   * @return the extracted public key from the first certificate
   * @throws SDJWTException if x5c is missing, invalid, or chain validation fails
   */
  @SuppressWarnings("unchecked")
  private PublicKey extractPublicKeyFromX5c(Map<String, Object> header) throws SDJWTException {
    Object x5cObj = header.get("x5c");

    if (x5cObj == null) {
      throw new SDJWTException("x5c header parameter not found");
    }

    if (!(x5cObj instanceof List)) {
      throw new SDJWTException("x5c must be an array");
    }

    List<?> x5cList = (List<?>) x5cObj;
    if (x5cList.isEmpty()) {
      throw new SDJWTException("x5c array is empty");
    }

    // Parse all certificates in the chain
    List<X509Certificate> certChain = new ArrayList<>();
    for (int i = 0; i < x5cList.size(); i++) {
      Object certObj = x5cList.get(i);
      if (!(certObj instanceof String)) {
        throw new SDJWTException("x5c certificate at index " + i + " must be a Base64-encoded string");
      }
      certChain.add(parseX509CertificateFromBase64((String) certObj));
    }

    // Validate the certificate chain
    validateCertificateChain(certChain);

    // Return the public key from the first certificate (signing key)
    return certChain.get(0).getPublicKey();
  }

  /**
   * Parses a Base64-encoded X.509 certificate.
   *
   * @param base64Cert the Base64-encoded DER certificate
   * @return the parsed X509Certificate
   * @throws SDJWTException if certificate parsing fails
   */
  private X509Certificate parseX509CertificateFromBase64(String base64Cert) throws SDJWTException {
    try {
      byte[] certBytes = Base64.getDecoder().decode(base64Cert);
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) certFactory.generateCertificate(
          new ByteArrayInputStream(certBytes)
      );
    } catch (IllegalArgumentException e) {
      throw new SDJWTException("Invalid Base64 encoding in x5c certificate", e);
    } catch (CertificateException e) {
      throw new SDJWTException("Failed to parse X.509 certificate from x5c", e);
    }
  }

  /**
   * Validates the X.509 certificate chain according to RFC 5280.
   *
   * Validation includes:
   * 1. Each certificate's validity period (notBefore, notAfter)
   * 2. Chain linkage: each certificate must be signed by the next certificate in the chain
   * 3. The last certificate must be self-signed (root CA) or chain validation succeeds up to it
   *
   * @param certChain the certificate chain to validate (ordered: end-entity first, root last)
   * @throws SDJWTException if chain validation fails
   */
  private void validateCertificateChain(List<X509Certificate> certChain) throws SDJWTException {
    if (certChain.isEmpty()) {
      throw new SDJWTException("Certificate chain is empty");
    }

    // Validate each certificate's validity period
    for (int i = 0; i < certChain.size(); i++) {
      X509Certificate cert = certChain.get(i);
      try {
        cert.checkValidity();
      } catch (CertificateExpiredException e) {
        throw new SDJWTException("Certificate at index " + i + " has expired: " + cert.getSubjectX500Principal(), e);
      } catch (CertificateNotYetValidException e) {
        throw new SDJWTException("Certificate at index " + i + " is not yet valid: " + cert.getSubjectX500Principal(), e);
      }
    }

    // Validate chain linkage: each certificate must be signed by the next one
    // We verify by checking if the signature is valid - this is the authoritative check
    for (int i = 0; i < certChain.size() - 1; i++) {
      X509Certificate currentCert = certChain.get(i);
      X509Certificate issuerCert = certChain.get(i + 1);

      // Verify the signature - if valid, the chain linkage is correct
      try {
        currentCert.verify(issuerCert.getPublicKey());
      } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException |
               NoSuchProviderException | SignatureException e) {
        throw new SDJWTException(
            "Certificate chain linkage failed at index " + i +
            ": certificate '" + currentCert.getSubjectX500Principal() +
            "' was not signed by '" + issuerCert.getSubjectX500Principal() + "'", e
        );
      }
    }

    // Validate the last certificate (should be self-signed root or trusted anchor)
    X509Certificate lastCert = certChain.get(certChain.size() - 1);
    if (isSelfSigned(lastCert)) {
      // Self-signed certificate - verify its own signature
      try {
        lastCert.verify(lastCert.getPublicKey());
      } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException |
               NoSuchProviderException | SignatureException e) {
        throw new SDJWTException(
            "Self-signed root certificate signature verification failed: " + lastCert.getSubjectX500Principal(), e
        );
      }
    }

    // If trustedRootCerts is provided, verify the chain terminates at a trusted root
    if (trustedRootCerts != null && !trustedRootCerts.isEmpty()) {
      boolean trusted = false;

      for (X509Certificate trustedRoot : trustedRootCerts) {
        // Case 1: The last certificate in chain IS the trusted root
        if (lastCert.equals(trustedRoot)) {
          trusted = true;
          break;
        }

        // Case 2: The last certificate is signed by the trusted root
        // Verify by signature check - this is the authoritative validation
        try {
          lastCert.verify(trustedRoot.getPublicKey());
          trusted = true;
          break;
        } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException |
                 NoSuchProviderException | SignatureException e) {
          // This trusted root didn't sign the certificate, try next one
          continue;
        }
      }

      if (!trusted) {
        throw new SDJWTException(
            "Certificate chain does not terminate at a trusted root CA. " +
            "Last certificate subject: " + lastCert.getSubjectX500Principal() +
            ", issuer: " + lastCert.getIssuerX500Principal()
        );
      }
    }
  }

  /**
   * Checks if a certificate is self-signed (issuer equals subject).
   * Uses canonical form to compare DNs regardless of RDN ordering.
   *
   * @param cert the certificate to check
   * @return true if the certificate is self-signed
   */
  private boolean isSelfSigned(X509Certificate cert) {
    String subjectDN = cert.getSubjectX500Principal()
        .getName(javax.security.auth.x500.X500Principal.CANONICAL);
    String issuerDN = cert.getIssuerX500Principal()
        .getName(javax.security.auth.x500.X500Principal.CANONICAL);
    return subjectDN.equals(issuerDN);
  }

  private void verifyKeyBindingJWT(SDJWT sdJwt, String expectedAudience, String expectedNonce,
      String originalSdJwtString) throws SDJWTException {
    try {
      SignedJWT kbJwt = SignedJWT.parse(sdJwt.getKeyBindingJwt());

      // Step 1: Verify using cnf claim (holder's bound key)
      PublicKey cnfPublicKey = extractHolderPublicKey(sdJwt);
      if (cnfPublicKey == null) {
        throw new SDJWTException("No holder public key available for key binding verification");
      }

      if (!verifyJWTSignature(kbJwt, cnfPublicKey)) {
        throw new SDJWTException("Key binding JWT signature verification failed (cnf verification)");
      }

      // Step 2: If KB-JWT header has x5c, verify the certificate chain and signature
      Map<String, Object> kbHeader = kbJwt.getHeader();
      Object kbX5cObj = kbHeader.get("x5c");
      if (kbX5cObj != null) {
        verifyKeyBindingX5c(kbJwt, kbX5cObj, cnfPublicKey);
      }

      Map<String, Object> kbClaims = kbJwt.getJWTClaimsSet();

      if (expectedAudience != null) {

        Object aud = kbClaims.get("aud");
        boolean audienceMatches = false;

        if (aud instanceof String) {
          audienceMatches = expectedAudience.equals(aud);
        } else if (aud instanceof List) {
          List<?> audList = (List<?>) aud;
          audienceMatches = audList.contains(expectedAudience);
        }

        if (!audienceMatches) {
          throw new SDJWTException(
              "Key binding JWT audience mismatch. Expected: " + expectedAudience + ", Got: " + aud);
        }
      }

      if (expectedNonce != null) {
        Object nonce = kbClaims.get("nonce");
        if (!expectedNonce.equals(nonce)) {
          throw new SDJWTException(
              "Key binding JWT nonce mismatch. Expected: " + expectedNonce + ", Got: " + nonce);
        }
      }

      verifySdHash(kbClaims, originalSdJwtString);

      // Verify iat claim in Key Binding JWT
      verifyKeyBindingJwtTemporalClaims(kbClaims);

    } catch (SDJWTException e) {
      throw e;
    }
  }

  /**
   * Verifies the KB-JWT header's x5c certificate chain and ensures the public key
   * matches the cnf claim's public key.
   *
   * @param kbJwt the Key Binding JWT
   * @param kbX5cObj the x5c array from KB-JWT header
   * @param cnfPublicKey the public key extracted from cnf claim
   * @throws SDJWTException if x5c verification fails or keys don't match
   */
  @SuppressWarnings("unchecked")
  private void verifyKeyBindingX5c(SignedJWT kbJwt, Object kbX5cObj, PublicKey cnfPublicKey)
      throws SDJWTException {

    if (!(kbX5cObj instanceof List)) {
      throw new SDJWTException("KB-JWT x5c must be an array");
    }

    List<?> x5cList = (List<?>) kbX5cObj;
    if (x5cList.isEmpty()) {
      throw new SDJWTException("KB-JWT x5c array is empty");
    }

    // Parse all certificates in the chain
    List<X509Certificate> certChain = new ArrayList<>();
    for (int i = 0; i < x5cList.size(); i++) {
      Object certObj = x5cList.get(i);
      if (!(certObj instanceof String)) {
        throw new SDJWTException("KB-JWT x5c certificate at index " + i + " must be a Base64-encoded string");
      }
      certChain.add(parseX509CertificateFromBase64((String) certObj));
    }

    // Validate the certificate chain
    validateCertificateChain(certChain);

    // Get the public key from the first certificate
    PublicKey x5cPublicKey = certChain.get(0).getPublicKey();

    // Verify KB-JWT signature using x5c public key
    if (!verifyJWTSignature(kbJwt, x5cPublicKey)) {
      throw new SDJWTException("Key binding JWT signature verification failed (x5c verification)");
    }

    // Verify that x5c public key matches cnf public key
    if (!publicKeysMatch(cnfPublicKey, x5cPublicKey)) {
      throw new SDJWTException(
          "KB-JWT x5c public key does not match cnf public key. " +
          "The holder's certificate chain must correspond to the bound key in cnf claim.");
    }
  }

  /**
   * Compares two public keys for equality.
   *
   * @param key1 the first public key
   * @param key2 the second public key
   * @return true if the keys are equivalent
   */
  private boolean publicKeysMatch(PublicKey key1, PublicKey key2) {
    if (key1 == null || key2 == null) {
      return false;
    }
    // Compare encoded forms for reliable equality check
    return java.util.Arrays.equals(key1.getEncoded(), key2.getEncoded());
  }

  private void verifySdHash(Map<String, Object> kbClaims, String originalSdJwtString)
      throws SDJWTException {
    Object sdHashClaim = kbClaims.get("sd_hash");
    if (sdHashClaim == null) {
      throw new SDJWTException("Key binding JWT missing required sd_hash claim");
    }

    if (!(sdHashClaim instanceof String)) {
      throw new SDJWTException("sd_hash claim must be a string");
    }

    String expectedSdHash = (String) sdHashClaim;

    String[] parts = originalSdJwtString.split("~", -1);
    StringBuilder sdJwtForHash = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      sdJwtForHash.append(parts[i]).append("~");
    }

    String calculatedSdHash = calculateSHA256Hash(sdJwtForHash.toString());

    if (!expectedSdHash.equals(calculatedSdHash)) {
      throw new SDJWTException(
          String.format("sd_hash verification failed. Expected: %s, Calculated: %s",
              expectedSdHash, calculatedSdHash));
    }
  }

  private String calculateSHA256Hash(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Verifies temporal claims (exp, iat, nbf) in the credential JWT.
   *
   * @param claims the JWT claims
   * @throws SDJWTException if temporal validation fails
   */
  private void verifyCredentialTemporalClaims(Map<String, Object> claims) throws SDJWTException {
    Instant now = Instant.now();

    // Verify exp (expiration time)
    Object expObj = claims.get("exp");
    if (expObj == null) {
      if (requireExp) {
        throw new SDJWTException("Credential JWT missing required exp claim");
      }
    } else {
      long exp = toLong(expObj);
      Instant expInstant = Instant.ofEpochSecond(exp);
      if (now.isAfter(expInstant.plus(clockSkew))) {
        throw new SDJWTException(
            String.format("Credential JWT has expired. exp: %s, current: %s",
                expInstant, now));
      }
    }

    // Verify iat (issued at)
    Object iatObj = claims.get("iat");
    if (iatObj == null) {
      if (requireIat) {
        throw new SDJWTException("Credential JWT missing required iat claim");
      }
    } else {
      long iat = toLong(iatObj);
      Instant iatInstant = Instant.ofEpochSecond(iat);
      if (iatInstant.isAfter(now.plus(clockSkew))) {
        throw new SDJWTException(
            String.format("Credential JWT iat is in the future. iat: %s, current: %s",
                iatInstant, now));
      }
    }

    // Verify nbf (not before)
    Object nbfObj = claims.get("nbf");
    if (nbfObj == null) {
      if (requireNbf) {
        throw new SDJWTException("Credential JWT missing required nbf claim");
      }
    } else {
      long nbf = toLong(nbfObj);
      Instant nbfInstant = Instant.ofEpochSecond(nbf);
      if (now.isBefore(nbfInstant.minus(clockSkew))) {
        throw new SDJWTException(
            String.format("Credential JWT is not yet valid. nbf: %s, current: %s",
                nbfInstant, now));
      }
    }
  }

  /**
   * Verifies temporal claims in the Key Binding JWT. According to SD-JWT specification, KB-JWT
   * should have iat claim.
   *
   * @param kbClaims the Key Binding JWT claims
   * @throws SDJWTException if temporal validation fails
   */
  private void verifyKeyBindingJwtTemporalClaims(Map<String, Object> kbClaims)
      throws SDJWTException {
    Instant now = Instant.now();

    // Verify iat (issued at) - required for KB-JWT per SD-JWT spec
    Object iatObj = kbClaims.get("iat");
    if (iatObj == null) {
      throw new SDJWTException("Key Binding JWT missing required iat claim");
    }

    long iat = toLong(iatObj);
    Instant iatInstant = Instant.ofEpochSecond(iat);

    // Check if iat is not in the future
    if (iatInstant.isAfter(now.plus(clockSkew))) {
      throw new SDJWTException(
          String.format("Key Binding JWT iat is in the future. iat: %s, current: %s",
              iatInstant, now));
    }

    // Check if KB-JWT is not too old
    if (now.isAfter(iatInstant.plus(maxKeyBindingJwtAge).plus(clockSkew))) {
      throw new SDJWTException(
          String.format("Key Binding JWT is too old. iat: %s, max age: %s, current: %s",
              iatInstant, maxKeyBindingJwtAge, now));
    }

    // Verify exp if present (optional for KB-JWT)
    Object expObj = kbClaims.get("exp");
    if (expObj != null) {
      long exp = toLong(expObj);
      Instant expInstant = Instant.ofEpochSecond(exp);
      if (now.isAfter(expInstant.plus(clockSkew))) {
        throw new SDJWTException(
            String.format("Key Binding JWT has expired. exp: %s, current: %s",
                expInstant, now));
      }
    }
  }

  /**
   * Converts a claim value to long.
   *
   * @param value the claim value (Number or String)
   * @return the long value
   * @throws SDJWTException if conversion fails
   */
  private long toLong(Object value) throws SDJWTException {
    if (value instanceof Number) {
      return ((Number) value).longValue();
    } else if (value instanceof String) {
      try {
        return Long.parseLong((String) value);
      } catch (NumberFormatException e) {
        throw new SDJWTException("Invalid temporal claim value: " + value);
      }
    }
    throw new SDJWTException("Temporal claim must be a number: " + value);
  }

  /**
   * Extracts the holder's public key for Key Binding JWT verification.
   * <p>
   * Priority: 1. If holderPublicKey was provided in constructor, use it
   * 2a. Otherwise, extract from cnf.jwk in the credential JWT
   * 2b. Otherwise, extract from cnf.x5c in the credential JWT
   *
   * @param sdJwt the parsed SD-JWT
   * @return the holder's public key
   * @throws SDJWTException if no holder public key is available
   */
  @SuppressWarnings("unchecked")
  private PublicKey extractHolderPublicKey(SDJWT sdJwt) throws SDJWTException {
    // Priority 1: Use externally provided holder public key
    if (this.holderPublicKey != null) {
      return this.holderPublicKey;
    }

    // Priority 2: Extract from cnf.jwk in credential JWT
    try {
      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());
      Map<String, Object> payload = credentialJwt.getJWTClaimsSet();

      Object cnfObj = payload.get("cnf");
      if (cnfObj == null) {
        throw new SDJWTException(
            "cnf claim not found in credential JWT and no external holder public key provided");
      }

      if (!(cnfObj instanceof Map)) {
        throw new SDJWTException("cnf claim must be an object");
      }

      Map<String, Object> cnf = (Map<String, Object>) cnfObj;

      // Priority 2a: Extract from cnf.jwk
      Object jwkObj = cnf.get("jwk");
      if (jwkObj != null) {
        if (!(jwkObj instanceof Map)) {
          throw new SDJWTException("jwk must be an object");
        }
        Map<String, Object> jwk = (Map<String, Object>) jwkObj;
        PublicKey jwkPublicKey = parseJwkToPublicKey(jwk);

        // If jwk contains x5c (RFC 7517), validate certificate chain and ensure keys match
        Object x5cInJwk = jwk.get("x5c");
        if (x5cInJwk != null) {
          verifyJwkX5c(jwk, jwkPublicKey);
        }

        return jwkPublicKey;
      }

      // Priority 2b: Extract from cnf.x5c (legacy format)
      Object x5cObj = cnf.get("x5c");
      if (x5cObj != null) {
        return extractPublicKeyFromCnfX5c(cnf);
      }

      // cnf exists but no jwk or x5c - might have kid reference
      Object kidObj = cnf.get("kid");
      if (kidObj != null) {
        throw new SDJWTException(
            "cnf.kid reference found but no external holder public key provided. " +
                "Please provide holder public key via constructor.");
      }
      throw new SDJWTException("Neither jwk nor x5c found in cnf claim");

    } catch (SDJWTException e) {
      throw e;
    }
  }

  /**
   * Verifies the x5c array within a JWK according to RFC 7517.
   * Validates the certificate chain and ensures the JWK public key matches
   * the public key in the first certificate (x5c[0]).
   *
   * @param jwk the JWK map containing x5c
   * @param jwkPublicKey the public key extracted from JWK parameters
   * @throws SDJWTException if x5c validation fails or keys don't match
   */
  @SuppressWarnings("unchecked")
  private void verifyJwkX5c(Map<String, Object> jwk, PublicKey jwkPublicKey) throws SDJWTException {
    Object x5cObj = jwk.get("x5c");

    if (!(x5cObj instanceof List)) {
      throw new SDJWTException("jwk.x5c must be an array");
    }

    List<?> x5cList = (List<?>) x5cObj;
    if (x5cList.isEmpty()) {
      throw new SDJWTException("jwk.x5c array is empty");
    }

    // Parse all certificates in the chain
    List<X509Certificate> certChain = new ArrayList<>();
    for (int i = 0; i < x5cList.size(); i++) {
      Object certObj = x5cList.get(i);
      if (!(certObj instanceof String)) {
        throw new SDJWTException("jwk.x5c certificate at index " + i + " must be a Base64-encoded string");
      }
      certChain.add(parseX509CertificateFromBase64((String) certObj));
    }

    // Validate the certificate chain
    validateCertificateChain(certChain);

    // Extract public key from the first certificate
    PublicKey x5cPublicKey = certChain.get(0).getPublicKey();

    // Verify that JWK public key matches x5c[0] public key (RFC 7517 requirement)
    if (!publicKeysMatch(jwkPublicKey, x5cPublicKey)) {
      throw new SDJWTException(
          "JWK public key does not match x5c[0] certificate public key. " +
          "Per RFC 7517, the key in x5c[0] MUST match the public key represented by the JWK.");
    }
  }

  /**
   * Extracts the public key from the cnf.x5c parameter and validates the certificate chain.
   * The first certificate in the x5c array contains the holder's public key.
   * Note: This is a legacy format. Prefer cnf.jwk with embedded x5c per RFC 7517.
   *
   * @param cnf the cnf claim map containing x5c
   * @return the holder's PublicKey extracted from the first certificate
   * @throws SDJWTException if x5c is missing, invalid, or chain validation fails
   */
  @SuppressWarnings("unchecked")
  private PublicKey extractPublicKeyFromCnfX5c(Map<String, Object> cnf) throws SDJWTException {
    Object x5cObj = cnf.get("x5c");

    if (!(x5cObj instanceof List)) {
      throw new SDJWTException("cnf.x5c must be an array");
    }

    List<?> x5cList = (List<?>) x5cObj;
    if (x5cList.isEmpty()) {
      throw new SDJWTException("cnf.x5c array is empty");
    }

    // Parse all certificates in the chain
    List<X509Certificate> certChain = new ArrayList<>();
    for (int i = 0; i < x5cList.size(); i++) {
      Object certObj = x5cList.get(i);
      if (!(certObj instanceof String)) {
        throw new SDJWTException("cnf.x5c certificate at index " + i + " must be a Base64-encoded string");
      }
      certChain.add(parseX509CertificateFromBase64((String) certObj));
    }

    // Validate the certificate chain
    validateCertificateChain(certChain);

    // Return the public key from the first certificate (holder's key)
    return certChain.get(0).getPublicKey();
  }

  /**
   * Parses a JWK (JSON Web Key) map to a PublicKey.
   *
   * @param jwk the JWK as a Map
   * @return the parsed PublicKey
   * @throws SDJWTException if the key type is unsupported or parsing fails
   */
  private PublicKey parseJwkToPublicKey(Map<String, Object> jwk) throws SDJWTException {
    String kty = (String) jwk.get("kty");

    if (kty == null) {
      throw new SDJWTException("kty (key type) is required in JWK");
    }

    if ("EC".equals(kty)) {
      return createECPublicKeyFromJwk(jwk);
    } else if ("RSA".equals(kty)) {
      return createRSAPublicKeyFromJwk(jwk);
    }

    throw new SDJWTException("Unsupported key type: " + kty);
  }

  /**
   * Creates an ECPublicKey from a JWK map.
   *
   * @param jwk the JWK containing EC key parameters
   * @return the ECPublicKey
   * @throws SDJWTException if key creation fails
   */
  private ECPublicKey createECPublicKeyFromJwk(Map<String, Object> jwk) throws SDJWTException {
    try {
      String crv = (String) jwk.get("crv");
      String xBase64 = (String) jwk.get("x");
      String yBase64 = (String) jwk.get("y");

      if (crv == null || xBase64 == null || yBase64 == null) {
        throw new SDJWTException("EC JWK must contain crv, x, and y parameters");
      }

      String curveName = getCurveNameForSpec(crv);

      byte[] xBytes = Base64.getUrlDecoder().decode(xBase64);
      byte[] yBytes = Base64.getUrlDecoder().decode(yBase64);

      BigInteger x = new BigInteger(1, xBytes);
      BigInteger y = new BigInteger(1, yBytes);

      ECPoint ecPoint = new ECPoint(x, y);

      AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
      parameters.init(new ECGenParameterSpec(curveName));
      ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);

      ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
      KeyFactory keyFactory = KeyFactory.getInstance("EC");

      return (ECPublicKey) keyFactory.generatePublic(publicKeySpec);

    } catch (SDJWTException e) {
      throw e;
    } catch (Exception e) {
      throw new SDJWTException("Failed to create EC public key from JWK", e);
    }
  }

  /**
   * Maps JWK curve names to Java curve specification names.
   *
   * @param crv the JWK curve name (e.g., "P-256")
   * @return the Java curve specification name (e.g., "secp256r1")
   * @throws SDJWTException if the curve is unsupported
   */
  private String getCurveNameForSpec(String crv) throws SDJWTException {
    switch (crv) {
      case "P-256": return "secp256r1";
      case "P-384": return "secp384r1";
      case "P-521": return "secp521r1";
      case "secp256k1": return "secp256k1";
      default: throw new SDJWTException("Unsupported EC curve: " + crv);
    }
  }

  /**
   * Creates an RSAPublicKey from a JWK map.
   *
   * @param jwk the JWK containing RSA key parameters
   * @return the RSAPublicKey
   * @throws SDJWTException if key creation fails
   */
  private RSAPublicKey createRSAPublicKeyFromJwk(Map<String, Object> jwk) throws SDJWTException {
    try {
      String nBase64 = (String) jwk.get("n");
      String eBase64 = (String) jwk.get("e");

      if (nBase64 == null || eBase64 == null) {
        throw new SDJWTException("RSA JWK must contain n and e parameters");
      }

      byte[] nBytes = Base64.getUrlDecoder().decode(nBase64);
      byte[] eBytes = Base64.getUrlDecoder().decode(eBase64);

      BigInteger modulus = new BigInteger(1, nBytes);
      BigInteger exponent = new BigInteger(1, eBytes);

      java.security.spec.RSAPublicKeySpec publicKeySpec =
          new java.security.spec.RSAPublicKeySpec(modulus, exponent);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

    } catch (SDJWTException e) {
      throw e;
    } catch (Exception e) {
      throw new SDJWTException("Failed to create RSA public key from JWK", e);
    }
  }

  private void verifyDisclosureIntegrity(SDJWT sdJwt) throws SDJWTException {
    try {
      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());
      Map<String, Object> payload = credentialJwt.getJWTClaimsSet();

      List<String> sdArray = extractSDArray(payload);
      String hashAlgorithm = extractHashAlgorithm(payload);

      for (Disclosure disclosure : sdJwt.getDisclosures()) {
        String computedDigest = disclosure.digest(hashAlgorithm);

        if (!sdArray.contains(computedDigest)) {
          throw new SDJWTException("Disclosure digest not found in _sd array: " + computedDigest);
        }
      }

    } catch (SDJWTException e) {
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> extractSDArray(Map<String, Object> payload) {
    Object sdObj = payload.get("_sd");
    if (sdObj instanceof List) {
      return (List<String>) sdObj;
    }
    return new ArrayList<>();
  }

  private String extractHashAlgorithm(Map<String, Object> payload) {
    Object alg = payload.get("_sd_alg");
    return alg != null ? alg.toString() : HashUtils.getDefaultHashAlgorithm();
  }

  private SDJWTClaimsSet buildClaimsSet(SDJWT sdJwt) throws SDJWTException {
    try {
      SignedJWT credentialJwt = SignedJWT.parse(sdJwt.getCredentialJwt());
      Map<String, Object> baseClaims = new LinkedHashMap<>(credentialJwt.getJWTClaimsSet());

      baseClaims.remove("_sd");
      baseClaims.remove("_sd_alg");

      for (Disclosure disclosure : sdJwt.getDisclosures()) {
        if (!disclosure.isArrayElement()) {
          baseClaims.put(disclosure.getClaimName(), disclosure.getClaimValue());
        }
      }

      return new SDJWTClaimsSet(baseClaims, sdJwt.getDisclosures());

    } catch (SDJWTException e) {
      throw e;
    }
  }

  public static class SDJWTClaimsSet {

    private final Map<String, Object> claims;
    private final List<Disclosure> disclosures;

    public SDJWTClaimsSet(Map<String, Object> claims, List<Disclosure> disclosures) {
      this.claims = Collections.unmodifiableMap(new LinkedHashMap<>(claims));
      this.disclosures = Collections.unmodifiableList(new ArrayList<>(disclosures));
    }

    public Map<String, Object> getClaims() {
      return claims;
    }

    public List<Disclosure> getDisclosures() {
      return disclosures;
    }

    public Object getClaim(String name) {
      return claims.get(name);
    }

    public String getStringClaim(String name) {
      Object value = getClaim(name);
      return value != null ? value.toString() : null;
    }

    public Long getLongClaim(String name) {
      Object value = getClaim(name);
      if (value instanceof Number) {
        return ((Number) value).longValue();
      }
      return null;
    }

    public Boolean getBooleanClaim(String name) {
      Object value = getClaim(name);
      if (value instanceof Boolean) {
        return (Boolean) value;
      }
      return null;
    }

    public Set<String> getClaimNames() {
      return claims.keySet();
    }

    public boolean hasClaim(String name) {
      return claims.containsKey(name);
    }

    @Override
    public String toString() {
      return "SDJWTClaimsSet{" +
          "claims=" + claims +
          ", disclosureCount=" + disclosures.size() +
          '}';
    }
  }
}