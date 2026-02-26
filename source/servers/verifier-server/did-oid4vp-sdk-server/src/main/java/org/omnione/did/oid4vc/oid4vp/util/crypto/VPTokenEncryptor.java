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

package org.omnione.did.oid4vc.oid4vp.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.service.VerifierConfigService;
import org.springframework.stereotype.Component;

/**
 * VP Token encryption utility using AES-256-GCM.
 */
@Slf4j
@Component
public class VPTokenEncryptor {

  private static final String AES_ALGORITHM = "AES";
  private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final int AES_KEY_LENGTH = 32;

  private final SecretKey aesKey;

  /**
   * Creates a VPTokenEncryptor with encryption key from configuration.
   *
   * @param configService the verifier configuration service
   */
  public VPTokenEncryptor(VerifierConfigService configService) {
    String encryptionKeyBase64 = configService.getOID4VPConfig().getCrypto().getVpTokenEncryptionKey();
    
    if (encryptionKeyBase64 == null || encryptionKeyBase64.isEmpty()) {
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_KEY_NOT_CONFIGURED));
    }

    try {
      byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
      
      if (keyBytes.length != AES_KEY_LENGTH) {
        throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_INVALID_KEY_LENGTH,
            "Expected " + AES_KEY_LENGTH + " bytes, but got " + keyBytes.length));
      }
      
      this.aesKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
      log.debug("VPTokenEncryptor initialized successfully");
    } catch (RuntimeException e) {
      throw e;
    }
  }

  /**
   * Encrypts the given plain text using AES-256-GCM.
   *
   * @param plainText the plain text to encrypt
   * @return Base64-encoded string containing IV + ciphertext
   * @throws RuntimeException if encryption fails
   */
  public String encrypt(String plainText) {
    if (plainText == null || plainText.isEmpty()) {
      return null;
    }

    try {
      byte[] iv = generateRandomIV();

      Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      byte[] encryptedData = concat(iv, cipherText);

      return Base64.getEncoder().encodeToString(encryptedData);
    } catch (GeneralSecurityException e) {
      log.error("Failed to encrypt VP Token", e);
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_ENCRYPT_FAILED, e));
    }
  }

  /**
   * Decrypts the given Base64-encoded encrypted data using AES-256-GCM.
   *
   * @param encryptedBase64 Base64-encoded string containing IV + ciphertext
   * @return the decrypted plain text
   * @throws RuntimeException if decryption fails
   */
  public String decrypt(String encryptedBase64) {
    if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
      return null;
    }

    try {
      byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);

      byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);
      byte[] cipherText = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

      Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);

      byte[] plainTextBytes = cipher.doFinal(cipherText);

      return new String(plainTextBytes, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      log.error("Failed to decode Base64 encrypted data", e);
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_DECRYPT_FAILED, e));
    } catch (GeneralSecurityException e) {
      log.error("Failed to decrypt VP Token", e);
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_DECRYPT_FAILED, e));
    }
  }

  /**
   * Generates a random 32-byte AES key encoded in Base64.
   * Utility method for generating a new encryption key.
   *
   * @return Base64-encoded 32-byte AES key
   */
  public static String generateKey() {
    byte[] key = new byte[AES_KEY_LENGTH];
    new SecureRandom().nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }

  private byte[] generateRandomIV() {
    byte[] iv = new byte[GCM_IV_LENGTH];
    new SecureRandom().nextBytes(iv);
    return iv;
  }

  private byte[] concat(byte[] first, byte[] second) {
    byte[] result = new byte[first.length + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}