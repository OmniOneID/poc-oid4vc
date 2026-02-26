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

package org.omnione.did.oid4vc.oid4vp.util.jar.jws.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.JWSSigner;

public class RSASSASigner implements JWSSigner {

  private final RSAPrivateKey privateKey;

  public RSASSASigner(RSAPrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  @Override
  public byte[] sign(String signingInput) throws OID4VPException {
    try {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(privateKey);
      signature.update(signingInput.getBytes());

      byte[] signatureBytes = signature.sign();

      int keySize = privateKey.getModulus().bitLength();
      int expectedLength = keySize / 8;

      if (signatureBytes.length == expectedLength) {

        return signatureBytes;
      } else if (signatureBytes.length > expectedLength) {

        return extractRSASignature(signatureBytes, expectedLength);
      } else {

        return padToExpectedLength(signatureBytes, expectedLength);
      }
    } catch (NoSuchAlgorithmException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE, "SHA256withRSA", e);
    } catch (InvalidKeyException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_KEY, e.getMessage(), e);
    } catch (SignatureException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_SIGN_FAILED, e.getMessage(), e);
    }
  }

  private byte[] extractRSASignature(byte[] signatureBytes, int expectedLength) {

    if (signatureBytes.length > expectedLength + 2 && signatureBytes[0] == 0x30) {

      // Try to parse as DER-wrapped signature
      int sequenceLength = signatureBytes[1] & 0xFF;
      int contentStart = 2;

      if ((sequenceLength & 0x80) != 0) {
        int lengthBytes = sequenceLength & 0x7F;
        if (lengthBytes <= 4 && contentStart + lengthBytes < signatureBytes.length) {
          sequenceLength = 0;
          for (int i = 0; i < lengthBytes; i++) {
            sequenceLength = (sequenceLength << 8) | (signatureBytes[contentStart++] & 0xFF);
          }
        }
      }

      if (contentStart < signatureBytes.length && signatureBytes[contentStart] == 0x04) {

        contentStart++;
        if (contentStart < signatureBytes.length) {
          int octetLength = signatureBytes[contentStart++] & 0xFF;
          if (octetLength == expectedLength
              && contentStart + octetLength <= signatureBytes.length) {
            byte[] extracted = new byte[expectedLength];
            System.arraycopy(signatureBytes, contentStart, extracted, 0, expectedLength);
            return extracted;
          }
        }
      } else if (sequenceLength == expectedLength
          && contentStart + expectedLength <= signatureBytes.length) {

        byte[] extracted = new byte[expectedLength];
        System.arraycopy(signatureBytes, contentStart, extracted, 0, expectedLength);
        return extracted;
      }
    }

    if (signatureBytes.length >= expectedLength) {
      byte[] trimmed = new byte[expectedLength];
      System.arraycopy(signatureBytes, signatureBytes.length - expectedLength, trimmed, 0,
          expectedLength);
      return trimmed;
    }

    return signatureBytes;
  }

  private byte[] padToExpectedLength(byte[] signatureBytes, int expectedLength) {
    byte[] padded = new byte[expectedLength];
    int paddingLength = expectedLength - signatureBytes.length;

    for (int i = 0; i < paddingLength; i++) {
      padded[i] = 0;
    }

    System.arraycopy(signatureBytes, 0, padded, paddingLength, signatureBytes.length);

    return padded;
  }
}
