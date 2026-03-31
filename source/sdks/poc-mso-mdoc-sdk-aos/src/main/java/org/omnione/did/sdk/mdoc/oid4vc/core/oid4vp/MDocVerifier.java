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

package org.omnione.did.sdk.mdoc.oid4vc.core.oid4vp;

import android.util.Log;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.sdk.mdoc.oid4vc.constant.MdocConstants;
import org.omnione.did.sdk.mdoc.oid4vc.exception.MdocErrorCode;
import org.omnione.did.sdk.mdoc.oid4vc.exception.MdocException;
import org.omnione.did.sdk.mdoc.oid4vc.util.CoseSignatureUtils;

/**
 * Verifies mDoc credentials following ISO/IEC 18013-5 and OID4VP specifications.
 *
 * <p>Performs the following verification steps:</p>
 * <ol>
 *   <li>Extract X.509 certificate chain from IssuerAuth COSE_Sign1 (label 33)</li>
 *   <li>Optionally validate certificate chain against trusted root certificates (PKIX)</li>
 *   <li>Verify IssuerAuth COSE_Sign1 signature using the issuer's public key</li>
 *   <li>Verify IssuerSignedItem digests against MSO valueDigests</li>
 *   <li>Optionally verify DeviceAuth signature using the device key from MSO</li>
 * </ol>
 *
 * <p>Returns a {@link MDocClaimsSet} containing the verified claims organized by namespace.</p>
 *
 * @see OID4VPHandler
 * @see MDocParser
 */
public class MDocVerifier {

  private static final String TAG = MDocVerifier.class.getSimpleName();

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private final List<X509Certificate> trustedRootCerts;
  private boolean requireDeviceAuth;
  private boolean requireValidityInfo;

  /**
   * Creates an MDocVerifier without certificate chain validation.
   */
  public MDocVerifier() {
    this(null);
  }

  /**
   * Creates an MDocVerifier with optional trusted root certificate validation.
   *
   * @param trustedRootCerts list of trusted root X.509 certificates for chain validation;
   *                         null to skip certificate chain validation
   */
  public MDocVerifier(List<X509Certificate> trustedRootCerts) {
    this.trustedRootCerts = trustedRootCerts;
    this.requireDeviceAuth = false;
    this.requireValidityInfo = false;
  }

  /**
   * Verifies an mDoc credential using the public key from the x5chain. No chain validation.
   *
   * @param mdocBase64 Base64URL-encoded mDoc DeviceResponse
   * @return verified claims set
   * @throws MdocException if verification fails
   */
  public static MDocClaimsSet verifyWithX5c(String mdocBase64) throws MdocException {
    return new MDocVerifier(null).verify(mdocBase64);
  }

  /**
   * Verifies an mDoc credential with trusted root certificate chain validation.
   *
   * @param mdocBase64       Base64URL-encoded mDoc DeviceResponse
   * @param trustedRootCerts list of trusted root X.509 certificates
   * @return verified claims set
   * @throws MdocException if verification or chain validation fails
   */
  public static MDocClaimsSet verifyWithX5c(String mdocBase64,
      List<X509Certificate> trustedRootCerts) throws MdocException {
    return new MDocVerifier(trustedRootCerts).verify(mdocBase64);
  }

  /**
   * Verifies an mDoc credential with DeviceAuth verification. No chain validation.
   *
   * @param mdocBase64  Base64URL-encoded mDoc DeviceResponse
   * @param clientId    OID4VP client_id for session transcript
   * @param nonce       OID4VP nonce for session transcript
   * @param responseUri OID4VP response_uri for session transcript
   * @return verified claims set
   * @throws MdocException if verification or DeviceAuth fails
   */
  public static MDocClaimsSet verifyWithX5c(String mdocBase64, String clientId,
      String nonce, String responseUri) throws MdocException {
    return new MDocVerifier(null).verify(mdocBase64, clientId, nonce, responseUri);
  }

