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

package org.omnione.did.oid4vc.oid4vp.util.jar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.CompactSigner;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.JWSSigner;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.SignedJWT;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.impl.ECDSASigner;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.impl.RSASSASigner;

public class RequestObjectBuilder {

  private static final String HEADER_TYPE = "oauth-authz-req+jwt";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final PrivateKey verifierPrivateKey;
  private final String keyId;
  private final String keyAlgorithm;
  private final String verifierId;
  private final CompactSigner compactSigner;

  public RequestObjectBuilder(PrivateKey verifierPrivateKey, String verifierId) {
    validateNotNull(verifierPrivateKey, "Verifier private key");
    validateNotEmpty(verifierId, "Verifier ID");

    this.verifierPrivateKey = verifierPrivateKey;
    this.compactSigner = null;
    this.keyId = null;
    this.keyAlgorithm = null;
    this.verifierId = verifierId;
  }

  public RequestObjectBuilder(CompactSigner compactSigner, String keyId, String keyAlgorithm, String verifierId) {
    validateNotNull(compactSigner, "CompactSigner");
    validateNotEmpty(keyId, "Key ID");
    validateNotEmpty(keyAlgorithm, "Key Algorithm");
    validateNotEmpty(verifierId, "Verifier ID");

    this.verifierPrivateKey = null;
    this.compactSigner = compactSigner;
    this.keyId = keyId;
    this.keyAlgorithm = keyAlgorithm;
    this.verifierId = verifierId;
  }

  private String build(String payloadJson) throws OID4VPException {
    try {
      SignerInfo signerInfo = createSignerInfo();
      return signAndSerialize(payloadJson, signerInfo);
    } catch (OID4VPException e) {
      throw e;
    } catch (JsonProcessingException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_INVALID_PARAMETER, "Failed to parse payload JSON: " + e.getMessage(), e);
    }
  }

  private SignerInfo createSignerInfo() {
    if (compactSigner != null) {
      return createSignerInfoFromCompactSigner();
    }
    return createSignerInfoFromPrivateKey();
  }

  private SignerInfo createSignerInfoFromCompactSigner() {
    if (keyAlgorithm.contains("RSA")) {
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_UNSUPPORTED_ALGORITHM,
          "RSA with WalletManager not yet implemented"));
    }

    if (isECAlgorithm(keyAlgorithm)) {
      String algorithm = isSecp256k1(keyAlgorithm) ? "ES256K" : "ES256";
      return new SignerInfo(algorithm, new ECDSASigner(compactSigner, keyId));
    }

    throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_UNSUPPORTED_ALGORITHM,
        "Unsupported key algorithm: " + keyAlgorithm));
  }

  private SignerInfo createSignerInfoFromPrivateKey() {
    if (verifierPrivateKey instanceof RSAPrivateKey rsaKey) {
      return new SignerInfo("RS256", new RSASSASigner(rsaKey));
    }

    if (verifierPrivateKey instanceof ECPrivateKey ecKey) {
      return new SignerInfo("ES256", new ECDSASigner(ecKey));
    }

    throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_UNSUPPORTED_ALGORITHM,
        "Unsupported private key type"));
  }

  private String signAndSerialize(String payloadJson, SignerInfo signerInfo)
      throws JsonProcessingException, OID4VPException {

    Map<String, Object> header = Map.of(
        "alg", signerInfo.algorithm(),
        "typ", HEADER_TYPE,
        "kid", verifierId
    );

    Map<String, Object> payloadMap = OBJECT_MAPPER.readValue(
        payloadJson,
        new TypeReference<>() {}
    );

    SignedJWT signedJWT = new SignedJWT(header, payloadMap);
    signedJWT.sign(signerInfo.signer());

    return signedJWT.serialize();
  }

  private boolean isECAlgorithm(String algorithm) {
    return algorithm.contains("Secp256r1") || algorithm.contains("SECP256r1") ||
        algorithm.contains("Secp256k1") || algorithm.contains("SECP256k1") ||
        algorithm.contains("EC");
  }

  private boolean isSecp256k1(String algorithm) {
    return algorithm.contains("Secp256k1") || algorithm.contains("SECP256k1");
  }

  private static void validateNotNull(Object value, String fieldName) {
    if (value == null) {
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_INVALID_PARAMETER,
          fieldName + " cannot be null"));
    }
  }

  private static void validateNotEmpty(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_INVALID_PARAMETER,
          fieldName + " cannot be null or empty"));
    }
  }

  private record SignerInfo(String algorithm, JWSSigner signer) {}
}