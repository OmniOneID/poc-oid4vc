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
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.sdk.mdoc.oid4vc.constant.MdocConstants;
import org.omnione.did.sdk.mdoc.oid4vc.datamodel.DeviceSignedDocument;
import org.omnione.did.sdk.mdoc.oid4vc.exception.MdocErrorCode;
import org.omnione.did.sdk.mdoc.oid4vc.exception.MdocException;
import org.omnione.did.sdk.mdoc.oid4vc.util.CoseSignatureUtils;

/**
 * Handles OID4VP (OpenID for Verifiable Presentation) operations for mDoc credentials.
 *
 * <p>Provides static methods to create VP Tokens from IssuerSigned CBOR with support for:
 * <ul>
 *   <li>Selective disclosure - filtering claims by namespace and element identifier</li>
 *   <li>DeviceAuth - COSE_Sign1 device authentication per ISO/IEC 18013-5</li>
 *   <li>DCQL (Digital Credentials Query Language) ID wrapping</li>
 * </ul>
 *
 * <p>The VP Token is a Base64URL-encoded CBOR DeviceResponse structure containing
 * the IssuerSigned data and optional DeviceSigned data with device authentication.</p>
 *
 * @see MDocVerifier
 * @see IssuerSignedItemFilter
 */
public class OID4VPHandler {

  private static final String TAG = OID4VPHandler.class.getSimpleName();

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  // ── Auto-detect docType overloads (issuerSignedBase64 only) ──

  /**
   * Creates a VP Token with all claims and no DeviceAuth.
   * DocType is automatically resolved from the first namespace in IssuerSigned.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @return Base64URL-encoded DeviceResponse CBOR
   * @throws MdocException if decoding fails or namespace is unrecognized
   */
  public static String createVPToken(String issuerSignedBase64) throws MdocException {
    return createVPToken(issuerSignedBase64, resolveDocType(issuerSignedBase64));
  }

  /**
   * Creates a VP Token with selective disclosure and no DeviceAuth.
   * DocType is automatically resolved from the first namespace in IssuerSigned.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param requestedClaims    map of namespace to set of requested element identifiers
   * @return Base64URL-encoded DeviceResponse CBOR
   * @throws MdocException if the credential is invalid or namespace is unrecognized
   */
  public static String createVPToken(String issuerSignedBase64,
      Map<String, Set<String>> requestedClaims) throws MdocException {
    return createVPToken(issuerSignedBase64, resolveDocType(issuerSignedBase64), requestedClaims);
  }

