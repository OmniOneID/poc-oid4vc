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
import java.util.Base64;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;

public class Base64UrlUtils {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private Base64UrlUtils() {
    throw new SDJWTException("Base64UrlUtils is a utility class and cannot be instantiated");
  }

  public static String encode(byte[] data) {
    if (data == null) {
      throw new SDJWTException("Data cannot be null");
    }
    return ENCODER.encodeToString(data);
  }

  public static String encode(String data) {
    if (data == null) {
      throw new SDJWTException("Data cannot be null");
    }
    return encode(data.getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] decode(String encoded) {
    if (encoded == null) {
      throw new SDJWTException("Encoded string cannot be null");
    }

    try {
      return DECODER.decode(encoded);
    } catch (IllegalArgumentException e) {
      throw new SDJWTException("Invalid Base64URL encoding: " + e.getMessage(), e);
    }
  }

  public static String decodeToString(String encoded) {
    byte[] decoded = decode(encoded);
    return new String(decoded, StandardCharsets.UTF_8);
  }

  public static boolean isValid(String encoded) {
    if (encoded == null || encoded.isEmpty()) {
      return false;
    }

    try {
      decode(encoded);
      return true;
    } catch (SDJWTException e) {
      return false;
    }
  }

  public static void validate(String encoded) {
    if (!isValid(encoded)) {
      throw new SDJWTException("Invalid Base64URL encoding: " + encoded);
    }
  }

}
