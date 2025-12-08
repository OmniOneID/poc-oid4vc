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

package org.omnione.did.oid4vc.oid4vci.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.omnione.did.oid4vc.exception.OID4VCException;
import org.omnione.did.sdjwt.core.builder.SDJWTBuilder;
import org.omnione.did.sdjwt.crypto.JWSSigner;
import org.omnione.did.sdjwt.crypto.SignedJWT;
import org.omnione.did.sdjwt.crypto.impl.ECDSASigner;
import org.omnione.did.sdjwt.crypto.impl.RSASSASigner;
import org.omnione.did.sdjwt.datamodel.DisclosureFrame;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.exception.SDJWTException;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.WalletManagerInterface;

public class OID4VCIssuer {

  private final PrivateKey issuerPrivateKey;
  private final WalletManagerInterface walletManager;
  private final String keyId;
  private final String issuerId;

  public OID4VCIssuer(PrivateKey issuerPrivateKey, String issuerId) {
    if (issuerPrivateKey == null) {
      throw new IllegalArgumentException("Issuer private key cannot be null");
    }
    if (issuerId == null || issuerId.trim().isEmpty()) {
      throw new IllegalArgumentException("Issuer ID cannot be null or empty");
    }

    this.issuerPrivateKey = issuerPrivateKey;
    this.walletManager = null;
    this.keyId = null;
    this.issuerId = issuerId;
  }

  public OID4VCIssuer(WalletManagerInterface walletManager, String keyId, String issuerId) {
    if (walletManager == null) {
      throw new IllegalArgumentException("Wallet manager cannot be null");
    }
    if (keyId == null || keyId.trim().isEmpty()) {
      throw new IllegalArgumentException("Key ID cannot be null or empty");
    }
    if (issuerId == null || issuerId.trim().isEmpty()) {
      throw new IllegalArgumentException("Issuer ID cannot be null or empty");
    }

    this.issuerPrivateKey = null;
    this.walletManager = walletManager;
    this.keyId = keyId;
    this.issuerId = issuerId;
  }

  public String issueCredential(String credentialType,
      Map<String, Object> subjectClaims,
      PublicKey holderPublicKey) throws OID4VCException {

    SDJWTBuilder builder = new SDJWTBuilder()
        .issuer(issuerId)
        .issuedAtNow()
        .expiresIn(365, ChronoUnit.DAYS)
        .verifiableCredentialType(credentialType);

    if (holderPublicKey != null) {
      Map<String, Object> cnf = createConfirmationClaim(holderPublicKey);
      builder.confirmation(cnf);
    }

    subjectClaims.forEach(builder::selectivelyDisclosableClaim);

    SDJWT sdJwt = builder.build(this::signJWT);

    return sdJwt.toString();
  }

  public String issueCredential(String credentialType,
      Map<String, Object> subjectClaims,
      DisclosureFrame disclosureFrame,
      PublicKey holderPublicKey) throws OID4VCException {

    try {
      disclosureFrame.validate(subjectClaims);
    } catch (SDJWTException e) {
      throw new OID4VCException("Invalid disclosure frame for given claims", e);
    }

    SDJWTBuilder builder = new SDJWTBuilder()
        .issuer(issuerId)
        .issuedAtNow()
        .expiresIn(365, ChronoUnit.DAYS)
        .verifiableCredentialType(credentialType);

    if (holderPublicKey != null) {
      Map<String, Object> cnf = createConfirmationClaim(holderPublicKey);
      builder.confirmation(cnf);
    }

    builder.buildWithStructuredFrame(subjectClaims, disclosureFrame);

    SDJWT sdJwt = builder.build(this::signJWT);

    return sdJwt.toString();
  }

  public String issueCredential(String credentialType,
      Map<String, Object> subjectClaims,
      Map<String, Object> disclosureFrameMap,
      PublicKey holderPublicKey) throws OID4VCException {

    try {
      DisclosureFrame disclosureFrame = DisclosureFrame.fromMap(disclosureFrameMap);
      return issueCredential(credentialType, subjectClaims, disclosureFrame, holderPublicKey);
    } catch (SDJWTException e) {
      throw new OID4VCException("Failed to create disclosure frame from map", e);
    }
  }

