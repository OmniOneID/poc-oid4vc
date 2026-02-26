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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.PrivateKey;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.CompactSigner;
import org.omnione.did.oid4vc.oid4vp.util.jar.jws.JWSSigner;

@Slf4j
public class ECDSASigner implements JWSSigner {

  private final PrivateKey privateKey;
  private final String keyId;
  private final CompactSigner compactSigner;

  public ECDSASigner(PrivateKey privateKey) {
    this.privateKey = privateKey;
    this.keyId = null;
    this.compactSigner = null;
  }

  public ECDSASigner(CompactSigner compactSigner, String keyId) {
    if (compactSigner == null) {
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_KEY, "CompactSigner cannot be null"));
    }
    if (keyId == null || keyId.trim().isEmpty()) {
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_KEY, "Key ID cannot be null or empty"));
    }

    this.privateKey = null;
    this.keyId = keyId;
    this.compactSigner = compactSigner;
  }

  @Override
  public byte[] sign(String signingInput) throws OID4VPException {
    if (compactSigner != null) {
      return signWithSigner(signingInput);
    } else {
      return signWithPrivateKey(signingInput);
    }
  }


  private byte[] signWithSigner(String signingInput) throws OID4VPException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedInput = digest.digest(signingInput.getBytes(StandardCharsets.UTF_8));

      byte[] signature = this.compactSigner.sign(this.keyId, hashedInput);

      log.info("=== Signer Signature Debug ===");
      log.info("Signature length: {}", signature.length);

      if (signature.length == 64) {
        log.info("Already in IEEE P1363 format (64 bytes)");
        return signature;
      } else if (signature.length == 65 && signature[0] == 0x20) {
        log.info("Detected 65-byte signature with 0x20 prefix - extracting 64-byte P1363");
        byte[] p1363Signature = new byte[64];
        System.arraycopy(signature, 1, p1363Signature, 0, 64);
        return p1363Signature;
      } else if (signature.length > 6 && signature[0] == 0x30) {
        log.info("Converting from DER format to IEEE P1363");
        return convertDERToP1363(signature);
      } else if (signature.length == 65 && (signature[0] & 0xFF) == 0x1f) {
        byte[] trimmed = new byte[64];
        System.arraycopy(signature, 1, trimmed, 0, 64);
        return trimmed;
      } else {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Unsupported signature format. Length: " + signature.length);
      }

    } catch (OID4VPException e) {
      throw e;
    } catch (NoSuchAlgorithmException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE, "SHA-256", e);
    } catch (Exception e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_SIGN_FAILED, e.getMessage(), e);
    }
  }

  private byte[] signWithPrivateKey(String signingInput) throws OID4VPException {
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
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_KEY, e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE, "SHA256withECDSA", e);
    } catch (SignatureException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_SIGN_FAILED, e.getMessage(), e);
    }
  }

  private byte[] convertDERToP1363(byte[] derSignature) throws OID4VPException {
    try {

      if (derSignature.length < 6 || derSignature[0] != 0x30) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Invalid DER signature format");
      }

      int sequenceLength = derSignature[1] & 0xFF;
      int offset = 2;

      if ((sequenceLength & 0x80) != 0) {
        int lengthBytes = sequenceLength & 0x7F;
        if (lengthBytes > 4 || offset + lengthBytes >= derSignature.length) {
          throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Invalid DER length encoding");
        }

        sequenceLength = 0;
        for (int i = 0; i < lengthBytes; i++) {
          sequenceLength = (sequenceLength << 8) | (derSignature[offset++] & 0xFF);
        }
      }

      if (offset >= derSignature.length || derSignature[offset] != 0x02) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Expected INTEGER tag for r");
      }
      offset++;

      int rLength = derSignature[offset++] & 0xFF;
      if (offset + rLength > derSignature.length) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Invalid r length");
      }

      byte[] rBytes = new byte[rLength];
      System.arraycopy(derSignature, offset, rBytes, 0, rLength);
      offset += rLength;

      if (offset >= derSignature.length || derSignature[offset] != 0x02) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Expected INTEGER tag for s");
      }
      offset++;

      int sLength = derSignature[offset++] & 0xFF;
      if (offset + sLength > derSignature.length) {
        throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Invalid s length");
      }

      byte[] sBytes = new byte[sLength];
      System.arraycopy(derSignature, offset, sBytes, 0, sLength);

      byte[] r = toFixedLength(rBytes, 32);
      byte[] s = toFixedLength(sBytes, 32);

      byte[] p1363Signature = new byte[64];
      System.arraycopy(r, 0, p1363Signature, 0, 32);
      System.arraycopy(s, 0, p1363Signature, 32, 32);

      return p1363Signature;

    } catch (OID4VPException e) {
      throw e;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT, "Invalid DER signature structure", e);
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
        throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_FORMAT,
            "Cannot convert to target length: " + targetLength));
      }
    } else {

      byte[] result = new byte[targetLength];
      System.arraycopy(input, 0, result, targetLength - input.length, input.length);
      return result;
    }
  }
}
