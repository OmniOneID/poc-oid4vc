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
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.omnione.did.sdk.mdoc.oid4vc.constant.MdocConstants;
import org.omnione.did.sdk.mdoc.oid4vc.exception.MdocErrorCode;
import org.omnione.did.sdk.mdoc.oid4vc.exception.MdocException;

/**
 * Utility class for parsing mDoc CBOR structures.
 *
 * <p>Provides static methods to decode, extract, and convert mDoc CBOR data including
 * DeviceResponse, Document, IssuerAuth (COSE_Sign1), MSO (Mobile Security Object),
 * X.509 certificate chains, and device keys.</p>
 *
 * <p>All methods handle CBOR tag 24 wrapping automatically.</p>
 *
 * @see MDocVerifier
 * @see OID4VPHandler
 */
public final class MDocParser {

  private static final String TAG = MDocParser.class.getSimpleName();

  private MDocParser() {
  }

  /**
   * Decodes a Base64URL or standard Base64 encoded string.
   * Tries Base64URL first, falls back to standard Base64.
   *
   * @param input Base64URL or Base64 encoded string
   * @return decoded bytes, or null if both decodings fail
   */
  public static byte[] decodeBase64(String input) {
    try {
      return Base64.getUrlDecoder().decode(input);
    } catch (IllegalArgumentException e) {
      try {
        return Base64.getDecoder().decode(input);
      } catch (IllegalArgumentException e2) {
        return null;
      }
    }
  }

