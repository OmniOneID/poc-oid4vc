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
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.sdjwt.crypto.JWSSigner;
import org.omnione.did.sdjwt.exception.SDJWTException;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.omnione.did.wallet.exception.WalletException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;

@Slf4j
public class ECDSASigner implements JWSSigner {

  private final ECPrivateKey privateKey;
  private final WalletManagerInterface walletManager;
  private final String keyId;

  public ECDSASigner(ECPrivateKey privateKey) {
    this.privateKey = privateKey;
    this.walletManager = null;
    this.keyId = null;
  }

  public ECDSASigner(WalletManagerInterface walletManager, String keyId) {
    if (walletManager == null) {
      throw new IllegalArgumentException("Wallet manager cannot be null");
    }
    if (keyId == null || keyId.trim().isEmpty()) {
      throw new IllegalArgumentException("Key ID cannot be null or empty");
    }

    this.privateKey = null;
    this.walletManager = walletManager;
    this.keyId = keyId;
  }

  @Override
  public byte[] sign(String signingInput) throws SDJWTException {
    if (walletManager != null) {

      return signWithWalletManager(signingInput);
    } else {

      return signWithPrivateKey(signingInput);
    }
  }

  private byte[] signWithWalletManager(String signingInput) throws SDJWTException {
    try {

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedInput = digest.digest(signingInput.getBytes(StandardCharsets.UTF_8));

      byte[] signature = walletManager.generateCompactSignatureFromHash(keyId, hashedInput);

      log.info("=== WalletManager Signature Debug ===");
      log.info("Signature length: {}", signature.length);
      log.info("First 10 bytes: {}", java.util.Arrays.toString(
          java.util.Arrays.copyOf(signature, Math.min(10, signature.length))));
      if (signature.length > 0) {
        log.info("First byte (hex): 0x{}", String.format("%02x", signature[0]));
      }

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
        log.info("Unknown signature format - trying as-is");
        throw new SDJWTException("Unsupported signature format. Length: " + signature.length +
            ", First byte: 0x" + String.format("%02x", signature[0]));
      }

    } catch (SDJWTException e) {
      throw e;
    } catch (WalletException e) {
      throw new SDJWTException("Failed to sign with wallet manager", e);
    } catch (NoSuchAlgorithmException e) {
      throw new SDJWTException("Failed to sign with wallet manager", e);
    }
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
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] convertDERToP1363(byte[] derSignature) throws SDJWTException {
    try {

      if (derSignature.length < 6 || derSignature[0] != 0x30) {
        throw new IllegalArgumentException("Invalid DER signature format");
      }

      int sequenceLength = derSignature[1] & 0xFF;
      int offset = 2;

      if ((sequenceLength & 0x80) != 0) {
        int lengthBytes = sequenceLength & 0x7F;
        if (lengthBytes > 4 || offset + lengthBytes >= derSignature.length) {
          throw new IllegalArgumentException("Invalid DER length encoding");
        }

        sequenceLength = 0;
        for (int i = 0; i < lengthBytes; i++) {
          sequenceLength = (sequenceLength << 8) | (derSignature[offset++] & 0xFF);
        }
      }

      if (offset >= derSignature.length || derSignature[offset] != 0x02) {
        throw new IllegalArgumentException("Expected INTEGER tag for r");
      }
      offset++;

      int rLength = derSignature[offset++] & 0xFF;
      if (offset + rLength > derSignature.length) {
        throw new IllegalArgumentException("Invalid r length");
      }

      byte[] rBytes = new byte[rLength];
      System.arraycopy(derSignature, offset, rBytes, 0, rLength);
      offset += rLength;

      if (offset >= derSignature.length || derSignature[offset] != 0x02) {
        throw new IllegalArgumentException("Expected INTEGER tag for s");
      }
      offset++;

      int sLength = derSignature[offset++] & 0xFF;
      if (offset + sLength > derSignature.length) {
        throw new IllegalArgumentException("Invalid s length");
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
        throw new IllegalArgumentException("Cannot convert to target length: " + targetLength);
      }
    } else {

      byte[] result = new byte[targetLength];
      System.arraycopy(input, 0, result, targetLength - input.length, input.length);
      return result;
    }
  }
}
