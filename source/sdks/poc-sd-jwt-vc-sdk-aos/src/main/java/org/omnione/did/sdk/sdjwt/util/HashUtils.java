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

package org.omnione.did.sdk.sdjwt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;

public class HashUtils {

  public static final String DEFAULT_HASH_ALGORITHM = "sha-256";

  private static final Set<String> SUPPORTED_ALGORITHMS = Set.of(
      "sha-1",
      "sha-256",
      "sha-384",
      "sha-512",
      "sha3-256",
      "sha3-384",
      "sha3-512"
  );

  private static final java.util.Map<String, String> ALGORITHM_MAPPING = java.util.Map.of(
      "sha-1", "SHA-1",
      "sha-256", "SHA-256",
      "sha-384", "SHA-384",
      "sha-512", "SHA-512",
      "sha3-256", "SHA3-256",
      "sha3-384", "SHA3-384",
      "sha3-512", "SHA3-512"
  );

  private HashUtils() {
    throw new SDJWTException("HashUtils is a utility class and cannot be instantiated");
  }

  public static String getDefaultHashAlgorithm() {
    return DEFAULT_HASH_ALGORITHM;
  }

  public static boolean isSupportedHashAlgorithm(String algorithm) {
    if (algorithm == null) {
      return false;
    }
    return SUPPORTED_ALGORITHMS.contains(algorithm.toLowerCase());
  }

  public static Set<String> getSupportedAlgorithms() {
    return SUPPORTED_ALGORITHMS;
  }

  public static String computeDigest(String input, String algorithm) {
    if (input == null) {
      throw new SDJWTException("Input cannot be null");
    }
    if (!isSupportedHashAlgorithm(algorithm)) {
      throw new SDJWTException("Unsupported hash algorithm: " + algorithm);
    }

    try {
      String javaAlgorithm = ALGORITHM_MAPPING.get(algorithm.toLowerCase());
      MessageDigest digest = MessageDigest.getInstance(javaAlgorithm);

      byte[] inputBytes = input.getBytes(StandardCharsets.US_ASCII);
      byte[] hashBytes = digest.digest(inputBytes);

      return Base64UrlUtils.encode(hashBytes);

    } catch (NoSuchAlgorithmException e) {
      throw new SDJWTException("Hash algorithm not available: " + algorithm, e);
    }
  }

  public static String computeDigest(String input) {
    return computeDigest(input, DEFAULT_HASH_ALGORITHM);
  }

  public static String[] computeDigests(String[] inputs, String algorithm) {
    if (inputs == null) {
      throw new SDJWTException("Inputs cannot be null");
    }

    String[] digests = new String[inputs.length];
    for (int i = 0; i < inputs.length; i++) {
      digests[i] = computeDigest(inputs[i], algorithm);
    }
    return digests;
  }

  public static String[] computeDigests(String[] inputs) {
    return computeDigests(inputs, DEFAULT_HASH_ALGORITHM);
  }

  public static boolean verifyDigest(String input, String expectedDigest, String algorithm) {
    if (expectedDigest == null) {
      throw new SDJWTException("Expected digest cannot be null");
    }

    String computedDigest = computeDigest(input, algorithm);
    return computedDigest.equals(expectedDigest);
  }

  public static boolean verifyDigest(String input, String expectedDigest) {
    return verifyDigest(input, expectedDigest, DEFAULT_HASH_ALGORITHM);
  }

  public static int getHashLength(String algorithm) {
    if (!isSupportedHashAlgorithm(algorithm)) {
      throw new SDJWTException("Unsupported hash algorithm: " + algorithm);
    }

    switch (algorithm.toLowerCase()) {
      case "sha-1": return 20;
      case "sha-256": return 32;
      case "sha-384": return 48;
      case "sha-512": return 64;
      case "sha3-256": return 32;
      case "sha3-384": return 48;
      case "sha3-512": return 64;
      default: throw new SDJWTException("Unknown algorithm: " + algorithm);
    }
  }

  public static int getSecurityStrength(String algorithm) {
    if (!isSupportedHashAlgorithm(algorithm)) {
      throw new SDJWTException("Unsupported hash algorithm: " + algorithm);
    }

    switch (algorithm.toLowerCase()) {
      case "sha-1": return 80;
      case "sha-256": return 128;
      case "sha-384": return 192;
      case "sha-512": return 256;
      case "sha3-256": return 128;
      case "sha3-384": return 192;
      case "sha3-512": return 256;
      default: throw new SDJWTException("Unknown algorithm: " + algorithm);
    }
  }

  public static String getRecommendedAlgorithm(int securityLevel) {
    switch (securityLevel) {
      case 80: return "sha-1";
      case 128: return "sha-256";
      case 192: return "sha-384";
      case 256: return "sha-512";
      default: throw new SDJWTException(
          "Unsupported security level: " + securityLevel +
              ". Supported levels: 80, 128, 192, 256");
    }
  }

  public static boolean isSecureAlgorithm(String algorithm) {
    if (!isSupportedHashAlgorithm(algorithm)) {
      return false;
    }

    return !algorithm.toLowerCase().equals("sha-1");
  }

  public static void validateSecureAlgorithm(String algorithm) {
    if (!isSupportedHashAlgorithm(algorithm)) {
      throw new SDJWTException("Unsupported hash algorithm: " + algorithm);
    }
    if (!isSecureAlgorithm(algorithm)) {
      throw new SDJWTException("Hash algorithm is not considered secure: " + algorithm);
    }
  }

}
