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
import org.omnione.did.sdk.sdjwt.crypto.JWSVerifier;
import org.omnione.did.sdk.sdjwt.crypto.SignedJWT;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;

import java.security.Signature;
import java.security.interfaces.RSAPublicKey;

public class RSASSAVerifier implements JWSVerifier {

  private final RSAPublicKey publicKey;

  public RSASSAVerifier(RSAPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public boolean verify(SignedJWT signedJWT) throws SDJWTException {
    try {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initVerify(publicKey);
      signature.update(signedJWT.getSigningInput().getBytes());

      byte[] signatureBytes = signedJWT.getSignatureBytes();

      if (signatureBytes == null) {
        throw new SDJWTException("Signature bytes cannot be null");
      }

      int keySize = publicKey.getModulus().bitLength();
      int expectedSignatureLength = keySize / 8;

      if (signatureBytes.length == expectedSignatureLength) {

        return signature.verify(signatureBytes);
      } else if (signatureBytes.length < expectedSignatureLength) {

        byte[] paddedSignature = padToExpectedLength(signatureBytes, expectedSignatureLength);
        return signature.verify(paddedSignature);
      } else if (isValidDERSequence(signatureBytes)) {

        byte[] extractedSignature = extractSignatureFromDER(signatureBytes);

        if (extractedSignature != null && extractedSignature.length == expectedSignatureLength) {
          return signature.verify(extractedSignature);
        }
      }

      try {
        return signature.verify(signatureBytes);
      } catch (SDJWTException e) {
        throw e;
      } catch (SignatureException e) {
        return false;
      }
    } catch (SDJWTException e) {
      throw e;
    } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
      throw new SDJWTException("RSA signature verification failed", e);
    }
  }

  private byte[] padToExpectedLength(byte[] signatureBytes, int expectedLength) {
    if (signatureBytes == null) {
      throw new SDJWTException("Signature bytes cannot be null");
    }

    byte[] padded = new byte[expectedLength];
    int paddingLength = expectedLength - signatureBytes.length;

    for (int i = 0; i < paddingLength; i++) {
      padded[i] = 0;
    }

    System.arraycopy(signatureBytes, 0, padded, paddingLength, signatureBytes.length);

    return padded;
  }

  private boolean isValidDERSequence(byte[] bytes) {
    if (bytes == null || bytes.length < 2) {
      return false;
    }

    return bytes[0] == 0x30;
  }

  private byte[] extractSignatureFromDER(byte[] derBytes) {
    try {
      if (derBytes == null || derBytes.length < 4 || derBytes[0] != 0x30) {
        return derBytes;
      }

      int sequenceLength = derBytes[1] & 0xFF;
      if (sequenceLength >= 0x80) {

        int lengthBytes = sequenceLength & 0x7F;
        if (derBytes.length < 2 + lengthBytes) {
          return derBytes;
        }

        sequenceLength = 0;
        for (int i = 0; i < lengthBytes; i++) {
          sequenceLength = (sequenceLength << 8) | (derBytes[2 + i] & 0xFF);
        }

        int contentStart = 2 + lengthBytes;
        if (derBytes.length >= contentStart + sequenceLength) {
          byte[] content = new byte[sequenceLength];
          System.arraycopy(derBytes, contentStart, content, 0, sequenceLength);
          return content;
        }
      } else {

        if (derBytes.length >= 2 + sequenceLength) {
          byte[] content = new byte[sequenceLength];
          System.arraycopy(derBytes, 2, content, 0, sequenceLength);
          return content;
        }
      }
    } catch (SDJWTException e) {
      throw e;
    }

    return derBytes;
  }
}