  /**
   * Decodes a Base64-encoded string to a DeviceResponse CBOR object.
   * Automatically unwraps CBOR tag 24 if present.
   *
   * @param mdocBase64 Base64URL or Base64 encoded mDoc data
   * @return decoded CBOR object
   * @throws MdocException if decoding or CBOR parsing fails
   */
  public static CBORObject decodeDeviceResponse(String mdocBase64) throws MdocException {
    byte[] decoded = decodeBase64(mdocBase64.trim());
    if (decoded == null) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_PARSE_FAILED.getMsg()
          + ": Failed to Base64 decode");
    }

    try {
      CBORObject cbor = CBORObject.DecodeFromBytes(decoded);
      if (cbor.isTagged() && cbor.getMostInnerTag().ToInt32Checked() == 24) {
        cbor = CBORObject.DecodeFromBytes(cbor.GetByteString());
      }
      return cbor;
    } catch (Exception e) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_PARSE_FAILED.getMsg()
          + ": " + e.getMessage(), e);
    }
  }

  /**
   * Extracts the first Document from a DeviceResponse.
   *
   * @param mdocBase64 Base64URL-encoded DeviceResponse
   * @return the first Document CBOR object
   * @throws MdocException if no documents are found in the DeviceResponse
   */
  public static CBORObject extractFirstDocument(String mdocBase64) throws MdocException {
    CBORObject deviceResponse = decodeDeviceResponse(mdocBase64);

    CBORObject documents = deviceResponse.get(MdocConstants.DeviceResponse.DOCUMENTS);
    if (documents == null || documents.getType() != CBORType.Array || documents.size() == 0) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": No documents in DeviceResponse");
    }

    return documents.get(0);
  }

  /**
   * Extracts the IssuerAuth (COSE_Sign1) from the first document in a DeviceResponse.
   *
   * @param mdocBase64 Base64URL-encoded DeviceResponse
   * @return the IssuerAuth CBOR array (COSE_Sign1 structure)
   * @throws MdocException if issuerSigned or issuerAuth is not found
   */
  public static CBORObject extractIssuerAuth(String mdocBase64) throws MdocException {
    CBORObject document = extractFirstDocument(mdocBase64);

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

    return issuerAuth;
  }

  /**
   * Extracts the X.509 certificate chain (x5chain, COSE label 33) from a COSE_Sign1 structure.
   * Checks unprotected header first, then protected header.
   *
   * @param coseSign1 COSE_Sign1 CBOR array
   * @return list of DER-encoded certificate bytes, or null if not found
   */
  public static List<byte[]> extractX5Chain(CBORObject coseSign1) {
    if (coseSign1 == null || coseSign1.size() < 4) {
      return null;
    }

    CBORObject unprotectedHeader = coseSign1.get(1);
    if (unprotectedHeader != null && unprotectedHeader.getType() == CBORType.Map) {
      CBORObject x5chainObj = unprotectedHeader.get(CBORObject.FromObject(33));
      if (x5chainObj != null) {
        return parseX5Chain(x5chainObj);
      }
    }

    try {
      CBORObject protectedHeaderBstr = coseSign1.get(0);
      if (protectedHeaderBstr != null) {
        CBORObject protectedHeader = CBORObject.DecodeFromBytes(
            protectedHeaderBstr.GetByteString());
        CBORObject x5chainObj = protectedHeader.get(CBORObject.FromObject(33));
        if (x5chainObj != null) {
          return parseX5Chain(x5chainObj);
        }
      }
    } catch (Exception e) {
      Log.d(TAG, "Failed to extract x5chain from protected header: " + e.getMessage());
    }

    return null;
  }

  /**
   * Extracts the Mobile Security Object (MSO) from an IssuerAuth COSE_Sign1 payload.
   * Automatically handles tag 24 wrapping.
   *
   * @param issuerAuth COSE_Sign1 CBOR array
   * @return the MSO CBOR map
   * @throws MdocException if the payload is null or cannot be parsed
   */
  public static CBORObject extractMSO(CBORObject issuerAuth) throws MdocException {
    CBORObject payloadObj = issuerAuth.get(2);
    if (payloadObj == null || payloadObj.isNull()) {
      throw new MdocException(MdocErrorCode.ERR_CODE_OID4VP_INVALID_CREDENTIAL.getMsg()
          + ": IssuerAuth payload is null");
    }

    byte[] payloadBytes = payloadObj.GetByteString();
    CBORObject mso = CBORObject.DecodeFromBytes(payloadBytes);
    if (mso.isTagged() && mso.getMostInnerTag().ToInt32Checked() == 24) {
      mso = CBORObject.DecodeFromBytes(mso.GetByteString());
    }

    return mso;
  }

  /**
   * Extracts the device public key (COSE_Key) from the MSO's deviceKeyInfo.
   *
   * @param mso the Mobile Security Object CBOR map
   * @return the deviceKey COSE_Key CBOR map, or null if not present
   */
  public static CBORObject extractDeviceKey(CBORObject mso) {
    CBORObject deviceKeyInfo = mso.get(MdocConstants.Mso.DEVICE_KEY_INFO);
    if (deviceKeyInfo == null) {
      return null;
    }

    return deviceKeyInfo.get(MdocConstants.DeviceKeyInfo.DEVICE_KEY);
  }

  /**
   * Converts a CBOR object to its Java equivalent.
   *
   * <p>Mapping: TextString->String, Integer->Integer/Long, Boolean->Boolean,
   * ByteString->byte[], FloatingPoint->Double, Array->List, Map->LinkedHashMap.
   * Tagged values are untagged before conversion.</p>
   *
   * @param cbor the CBOR object to convert
   * @return the Java equivalent, or null if the input is null
   */
  public static Object cborToJavaObject(CBORObject cbor) {
    if (cbor == null || cbor.isNull()) {
      return null;
    }
    if (cbor.isTagged()) {
      cbor = cbor.UntagOne();
    }
    switch (cbor.getType()) {
      case TextString:
        return cbor.AsString();
      case Integer:
        return cbor.CanValueFitInInt32() ? cbor.AsInt32Value() : cbor.AsInt64Value();
      case Boolean:
        return cbor.isTrue();
      case ByteString:
        return cbor.GetByteString();
      case FloatingPoint:
        return cbor.AsDoubleValue();
      case Array: {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < cbor.size(); i++) {
          list.add(cborToJavaObject(cbor.get(i)));
        }
        return list;
      }
      case Map: {
        Map<String, Object> map = new LinkedHashMap<>();
        for (CBORObject key : cbor.getKeys()) {
          map.put(key.AsString(), cborToJavaObject(cbor.get(key)));
        }
        return map;
      }
      default:
        return cbor.toString();
    }
  }

  private static List<byte[]> parseX5Chain(CBORObject x5chainObj) {
    List<byte[]> certs = new ArrayList<>();

    if (x5chainObj.getType() == CBORType.ByteString) {
      certs.add(x5chainObj.GetByteString());
    } else if (x5chainObj.getType() == CBORType.Array) {
      for (int i = 0; i < x5chainObj.size(); i++) {
        certs.add(x5chainObj.get(i).GetByteString());
      }
    }

    return certs;
  }
}