  /**
   * Verifies an mDoc credential with trusted root validation and DeviceAuth verification.
   *
   * @param mdocBase64       Base64URL-encoded mDoc DeviceResponse
   * @param trustedRootCerts list of trusted root X.509 certificates
   * @param clientId         OID4VP client_id for session transcript
   * @param nonce            OID4VP nonce for session transcript
   * @param responseUri      OID4VP response_uri for session transcript
   * @return verified claims set
   * @throws MdocException if any verification step fails
   */
  public static MDocClaimsSet verifyWithX5c(String mdocBase64,
      List<X509Certificate> trustedRootCerts, String clientId, String nonce,
      String responseUri) throws MdocException {
    return new MDocVerifier(trustedRootCerts).verify(mdocBase64, clientId, nonce, responseUri);
  }

  /**
   * Sets whether DeviceAuth verification is required.
   *
   * @param require true to require DeviceAuth; false to make it optional
   * @return this verifier instance for fluent configuration
   */
  public MDocVerifier requireDeviceAuth(boolean require) {
    this.requireDeviceAuth = require;
    return this;
  }

  /**
   * Sets whether MSO validityInfo check is required.
   *
   * @param require true to require validityInfo validation; false to skip
   * @return this verifier instance for fluent configuration
   */
  public MDocVerifier requireValidityInfo(boolean require) {
    this.requireValidityInfo = require;
    return this;
  }

  /**
   * Verifies an mDoc credential without DeviceAuth.
   *
   * @param mdocBase64 Base64URL-encoded mDoc DeviceResponse
   * @return verified claims set
   * @throws MdocException if verification fails
   */
  public MDocClaimsSet verify(String mdocBase64) throws MdocException {
    return verify(mdocBase64, null, null, null);
  }

  /**
   * Verifies an mDoc credential with optional DeviceAuth.
   *
   * <p>If clientId, nonce, and responseUri are all non-null, DeviceAuth verification is performed.
   * If any are null and requireDeviceAuth is false, DeviceAuth verification is skipped.</p>
   *
   * @param mdocBase64  Base64URL-encoded mDoc DeviceResponse
   * @param clientId    OID4VP client_id; null to skip DeviceAuth
   * @param nonce       OID4VP nonce; null to skip DeviceAuth
   * @param responseUri OID4VP response_uri; null to skip DeviceAuth
   * @return verified claims set with namespace-organized claims
   * @throws MdocException if any verification step fails
   */
  public MDocClaimsSet verify(String mdocBase64, String clientId, String nonce,
      String responseUri) throws MdocException {
    try {
      if (mdocBase64 == null || mdocBase64.trim().isEmpty()) {
        throw new MdocException(MdocErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER.getMsg()
            + ": mdocBase64");
      }

      CBORObject document = MDocParser.extractFirstDocument(mdocBase64);
      CBORObject issuerSigned = document.get(MdocConstants.Document.ISSUER_SIGNED);
      if (issuerSigned == null) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
            + ": No issuerSigned found");
      }