  /**
   * Creates a VP Token with all claims and DeviceAuth.
   * DocType is automatically resolved from the first namespace in IssuerSigned.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param holderPrivateKey   holder's EC private key for DeviceAuth signing
   * @param clientId           OID4VP client_id (verifier identifier)
   * @param nonce              OID4VP nonce for session binding
   * @param responseUri        OID4VP response_uri
   * @return Base64URL-encoded DeviceResponse CBOR with DeviceAuth
   * @throws MdocException if the credential is invalid or namespace is unrecognized
   */
  public static String createVPToken(String issuerSignedBase64,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {
    return createVPToken(issuerSignedBase64, resolveDocType(issuerSignedBase64),
        null, holderPrivateKey, clientId, nonce, responseUri);
  }

  /**
   * Creates a VP Token with selective disclosure and DeviceAuth.
   * DocType is automatically resolved from the first namespace in IssuerSigned.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param requestedClaims    map of namespace to set of requested element identifiers
   * @param holderPrivateKey   holder's EC private key for DeviceAuth signing
   * @param clientId           OID4VP client_id (verifier identifier)
   * @param nonce              OID4VP nonce for session binding
   * @param responseUri        OID4VP response_uri
   * @return Base64URL-encoded DeviceResponse CBOR with DeviceAuth
   * @throws MdocException if the credential is invalid or namespace is unrecognized
   */
  public static String createVPToken(String issuerSignedBase64,
      Map<String, Set<String>> requestedClaims,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {
    return createVPToken(issuerSignedBase64, resolveDocType(issuerSignedBase64),
        requestedClaims, holderPrivateKey, clientId, nonce, responseUri);
  }

  /**
   * Creates a VP Token wrapped in DCQL JSON format.
   * DocType is automatically resolved from the first namespace in IssuerSigned.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param requestedClaims    map of namespace to set of requested element identifiers
   * @param dcqlId             DCQL credential query identifier
   * @param holderPrivateKey   holder's EC private key for DeviceAuth; null to skip
   * @param clientId           OID4VP client_id
   * @param nonce              OID4VP nonce
   * @param responseUri        OID4VP response_uri
   * @return JSON string in format: {"dcqlId":["vpToken"]}
   * @throws MdocException if VP Token creation fails or namespace is unrecognized
   * @deprecated This method will be removed in a future release.
   */
  @Deprecated
  public static String createVPTokenWithDcqlId(String issuerSignedBase64,
      Map<String, Set<String>> requestedClaims,
      String dcqlId,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {
    return createVPTokenWithDcqlId(issuerSignedBase64, resolveDocType(issuerSignedBase64),
        requestedClaims, dcqlId, holderPrivateKey, clientId, nonce, responseUri);
  }

  // ── Explicit docType overloads ──

  /**
   * Creates a VP Token with all claims and no DeviceAuth.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param docType            the document type (e.g., {@link MdocConstants.DocType#EUDI_PID_1})
   * @return Base64URL-encoded DeviceResponse CBOR
   * @throws MdocException if decoding fails or the IssuerSigned is invalid
   */
  public static String createVPToken(String issuerSignedBase64, String docType) throws MdocException {
    return createVPToken(issuerSignedBase64, docType, null, null, null, null, null);
  }

  /**
   * Creates a VP Token with selective disclosure and no DeviceAuth.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param docType            the document type
   * @param requestedClaims    map of namespace to set of requested element identifiers;
   *                           null or empty includes all claims
   * @return Base64URL-encoded DeviceResponse CBOR
   * @throws MdocException if the credential is invalid or VP Token creation fails
   */
  public static String createVPToken(String issuerSignedBase64, String docType,
      Map<String, Set<String>> requestedClaims) throws MdocException {
    return createVPToken(issuerSignedBase64, docType, requestedClaims, null, null, null, null);
  }

  /**
   * Creates a VP Token with all claims and DeviceAuth.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param docType            the document type
   * @param holderPrivateKey   holder's EC private key for DeviceAuth signing
   * @param clientId           OID4VP client_id (verifier identifier)
   * @param nonce              OID4VP nonce for session binding
   * @param responseUri        OID4VP response_uri
   * @return Base64URL-encoded DeviceResponse CBOR with DeviceAuth
   * @throws MdocException if the credential is invalid or VP Token creation fails
   */
  public static String createVPToken(String issuerSignedBase64, String docType,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {
    return createVPToken(issuerSignedBase64, docType, null, holderPrivateKey, clientId, nonce, responseUri);
  }

  /**
   * Creates a VP Token with selective disclosure and DeviceAuth.
   *
   * <p>This is the primary method that all other createVPToken overloads delegate to.
   * It performs the following steps:</p>
   * <ol>
   *   <li>Decodes IssuerSigned CBOR from Base64URL</li>
   *   <li>Extracts nameSpaces and issuerAuth from IssuerSigned</li>
   *   <li>Filters IssuerSignedItems by requestedClaims</li>
   *   <li>Builds DeviceSigned with optional DeviceAuth COSE_Sign1 signature</li>
   *   <li>Assembles and encodes the DeviceResponse</li>
   * </ol>
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param docType            the document type (e.g., {@link MdocConstants.DocType#EUDI_PID_1})
   * @param requestedClaims    map of namespace to set of requested element identifiers;
   *                           null or empty includes all claims
   * @param holderPrivateKey   holder's EC private key for DeviceAuth; null to skip DeviceAuth
   * @param clientId           OID4VP client_id; required when holderPrivateKey is provided
   * @param nonce              OID4VP nonce; required when holderPrivateKey is provided
   * @param responseUri        OID4VP response_uri; required when holderPrivateKey is provided
   * @return Base64URL-encoded DeviceResponse CBOR
   * @throws MdocException if the credential is invalid or VP Token creation fails
   */
  public static String createVPToken(String issuerSignedBase64, String docType,
      Map<String, Set<String>> requestedClaims,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {
    validateNotEmpty(issuerSignedBase64, "issuerSignedBase64");
    validateNotEmpty(docType, "docType");

    byte[] issuerSignedBytes = Base64.getUrlDecoder().decode(issuerSignedBase64);
    CBORObject issuerSigned = CBORObject.DecodeFromBytes(issuerSignedBytes);

    CBORObject nameSpaces = issuerSigned.get(MdocConstants.IssuerSigned.NAME_SPACES);
    CBORObject issuerAuth = issuerSigned.get(MdocConstants.IssuerSigned.ISSUER_AUTH);
    if (issuerAuth == null) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": No issuerAuth found in IssuerSigned");
    }

    CBORObject filteredNameSpaces = IssuerSignedItemFilter.filterNameSpaces(
        nameSpaces, requestedClaims);

    CBORObject rebuiltIssuerSigned = CBORObject.NewMap();
    rebuiltIssuerSigned.Add(MdocConstants.IssuerSigned.NAME_SPACES, filteredNameSpaces);
    rebuiltIssuerSigned.Add(MdocConstants.IssuerSigned.ISSUER_AUTH, issuerAuth);

    CBORObject deviceSigned = buildDeviceSigned(
        docType, holderPrivateKey, clientId, nonce, responseUri);

    DeviceSignedDocument doc = new DeviceSignedDocument(docType, rebuiltIssuerSigned, deviceSigned);
    CBORObject deviceResponse = doc.toDeviceResponse();

    String vpToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(deviceResponse.EncodeToBytes());

    Log.i(TAG, "Created mdoc VP Token - docType: " + docType +
        ", namespaces: " + filteredNameSpaces.getKeys().size() +
        ", deviceAuth: " + (holderPrivateKey != null ? "yes" : "no"));

    return vpToken;
  }

  /**
   * Creates a VP Token wrapped in DCQL (Digital Credentials Query Language) JSON format.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @param docType            the document type
   * @param requestedClaims    map of namespace to set of requested element identifiers
   * @param dcqlId             DCQL credential query identifier
   * @param holderPrivateKey   holder's EC private key for DeviceAuth; null to skip DeviceAuth
   * @param clientId           OID4VP client_id
   * @param nonce              OID4VP nonce
   * @param responseUri        OID4VP response_uri
   * @return JSON string in format: {"dcqlId":["vpToken"]}
   * @throws MdocException if VP Token creation fails
   * @deprecated This method will be removed in a future release.
   */
  @Deprecated
  public static String createVPTokenWithDcqlId(String issuerSignedBase64, String docType,
      Map<String, Set<String>> requestedClaims,
      String dcqlId,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {

    String vpToken = createVPToken(issuerSignedBase64, docType, requestedClaims,
        holderPrivateKey, clientId, nonce, responseUri);

    return "{\"" + dcqlId + "\":[\"" + vpToken + "\"]}";
  }

  /**
   * Builds the DeviceSigned structure with optional DeviceAuth.
   *
   * <p>DeviceNameSpaces is an empty map for OID4VP. If holder private key and
   * OID4VP parameters are provided, a COSE_Sign1 DeviceAuth signature is created.</p>
   */
  private static CBORObject buildDeviceSigned(String docType,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {

    // DeviceNameSpaces is an empty map in OID4VP
    byte[] deviceNameSpacesCbor = CBORObject.NewMap().EncodeToBytes();
    CBORObject deviceNameSpacesWrapped = CBORObject.FromObject(deviceNameSpacesCbor).WithTag(24);

    CBORObject deviceSigned = CBORObject.NewMap();
    deviceSigned.Add(MdocConstants.DeviceSigned.NAME_SPACES, deviceNameSpacesWrapped);

    // Create DeviceAuth if signing key is provided
    if (holderPrivateKey != null && clientId != null && nonce != null && responseUri != null) {
      CBORObject deviceAuth = CBORObject.NewMap();

      CBORObject deviceSignature = createDeviceSignature(
          docType, deviceNameSpacesCbor, holderPrivateKey, clientId, nonce, responseUri);

      deviceAuth.Add(MdocConstants.DeviceSigned.DEVICE_SIGNATURE, deviceSignature);
      deviceSigned.Add(MdocConstants.DeviceSigned.DEVICE_AUTH, deviceAuth);
    } else {
      CBORObject deviceAuth = CBORObject.NewMap();
      deviceSigned.Add(MdocConstants.DeviceSigned.DEVICE_AUTH, deviceAuth);
    }

    return deviceSigned;
  }

  /**
   * Creates a COSE_Sign1 device signature for DeviceAuth.
   *
   * <p>Constructs the DeviceAuthentication CBOR structure per ISO/IEC 18013-5,
   * builds the COSE Sig_structure, signs it, and returns a detached COSE_Sign1
   * (payload=null).</p>
   */
  private static CBORObject createDeviceSignature(String docType,
      byte[] deviceNameSpacesCbor,
      PrivateKey holderPrivateKey,
      String clientId,
      String nonce,
      String responseUri) throws MdocException {
    try {
      // SessionTranscript (Appendix B.3.4)
      CBORObject sessionTranscript = buildOID4VPSessionTranscript(clientId, nonce, responseUri);

      // DeviceAuthentication
      CBORObject deviceAuthentication = CBORObject.NewArray();
      deviceAuthentication.Add(MdocConstants.DeviceAuthentication.DEVICE_AUTHENTICATION);
      deviceAuthentication.Add(sessionTranscript);
      deviceAuthentication.Add(docType);
      deviceAuthentication.Add(CBORObject.FromObject(deviceNameSpacesCbor).WithTag(24));

      // tag24 wrapping
      byte[] deviceAuthenticationBytes = CBORObject.FromObject(
          deviceAuthentication.EncodeToBytes()).WithTag(24).EncodeToBytes();

      // Determine algorithm
      int coseAlg = determineCoseAlgorithm(holderPrivateKey);
      String jcaAlg = CoseSignatureUtils.mapCoseAlgToJca(coseAlg);

      // protected header
      CBORObject protectedHeader = CBORObject.NewMap();
      protectedHeader.Add(CBORObject.FromObject(1), coseAlg);
      byte[] protectedHeaderBytes = protectedHeader.EncodeToBytes();

      // Sig_structure
      CBORObject sigStructure = CBORObject.NewArray();
      sigStructure.Add(MdocConstants.Cose.SIGNATURE1);
      sigStructure.Add(protectedHeaderBytes);
      sigStructure.Add(new byte[0]);
      sigStructure.Add(deviceAuthenticationBytes);

      byte[] toBeSigned = sigStructure.EncodeToBytes();

      // Sign
      Signature signer = Signature.getInstance(jcaAlg, "BC");
      signer.initSign(holderPrivateKey);
      signer.update(toBeSigned);
      byte[] derSignature = signer.sign();

      // DER -> raw R||S
      byte[] rawSignature = CoseSignatureUtils.convertDerToRaw(derSignature, coseAlg);

      // COSE_Sign1 (payload=null, detached)
      CBORObject coseSign1 = CBORObject.NewArray();
      coseSign1.Add(protectedHeaderBytes);
      coseSign1.Add(CBORObject.NewMap());
      coseSign1.Add(CBORObject.Null);
      coseSign1.Add(rawSignature);

      Log.d(TAG, "Created deviceSignature COSE_Sign1 with algorithm: " + jcaAlg);
      return coseSign1;
    } catch (GeneralSecurityException e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_BASE.getMsg()
          + ": Failed to create device signature: " + e.getMessage(), e);
    }
  }

  /**
   * Builds the OID4VP SessionTranscript per OpenID4VP Appendix B.3.4.
   *
   * <p>Structure: [null, null, [OpenID4VPHandover]] where OpenID4VPHandover
   * contains the SHA-256 hash of [clientId, nonce, null, responseUri].</p>
   */
  private static CBORObject buildOID4VPSessionTranscript(String clientId, String nonce,
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
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_BASE.getMsg()
          + ": Failed to build session transcript: " + e.getMessage(), e);
    }
  }

  private static int determineCoseAlgorithm(PrivateKey privateKey) {
    if (privateKey instanceof ECPrivateKey) {
      ECPrivateKey ecKey = (ECPrivateKey) privateKey;
      int fieldSize = ecKey.getParams().getCurve().getField().getFieldSize();
      if (fieldSize <= 256) return -7;   // ES256
      if (fieldSize <= 384) return -35;  // ES384
      return -36;                         // ES512
    }
    return -7; // default ES256
  }

  /**
   * Resolves the docType from the first namespace found in IssuerSigned CBOR.
   *
   * @param issuerSignedBase64 Base64URL-encoded IssuerSigned CBOR
   * @return resolved docType string
   * @throws MdocException if namespace is missing or unrecognized
   */
  private static String resolveDocType(String issuerSignedBase64) {
    validateNotEmpty(issuerSignedBase64, "issuerSignedBase64");

    byte[] issuerSignedBytes = Base64.getUrlDecoder().decode(issuerSignedBase64);
    CBORObject issuerSigned = CBORObject.DecodeFromBytes(issuerSignedBytes);
    CBORObject nameSpaces = issuerSigned.get(MdocConstants.IssuerSigned.NAME_SPACES);

    if (nameSpaces == null || nameSpaces.getKeys().isEmpty()) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": No nameSpaces found in IssuerSigned");
    }

    String firstNameSpace = nameSpaces.getKeys().iterator().next().AsString();

    if (MdocConstants.Namespace.EUDI_PID_1.equals(firstNameSpace)) {
      return MdocConstants.DocType.EUDI_PID_1;
    } else if (MdocConstants.Namespace.ISO_18013_5_1.equals(firstNameSpace)) {
      return MdocConstants.DocType.ISO_18013_5_1_MDL;
    } else {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": Unrecognized namespace: " + firstNameSpace);
    }
  }

  private static void validateNotEmpty(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      throw new MdocException(MdocErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER.getMsg()
          + ": " + fieldName);
    }
  }
}
