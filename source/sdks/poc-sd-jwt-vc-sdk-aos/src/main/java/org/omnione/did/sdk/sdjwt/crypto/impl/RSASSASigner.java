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

import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;

public class RSASSASigner implements JWSSigner {

  private final RSAPrivateKey privateKey;

  public RSASSASigner(RSAPrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  @Override
  public byte[] sign(String signingInput) throws SDJWTException {
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
    } catch (SDJWTException e) {
      throw e;
    } catch (NoSuchAlgorithmException e) {
      throw new SDJWTException("Failed to sign with RSA: " + e.getMessage(), e);
    } catch (InvalidKeyException e) {
      throw new SDJWTException("Invalid RSA private key: " + e.getMessage(), e);
    } catch (SignatureException e) {
      throw new SDJWTException("RSA signature operation failed: " + e.getMessage(), e);
    }
  }

  private byte[] extractRSASignature(byte[] signatureBytes, int expectedLength) {

    if (signatureBytes.length > expectedLength + 2 && signatureBytes[0] == 0x30) {

      try {
        int sequenceLength = signatureBytes[1] & 0xFF;
        int contentStart = 2;

        if ((sequenceLength & 0x80) != 0) {
          int lengthBytes = sequenceLength & 0x7F;
          sequenceLength = 0;
          for (int i = 0; i < lengthBytes; i++) {
            sequenceLength = (sequenceLength << 8) | (signatureBytes[contentStart++] & 0xFF);
          }
        }

        if (contentStart < signatureBytes.length && signatureBytes[contentStart] == 0x04) {

          contentStart++;
          int octetLength = signatureBytes[contentStart++] & 0xFF;
          if (octetLength == expectedLength
              && contentStart + octetLength <= signatureBytes.length) {
            byte[] extracted = new byte[expectedLength];
            System.arraycopy(signatureBytes, contentStart, extracted, 0, expectedLength);
            return extracted;
          }
        } else if (sequenceLength == expectedLength
            && contentStart + expectedLength <= signatureBytes.length) {

          byte[] extracted = new byte[expectedLength];
          System.arraycopy(signatureBytes, contentStart, extracted, 0, expectedLength);
          return extracted;
        }
      } catch (SDJWTException e) {

        throw e;
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
