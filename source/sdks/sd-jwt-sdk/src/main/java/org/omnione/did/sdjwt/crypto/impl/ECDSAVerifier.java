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

package org.omnione.did.sdjwt.crypto.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import org.omnione.did.sdjwt.crypto.JWSVerifier;
import org.omnione.did.sdjwt.crypto.SignedJWT;
import org.omnione.did.sdjwt.exception.SDJWTException;

import java.security.Signature;
import java.security.interfaces.ECPublicKey;

public class ECDSAVerifier implements JWSVerifier {

  private final ECPublicKey publicKey;

  public ECDSAVerifier(ECPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public boolean verify(SignedJWT signedJWT) throws SDJWTException {
    try {
      Signature signature = Signature.getInstance("SHA256withECDSA");
      signature.initVerify(publicKey);
      signature.update(signedJWT.getSigningInput().getBytes());

      byte[] signatureBytes = signedJWT.getSignatureBytes();

      if (signatureBytes == null) {
        throw new SDJWTException("Signature bytes cannot be null");
      }

      if (signatureBytes.length == 64) {

        byte[] derSignature = convertP1363ToDER(signatureBytes);
        return signature.verify(derSignature);
      } else {

        return signature.verify(signatureBytes);
      }
    } catch (SDJWTException e) {
      throw e;
    } catch (SignatureException e) {
      throw new SDJWTException(e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] convertP1363ToDER(byte[] p1363Signature) {
    if (p1363Signature.length != 64) {
      throw new IllegalArgumentException("IEEE P1363 signature must be 64 bytes");
    }

    byte[] r = new byte[32];
    byte[] s = new byte[32];
    System.arraycopy(p1363Signature, 0, r, 0, 32);
    System.arraycopy(p1363Signature, 32, s, 0, 32);

    r = removeLeadingZeros(r);
    s = removeLeadingZeros(s);

    if ((r[0] & 0x80) != 0) {
      byte[] temp = new byte[r.length + 1];
      temp[0] = 0;
      System.arraycopy(r, 0, temp, 1, r.length);
      r = temp;
    }
    if ((s[0] & 0x80) != 0) {
      byte[] temp = new byte[s.length + 1];
      temp[0] = 0;
      System.arraycopy(s, 0, temp, 1, s.length);
      s = temp;
    }

    int rTotalLength = 2 + r.length;
    int sTotalLength = 2 + s.length;
    int sequenceContentLength = rTotalLength + sTotalLength;
    int totalLength = 2 + sequenceContentLength;

    byte[] derSignature = new byte[totalLength];
    int offset = 0;

    derSignature[offset++] = 0x30;
    derSignature[offset++] = (byte) sequenceContentLength;

    derSignature[offset++] = 0x02;
    derSignature[offset++] = (byte) r.length;
    System.arraycopy(r, 0, derSignature, offset, r.length);
    offset += r.length;

    derSignature[offset++] = 0x02;
    derSignature[offset++] = (byte) s.length;
    System.arraycopy(s, 0, derSignature, offset, s.length);

    return derSignature;
  }

  private byte[] removeLeadingZeros(byte[] bytes) {
    int leadingZeros = 0;
    for (int i = 0; i < bytes.length - 1; i++) {
      if (bytes[i] == 0) {
        leadingZeros++;
      } else {
        break;
      }
    }

    if (leadingZeros == 0) {
      return bytes;
    }

    byte[] result = new byte[bytes.length - leadingZeros];
    System.arraycopy(bytes, leadingZeros, result, 0, result.length);
    return result;
  }
}