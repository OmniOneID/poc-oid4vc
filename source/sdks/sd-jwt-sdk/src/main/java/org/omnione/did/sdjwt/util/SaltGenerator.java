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

package org.omnione.did.sdjwt.util;

import org.omnione.did.sdjwt.exception.SDJWTException;
import java.security.SecureRandom;
import java.util.Base64;

public class SaltGenerator {

  public static final int DEFAULT_SALT_LENGTH = 16;

  public static final int MIN_SALT_LENGTH = 8;

  public static final int MAX_SALT_LENGTH = 64;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private SaltGenerator() {
    throw new UnsupportedOperationException(
        "SaltGenerator is a utility class and cannot be instantiated");
  }

  public static String generate() {
    return generate(DEFAULT_SALT_LENGTH);
  }

  public static String generate(int lengthInBytes) {
    if (lengthInBytes < MIN_SALT_LENGTH) {
      throw new IllegalArgumentException(
          String.format("Salt length must be at least %d bytes, but was %d",
              MIN_SALT_LENGTH, lengthInBytes));
    }
    if (lengthInBytes > MAX_SALT_LENGTH) {
      throw new IllegalArgumentException(
          String.format("Salt length must be at most %d bytes, but was %d",
              MAX_SALT_LENGTH, lengthInBytes));
    }

    byte[] saltBytes = new byte[lengthInBytes];
    SECURE_RANDOM.nextBytes(saltBytes);
    return BASE64_URL_ENCODER.encodeToString(saltBytes);
  }

  public static String[] generateMultiple(int count) {
    return generateMultiple(count, DEFAULT_SALT_LENGTH);
  }

  public static String[] generateMultiple(int count, int lengthInBytes) {
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }

    String[] salts = new String[count];
    for (int i = 0; i < count; i++) {
      salts[i] = generate(lengthInBytes);
    }
    return salts;
  }

  public static boolean isValid(String salt) {
    if (salt == null || salt.isEmpty()) {
      return false;
    }

    try {

      byte[] decoded = Base64UrlUtils.decode(salt);

      return decoded.length >= MIN_SALT_LENGTH && decoded.length <= MAX_SALT_LENGTH;

    } catch (SDJWTException | IllegalArgumentException e) {
      return false;
    }
  }

  public static void validateSalt(String salt) {
    if (salt == null) {
      throw new IllegalArgumentException("Salt cannot be null");
    }
    if (salt.isEmpty()) {
      throw new IllegalArgumentException("Salt cannot be empty");
    }

    try {
      byte[] decoded = Base64UrlUtils.decode(salt);

      if (decoded.length < MIN_SALT_LENGTH) {
        throw new IllegalArgumentException(
            String.format("Salt is too short. Expected at least %d bytes, but got %d",
                MIN_SALT_LENGTH, decoded.length));
      }
      if (decoded.length > MAX_SALT_LENGTH) {
        throw new IllegalArgumentException(
            String.format("Salt is too long. Expected at most %d bytes, but got %d",
                MAX_SALT_LENGTH, decoded.length));
      }

    } catch (IllegalArgumentException e) {
      throw e;
    } catch (SDJWTException e) {
      throw new IllegalArgumentException("Salt is not valid base64url: " + e.getMessage(), e);
    }
  }
}