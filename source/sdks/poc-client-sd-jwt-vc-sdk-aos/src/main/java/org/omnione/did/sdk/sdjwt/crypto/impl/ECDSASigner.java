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

package org.omnione.did.sdk.sdjwt.crypto.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.omnione.did.sdk.sdjwt.crypto.JWSSigner;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;

public class ECDSASigner implements JWSSigner {

  private final ECPrivateKey privateKey;
  private final String keyId;

  public ECDSASigner(ECPrivateKey privateKey) {
    this.privateKey = privateKey;
    this.keyId = null;
  }

  @Override
  public byte[] sign(String signingInput) throws SDJWTException {
    return signWithPrivateKey(signingInput);
  }

  private byte[] signWithPrivateKey(String signingInput) throws SDJWTException {
    try {
      Signature signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(privateKey);
      signature.update(signingInput.getBytes());

      byte[] signatureBytes = signature.sign();

      if (signatureBytes.length == 64) {

        return signatureBytes;
      } else {

        return convertDERToP1363(signatureBytes);
      }
    } catch (InvalidKeyException e) {
      throw new SDJWTException("Invalid EC private key: " + e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new SDJWTException("ECDSA algorithm not available: " + e.getMessage(), e);
    } catch (SignatureException e) {
      throw new SDJWTException("ECDSA signature operation failed: " + e.getMessage(), e);
    }
  }

  private byte[] convertDERToP1363(byte[] derSignature) throws SDJWTException {
    try {

      if (derSignature.length < 6 || derSignature[0] != 0x30) {
        throw new SDJWTException("Invalid DER signature format");
      }

      int sequenceLength = derSignature[1] & 0xFF;
      int offset = 2;

      if ((sequenceLength & 0x80) != 0) {
        int lengthBytes = sequenceLength & 0x7F;
        if (lengthBytes > 4 || offset + lengthBytes >= derSignature.length) {
          throw new SDJWTException("Invalid DER length encoding");
        }

        sequenceLength = 0;
        for (int i = 0; i < lengthBytes; i++) {
          sequenceLength = (sequenceLength << 8) | (derSignature[offset++] & 0xFF);
        }
      }

      if (offset >= derSignature.length || derSignature[offset] != 0x02) {
        throw new SDJWTException("Expected INTEGER tag for r");
      }
      offset++;

      int rLength = derSignature[offset++] & 0xFF;
      if (offset + rLength > derSignature.length) {
        throw new SDJWTException("Invalid r length");
      }

      byte[] rBytes = new byte[rLength];
      System.arraycopy(derSignature, offset, rBytes, 0, rLength);
      offset += rLength;

      if (offset >= derSignature.length || derSignature[offset] != 0x02) {
        throw new SDJWTException("Expected INTEGER tag for s");
      }
      offset++;

      int sLength = derSignature[offset++] & 0xFF;
      if (offset + sLength > derSignature.length) {
        throw new SDJWTException("Invalid s length");
      }

      byte[] sBytes = new byte[sLength];
      System.arraycopy(derSignature, offset, sBytes, 0, sLength);

      byte[] r = toFixedLength(rBytes, 32);
      byte[] s = toFixedLength(sBytes, 32);

      byte[] p1363Signature = new byte[64];
      System.arraycopy(r, 0, p1363Signature, 0, 32);
      System.arraycopy(s, 0, p1363Signature, 32, 32);

      return p1363Signature;

    } catch (SDJWTException e) {
      throw e;
    }
  }

  private byte[] toFixedLength(byte[] input, int targetLength) {
    if (input.length == targetLength) {
      return input;
    } else if (input.length > targetLength) {

      int leadingZeros = 0;
      for (int i = 0; i < input.length - targetLength; i++) {
        if (input[i] == 0) {
          leadingZeros++;
        } else {
          break;
        }
      }

      if (leadingZeros > 0 && input.length - leadingZeros == targetLength) {
        byte[] result = new byte[targetLength];
        System.arraycopy(input, leadingZeros, result, 0, targetLength);
        return result;
      } else {
        throw new SDJWTException("Cannot convert to target length: " + targetLength);
      }
    } else {

      byte[] result = new byte[targetLength];
      System.arraycopy(input, 0, result, targetLength - input.length, input.length);
      return result;
    }
  }
}