  private Map<String, Object> createConfirmationClaim(PublicKey holderPublicKey) {
    Map<String, Object> jwkMap = new HashMap<>();
    jwkMap.put("kty", getKeyType(holderPublicKey));
    jwkMap.put("use", "sig");

    if (holderPublicKey.getAlgorithm().equals("EC")) {
      try {
        ECPublicKey ecPublicKey = (ECPublicKey) holderPublicKey;

        String crv = getCurveNameFromECKey(ecPublicKey);
        jwkMap.put("crv", crv);

        BigInteger x = ecPublicKey.getW().getAffineX();
        BigInteger y = ecPublicKey.getW().getAffineY();

        String xBase64 = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(x.toByteArray());
        String yBase64 = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(y.toByteArray());

        jwkMap.put("x", xBase64);
        jwkMap.put("y", yBase64);
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to extract EC key parameters", e);
      }
    } else if (holderPublicKey.getAlgorithm().equals("RSA")) {
      // TODO
    }

    return Map.of("jwk", jwkMap);
  }

  private String getKeyType(PublicKey publicKey) {
    if (publicKey.getAlgorithm().equals("RSA")) {
      return "RSA";
    } else if (publicKey.getAlgorithm().equals("EC")) {
      return "EC";
    } else {
      throw new IllegalArgumentException("Unsupported key type: " + publicKey.getAlgorithm());
    }
  }

  private String getCurveNameFromECKey(java.security.interfaces.ECPublicKey ecPublicKey) {
    java.security.spec.ECParameterSpec ecParams = ecPublicKey.getParams();
    int keySize = ecParams.getOrder().bitLength();

    switch (keySize) {
      case 256:
        return "P-256";
      case 384:
        return "P-384";
      case 521:
        return "P-521";
      default:
        throw new IllegalArgumentException("Unsupported EC key size: " + keySize);
    }
  }

  private String signJWT(String payloadJson) throws OID4VCException {
    try {
      if (walletManager != null) {

        return signJWTWithWalletManager(payloadJson);
      } else {

        return signJWTWithPrivateKey(payloadJson);
      }
    } catch (OID4VCException e) {
      throw e;
    } catch (SDJWTException e) {
      throw new OID4VCException("Failed to sign JWT", e);
    }
  }

  private String signJWTWithWalletManager(String payloadJson) throws OID4VCException {
    try {

      String keyAlgorithm = walletManager.getKeyAlgorithm(keyId);
      String algorithm;
      JWSSigner signer;

      if (keyAlgorithm.contains("RSA")) {
        algorithm = "RS256";

        throw new IllegalArgumentException("RSA with WalletManager not yet implemented");
      } else if (keyAlgorithm.contains("Secp256r1") || keyAlgorithm.contains("SECP256r1") ||
          keyAlgorithm.contains("Secp256k1") || keyAlgorithm.contains("SECP256k1") ||
          keyAlgorithm.contains("EC")) {
        algorithm =
            keyAlgorithm.contains("Secp256k1") || keyAlgorithm.contains("SECP256k1") ? "ES256K"
                : "ES256";
        signer = new ECDSASigner(walletManager, keyId);
      } else {
        throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlgorithm);
      }

      Map<String, Object> header = new HashMap<>();
      header.put("alg", algorithm);
      header.put("typ", "vc+sd-jwt");
      header.put("kid", issuerId);

      Map<String, Object> payloadMap = new ObjectMapper().readValue(payloadJson,
          new TypeReference<Map<String, Object>>() {
          });

      SignedJWT signedJWT = new SignedJWT(header, payloadMap);
      signedJWT.sign(signer);

      return signedJWT.serialize();

    } catch (OID4VCException e) {
      throw e;
    } catch (SDJWTException e) {
      throw new OID4VCException("Failed to sign with wallet manager", e);
    } catch (WalletException e) {
      throw new OID4VCException("Failed to sign with wallet manager", e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private String signJWTWithPrivateKey(String payloadJson) throws OID4VCException {
    try {

      String algorithm;
      JWSSigner signer;

      if (issuerPrivateKey instanceof RSAPrivateKey) {
        algorithm = "RS256";
        signer = new RSASSASigner((RSAPrivateKey) issuerPrivateKey);
      } else if (issuerPrivateKey instanceof ECPrivateKey) {
        algorithm = "ES256";
        signer = new ECDSASigner((ECPrivateKey) issuerPrivateKey);
      } else {
        throw new IllegalArgumentException("Unsupported private key type");
      }

      Map<String, Object> header = new HashMap<>();
      header.put("alg", algorithm);
      header.put("typ", "vc+sd-jwt");
      header.put("kid", issuerId);

      Map<String, Object> payloadMap = new ObjectMapper().readValue(payloadJson,
          new TypeReference<Map<String, Object>>() {
          });

      SignedJWT signedJWT = new SignedJWT(header, payloadMap);
      signedJWT.sign(signer);

      return signedJWT.serialize();
    } catch (OID4VCException e) {
      throw e;
    } catch (SDJWTException e) {
      throw new OID4VCException("Failed to sign JWT with private key", e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}