      CBORObject issuerAuth = issuerSigned.get(MdocConstants.IssuerSigned.ISSUER_AUTH);
      if (issuerAuth == null || issuerAuth.getType() != CBORType.Array) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
            + ": No issuerAuth (COSE_Sign1) found");
      }

      List<byte[]> x5chain = MDocParser.extractX5Chain(issuerAuth);
      if (x5chain == null || x5chain.isEmpty()) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_NO_X5CHAIN.getMsg());
      }

      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      List<X509Certificate> certChain = new ArrayList<>();
      for (byte[] certBytes : x5chain) {
        certChain.add((X509Certificate) certFactory.generateCertificate(
            new ByteArrayInputStream(certBytes)));
      }

      if (trustedRootCerts != null && !trustedRootCerts.isEmpty()) {
        validateCertificateChain(certChain, trustedRootCerts);
      }

      verifyCoseSign1Signature(issuerAuth, certChain.get(0).getPublicKey());
      verifyIssuerSignedItemDigests(document, issuerAuth);

      boolean hasDeviceAuthParams = clientId != null && nonce != null && responseUri != null;
      if (hasDeviceAuthParams) {
        verifyDeviceAuth(mdocBase64, document, clientId, nonce, responseUri);
      } else if (requireDeviceAuth) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
            + ": DeviceAuth required but clientId/nonce/responseUri not provided");
      }

      return buildClaimsSet(document);

    } catch (GeneralSecurityException e) {
      throw new MdocException("mdoc verification failed: " + e.getMessage(), e);
    }
  }

  private void verifyCoseSign1Signature(CBORObject coseSign1, PublicKey publicKey) throws MdocException {
    try {
    byte[] protectedHeaderBytes = coseSign1.get(0).GetByteString();
    byte[] payload = coseSign1.get(2).isNull() ? new byte[0] : coseSign1.get(2).GetByteString();
    byte[] signatureBytes = coseSign1.get(3).GetByteString();

    CBORObject protectedHeader = CBORObject.DecodeFromBytes(protectedHeaderBytes);
    CBORObject algObj = protectedHeader.get(CBORObject.FromObject(1));
    int algId = algObj != null ? algObj.AsInt32Value() : -7;

    String jcaAlgorithm = CoseSignatureUtils.mapCoseAlgToJca(algId);

    CBORObject sigStructure = CBORObject.NewArray();
    sigStructure.Add(MdocConstants.Cose.SIGNATURE1);
    sigStructure.Add(protectedHeaderBytes);
    sigStructure.Add(new byte[0]);
    sigStructure.Add(payload);

    byte[] toBeSigned = sigStructure.EncodeToBytes();

    Signature verifier = Signature.getInstance(jcaAlgorithm, "BC");
    verifier.initVerify(publicKey);
    verifier.update(toBeSigned);

    byte[] derSignature = CoseSignatureUtils.convertRawToDer(signatureBytes);
    if (!verifier.verify(derSignature)) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_SIGNATURE_FAILED.getMsg());
    }

    Log.i(TAG, "IssuerAuth signature verification successful");
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_SIGNATURE_FAILED.getMsg()
          + ": " + e.getMessage(), e);
    }
  }

  private void verifyIssuerSignedItemDigests(CBORObject document, CBORObject issuerAuth)
      throws MdocException {
    try {
    CBORObject mso = MDocParser.extractMSO(issuerAuth);

    CBORObject valueDigests = mso.get(MdocConstants.Mso.VALUE_DIGESTS);
    if (valueDigests == null || valueDigests.getType() != CBORType.Map) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": No valueDigests in MSO");
    }

    String digestAlgorithm = MdocConstants.Mso.SHA_256;
    CBORObject digestAlgObj = mso.get(MdocConstants.Mso.DIGEST_ALGORITHM);
    if (digestAlgObj != null) {
      digestAlgorithm = digestAlgObj.AsString();
    }
    MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);

    CBORObject issuerSigned = document.get(MdocConstants.Document.ISSUER_SIGNED);
    CBORObject nameSpaces = issuerSigned.get(MdocConstants.IssuerSigned.NAME_SPACES);
    if (nameSpaces == null || nameSpaces.getType() != CBORType.Map) {
      Log.w(TAG, "No nameSpaces in issuerSigned - nothing to verify");
      return;
    }

    int verifiedCount = 0;
    for (CBORObject nsKey : nameSpaces.getKeys()) {
      String nameSpace = nsKey.AsString();
      CBORObject items = nameSpaces.get(nsKey);
      CBORObject nsDigests = valueDigests.get(nsKey);

      if (nsDigests == null || nsDigests.getType() != CBORType.Map) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DIGEST_MISMATCH.getMsg()
            + ": No valueDigests for namespace: " + nameSpace);
      }

      if (items.getType() != CBORType.Array) {
        continue;
      }

      for (int i = 0; i < items.size(); i++) {
        CBORObject itemWrapped = items.get(i);

        byte[] innerBytes;
        if (itemWrapped.isTagged() && itemWrapped.getMostInnerTag().ToInt32Checked() == 24) {
          innerBytes = itemWrapped.GetByteString();
        } else {
          innerBytes = itemWrapped.EncodeToBytes();
        }

        CBORObject item = CBORObject.DecodeFromBytes(innerBytes);
        CBORObject digestIdObj = item.get(MdocConstants.IssuerSignedItem.DIGEST_ID);
        if (digestIdObj == null) {
          throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
              + ": IssuerSignedItem missing digestID in namespace: " + nameSpace);
        }
        int digestId = digestIdObj.AsInt32Value();
        String elementId = item.get(MdocConstants.IssuerSignedItem.ELEMENT_IDENTIFIER) != null
            ? item.get(MdocConstants.IssuerSignedItem.ELEMENT_IDENTIFIER).AsString() : "unknown";

        CBORObject expectedHashObj = nsDigests.get(CBORObject.FromObject(digestId));
        if (expectedHashObj == null) {
          throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DIGEST_MISMATCH.getMsg()
              + ": No digest in MSO for namespace: " + nameSpace + ", digestID: " + digestId);
        }
        byte[] expectedHash = expectedHashObj.GetByteString();

        byte[] tag24Bytes = CBORObject.FromObject(innerBytes).WithTag(24).EncodeToBytes();
        byte[] computedHash = digest.digest(tag24Bytes);

        if (!MessageDigest.isEqual(computedHash, expectedHash)) {
          throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DIGEST_MISMATCH.getMsg()
              + ": namespace: " + nameSpace + ", digestID: " + digestId
              + ", element: " + elementId);
        }

        verifiedCount++;
      }
    }

    Log.i(TAG, "Verified " + verifiedCount + " IssuerSignedItem digest(s) across " + nameSpaces.getKeys().size() + " namespace(s)");
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DIGEST_MISMATCH.getMsg()
          + ": " + e.getMessage(), e);
    }
  }

  private void verifyDeviceAuth(String mdocBase64, CBORObject document,
      String clientId, String nonce, String responseUri) throws MdocException {
    CBORObject deviceSigned = document.get(MdocConstants.Document.DEVICE_SIGNED);
    if (deviceSigned == null) {
      if (requireDeviceAuth) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
            + ": No deviceSigned in document");
      }
      Log.w(TAG, "No deviceSigned in document - skipping DeviceAuth");
      return;
    }

    CBORObject deviceAuth = deviceSigned.get(MdocConstants.DeviceSigned.DEVICE_AUTH);
    if (deviceAuth == null) {
      if (requireDeviceAuth) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
            + ": No deviceAuth in deviceSigned");
      }
      Log.w(TAG, "No deviceAuth in deviceSigned - skipping DeviceAuth");
      return;
    }

    CBORObject deviceSignature = deviceAuth.get(MdocConstants.DeviceSigned.DEVICE_SIGNATURE);
    if (deviceSignature == null) {
      if (requireDeviceAuth) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
            + ": No deviceSignature found");
      }
      Log.w(TAG, "No deviceSignature in deviceAuth - skipping");
      return;
    }

    CBORObject sessionTranscript = buildOID4VPSessionTranscript(clientId, nonce, responseUri);

    CBORObject docTypeObj = document.get(MdocConstants.Document.DOC_TYPE);
    if (docTypeObj == null) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": No docType in document");
    }

    CBORObject deviceNameSpacesBytes = deviceSigned.get(MdocConstants.DeviceSigned.NAME_SPACES);
    byte[] deviceNameSpacesCbor;
    if (deviceNameSpacesBytes != null && deviceNameSpacesBytes.isTagged()
        && deviceNameSpacesBytes.getMostInnerTag().ToInt32Checked() == 24) {
      deviceNameSpacesCbor = deviceNameSpacesBytes.GetByteString();
    } else if (deviceNameSpacesBytes != null) {
      deviceNameSpacesCbor = deviceNameSpacesBytes.EncodeToBytes();
    } else {
      deviceNameSpacesCbor = CBORObject.NewMap().EncodeToBytes();
    }

    CBORObject deviceAuthentication = CBORObject.NewArray();
    deviceAuthentication.Add(MdocConstants.DeviceAuthentication.DEVICE_AUTHENTICATION);
    deviceAuthentication.Add(sessionTranscript);
    deviceAuthentication.Add(docTypeObj.AsString());
    deviceAuthentication.Add(CBORObject.FromObject(deviceNameSpacesCbor).WithTag(24));

    byte[] deviceAuthenticationBytes = CBORObject.FromObject(
        deviceAuthentication.EncodeToBytes()).WithTag(24).EncodeToBytes();

    PublicKey deviceKey = extractDeviceKeyFromMSO(mdocBase64);
    if (deviceKey == null) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
          + ": Failed to extract DeviceKey from MSO");
    }

    verifyDeviceSignature(deviceSignature, deviceAuthenticationBytes, deviceKey);
  }

  private CBORObject buildOID4VPSessionTranscript(String clientId, String nonce,
      String responseUri) throws MdocException {
    try {
      CBORObject handoverInfo = CBORObject.NewArray();
      handoverInfo.Add(clientId);
      handoverInfo.Add(nonce);
      handoverInfo.Add(CBORObject.Null);
      handoverInfo.Add(responseUri);

      byte[] handoverInfoHash = MessageDigest.getInstance("SHA-256")
          .digest(handoverInfo.EncodeToBytes());

      CBORObject handover = CBORObject.NewArray();
      handover.Add(MdocConstants.DeviceAuthentication.OPENID4VP_HANDOVER);
      handover.Add(handoverInfoHash);

      CBORObject sessionTranscript = CBORObject.NewArray();
      sessionTranscript.Add(CBORObject.Null);
      sessionTranscript.Add(CBORObject.Null);
      sessionTranscript.Add(handover);

      return sessionTranscript;
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
          + ": Failed to build session transcript: " + e.getMessage(), e);
    }
  }

  private void verifyDeviceSignature(CBORObject deviceSignature,
      byte[] deviceAuthenticationBytes, PublicKey deviceKey) throws MdocException {
    if (deviceSignature == null || deviceSignature.size() < 4) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
          + ": Invalid deviceSignature structure");
    }

    try {
      byte[] protectedHeaderBytes = deviceSignature.get(0).GetByteString();
      byte[] signatureBytes = deviceSignature.get(3).GetByteString();

      CBORObject protectedHeader = CBORObject.DecodeFromBytes(protectedHeaderBytes);
      CBORObject algObj = protectedHeader.get(CBORObject.FromObject(1));
      int algId = algObj != null ? algObj.AsInt32Value() : -7;

      CBORObject sigStructure = CBORObject.NewArray();
      sigStructure.Add(MdocConstants.Cose.SIGNATURE1);
      sigStructure.Add(protectedHeaderBytes);
      sigStructure.Add(new byte[0]);
      sigStructure.Add(deviceAuthenticationBytes);

      byte[] toBeSigned = sigStructure.EncodeToBytes();

      Signature verifier = Signature.getInstance(CoseSignatureUtils.mapCoseAlgToJca(algId), "BC");
      verifier.initVerify(deviceKey);
      verifier.update(toBeSigned);

      byte[] derSignature = CoseSignatureUtils.convertRawToDer(signatureBytes);
      if (!verifier.verify(derSignature)) {
        throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
            + ": DeviceAuth signature verification failed");
      }

      Log.i(TAG, "DeviceAuth signature verification successful");
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_DEVICE_AUTH_FAILED.getMsg()
          + ": " + e.getMessage(), e);
    }
  }

  private void validateCertificateChain(List<X509Certificate> certChain,
      List<X509Certificate> trustedRoots) throws MdocException {
    try {
    Set<TrustAnchor> trustAnchors = new HashSet<>();
    for (X509Certificate root : trustedRoots) {
      trustAnchors.add(new TrustAnchor(root, null));
    }

    List<X509Certificate> intermediateCerts = new ArrayList<>();
    if (certChain.size() > 1) {
      intermediateCerts.addAll(certChain.subList(1, certChain.size()));
    }
    intermediateCerts.addAll(trustedRoots);

    CertStore certStore = CertStore.getInstance("Collection",
        new CollectionCertStoreParameters(intermediateCerts));

    X509CertSelector targetSelector = new X509CertSelector();
    targetSelector.setCertificate(certChain.get(0));

    PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetSelector);
    params.addCertStore(certStore);
    params.setRevocationEnabled(false);

    CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
    PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) builder.build(params);

    Log.i(TAG, "Certificate chain validation successful. Trust anchor: " +
        result.getTrustAnchor().getTrustedCert().getSubjectX500Principal());
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_SIGNATURE_FAILED.getMsg()
          + ": Certificate chain validation failed: " + e.getMessage(), e);
    }
  }

  private PublicKey extractDeviceKeyFromMSO(String mdocBase64) throws MdocException {
    CBORObject issuerAuth = MDocParser.extractIssuerAuth(mdocBase64);
    CBORObject mso = MDocParser.extractMSO(issuerAuth);
    CBORObject deviceKeyCose = MDocParser.extractDeviceKey(mso);

    if (deviceKeyCose == null) {
      return null;
    }

    return coseKeyToPublicKey(deviceKeyCose);
  }

  private PublicKey coseKeyToPublicKey(CBORObject coseKey) throws MdocException {
    CBORObject ktyObj = coseKey.get(CBORObject.FromObject(1));
    if (ktyObj == null || ktyObj.AsInt32Value() != 2) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_UNSUPPORTED_KEY.getMsg()
          + ": expected EC2 (2), got: " + ktyObj);
    }

    try {
      CBORObject crvObj = coseKey.get(CBORObject.FromObject(-1));
      int crv = crvObj != null ? crvObj.AsInt32Value() : 1;

      byte[] x = coseKey.get(CBORObject.FromObject(-2)).GetByteString();
      byte[] y = coseKey.get(CBORObject.FromObject(-3)).GetByteString();

      String curveName;
      switch (crv) {
        case 1: curveName = "secp256r1"; break;
        case 2: curveName = "secp384r1"; break;
        case 3: curveName = "secp521r1"; break;
        default: throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_UNSUPPORTED_KEY.getMsg()
            + ": Unsupported curve: " + crv);
      }

      byte[] uncompressedPoint = new byte[1 + x.length + y.length];
      uncompressedPoint[0] = 0x04;
      System.arraycopy(x, 0, uncompressedPoint, 1, x.length);
      System.arraycopy(y, 0, uncompressedPoint, 1 + x.length, y.length);

      java.security.spec.ECParameterSpec ecSpec = getECParameterSpec(curveName);
      java.security.spec.ECPoint point = decodeECPoint(uncompressedPoint, ecSpec.getCurve());

      KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
      return keyFactory.generatePublic(new java.security.spec.ECPublicKeySpec(point, ecSpec));
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_UNSUPPORTED_KEY.getMsg()
          + ": Failed to convert COSE key to PublicKey: " + e.getMessage(), e);
    }
  }

  private java.security.spec.ECParameterSpec getECParameterSpec(String curveName) {
    org.bouncycastle.jce.spec.ECNamedCurveParameterSpec bcSpec =
        org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(curveName);
    return new java.security.spec.ECParameterSpec(
        new EllipticCurve(
            new java.security.spec.ECFieldFp(bcSpec.getCurve().getField().getCharacteristic()),
            bcSpec.getCurve().getA().toBigInteger(),
            bcSpec.getCurve().getB().toBigInteger()),
        new java.security.spec.ECPoint(
            bcSpec.getG().getAffineXCoord().toBigInteger(),
            bcSpec.getG().getAffineYCoord().toBigInteger()),
        bcSpec.getN(),
        bcSpec.getH().intValue());
  }

  private java.security.spec.ECPoint decodeECPoint(byte[] uncompressed, EllipticCurve curve) {
    int fieldSize = (curve.getField().getFieldSize() + 7) / 8;
    byte[] xBytes = new byte[fieldSize];
    byte[] yBytes = new byte[fieldSize];
    System.arraycopy(uncompressed, 1, xBytes, 0, fieldSize);
    System.arraycopy(uncompressed, 1 + fieldSize, yBytes, 0, fieldSize);
    return new java.security.spec.ECPoint(
        new java.math.BigInteger(1, xBytes),
        new java.math.BigInteger(1, yBytes));
  }

  private MDocClaimsSet buildClaimsSet(CBORObject document) {
    String docType = null;
    CBORObject docTypeObj = document.get(MdocConstants.Document.DOC_TYPE);
    if (docTypeObj != null) {
      docType = docTypeObj.AsString();
    }

    Map<String, Map<String, Object>> nameSpaceClaims = new LinkedHashMap<>();

    CBORObject issuerSigned = document.get(MdocConstants.Document.ISSUER_SIGNED);
    if (issuerSigned != null) {
      CBORObject nameSpaces = issuerSigned.get(MdocConstants.IssuerSigned.NAME_SPACES);
      if (nameSpaces != null && nameSpaces.getType() == CBORType.Map) {
        for (CBORObject nsKey : nameSpaces.getKeys()) {
          String ns = nsKey.AsString();
          CBORObject items = nameSpaces.get(nsKey);
          if (items.getType() != CBORType.Array) {
            continue;
          }

          Map<String, Object> nsClaims = new LinkedHashMap<>();
          for (int i = 0; i < items.size(); i++) {
            CBORObject itemWrapped = items.get(i);
            byte[] innerBytes;
            if (itemWrapped.isTagged()
                && itemWrapped.getMostInnerTag().ToInt32Checked() == 24) {
              innerBytes = itemWrapped.GetByteString();
            } else {
              innerBytes = itemWrapped.EncodeToBytes();
            }
            CBORObject item = CBORObject.DecodeFromBytes(innerBytes);
            String elementId = item.get(MdocConstants.IssuerSignedItem.ELEMENT_IDENTIFIER) != null
                ? item.get(MdocConstants.IssuerSignedItem.ELEMENT_IDENTIFIER).AsString() : null;
            CBORObject elementValue = item.get(MdocConstants.IssuerSignedItem.ELEMENT_VALUE);

            if (elementId != null && elementValue != null) {
              nsClaims.put(elementId, MDocParser.cborToJavaObject(elementValue));
            }
          }
          nameSpaceClaims.put(ns, nsClaims);
        }
      }
    }

    return new MDocClaimsSet(docType, nameSpaceClaims);
  }

  /**
   * Represents the verified claims extracted from an mDoc credential.
   *
   * <p>Claims are organized by namespace (e.g., "eu.europa.ec.eudi.pid.1") and can be
   * accessed either as a flat map or per-namespace. All maps returned are unmodifiable.</p>
   */
  public static class MDocClaimsSet {

    private final String docType;
    private final Map<String, Map<String, Object>> nameSpaces;

    public MDocClaimsSet(String docType, Map<String, Map<String, Object>> nameSpaces) {
      this.docType = docType;
      Map<String, Map<String, Object>> unmodifiable = new LinkedHashMap<>();
      for (Map.Entry<String, Map<String, Object>> entry : nameSpaces.entrySet()) {
        unmodifiable.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
      }
      this.nameSpaces = Collections.unmodifiableMap(unmodifiable);
    }

    public String getDocType() {
      return docType;
    }

    /**
     * Returns all claims from all namespaces merged into a flat map.
     *
     * @return unmodifiable map of all element identifiers to their values
     */
    public Map<String, Object> getClaims() {
      Map<String, Object> allClaims = new LinkedHashMap<>();
      for (Map<String, Object> nsClaims : nameSpaces.values()) {
        allClaims.putAll(nsClaims);
      }
      return Collections.unmodifiableMap(allClaims);
    }

    public Map<String, Object> getNameSpaceClaims(String nameSpace) {
      return nameSpaces.getOrDefault(nameSpace, Collections.emptyMap());
    }

    public Set<String> getNameSpaces() {
      return nameSpaces.keySet();
    }

    public Set<String> getElementIdentifiers(String nameSpace) {
      Map<String, Object> nsClaims = nameSpaces.get(nameSpace);
      return nsClaims != null ? nsClaims.keySet() : Collections.emptySet();
    }

    public Object getClaim(String nameSpace, String elementIdentifier) {
      Map<String, Object> nsClaims = nameSpaces.get(nameSpace);
      return nsClaims != null ? nsClaims.get(elementIdentifier) : null;
    }

    public String getStringClaim(String nameSpace, String elementIdentifier) {
      Object value = getClaim(nameSpace, elementIdentifier);
      return value != null ? value.toString() : null;
    }

    public Long getLongClaim(String nameSpace, String elementIdentifier) {
      Object value = getClaim(nameSpace, elementIdentifier);
      if (value instanceof Number) {
        return ((Number) value).longValue();
      }
      return null;
    }

    public Boolean getBooleanClaim(String nameSpace, String elementIdentifier) {
      Object value = getClaim(nameSpace, elementIdentifier);
      if (value instanceof Boolean) {
        return (Boolean) value;
      }
      return null;
    }

    public boolean hasClaim(String nameSpace, String elementIdentifier) {
      Map<String, Object> nsClaims = nameSpaces.get(nameSpace);
      return nsClaims != null && nsClaims.containsKey(elementIdentifier);
    }

    @Override
    public String toString() {
      return "MDocClaimsSet{" +
          "docType='" + docType + '\'' +
          ", nameSpaces=" + nameSpaces.keySet() +
          ", totalClaims=" + nameSpaces.values().stream().mapToInt(Map::size).sum() +
          '}';
    }
  }
}
