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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import org.omnione.did.sdjwt.exception.SDJWTException;
import java.util.Base64;
import java.util.Map;

public class SimpleJWTDecoder {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static class SimpleJWT {

    private final JsonNode header;
    private final JsonNode payload;
    private final String signature;

    public SimpleJWT(JsonNode header, JsonNode payload, String signature) {
      this.header = header;
      this.payload = payload;
      this.signature = signature;
    }

    public JsonNode getHeader() {
      return header;
    }

    public JsonNode getPayload() {
      return payload;
    }

    public String getSignature() {
      return signature;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPayloadAsMap() {
      try {
        return objectMapper.convertValue(payload, Map.class);
      } catch (SDJWTException e) {
        throw e;
      }
    }

    public Object getClaim(String claimName) {
      return payload.get(claimName);
    }

    public String getStringClaim(String claimName) {
      JsonNode claim = payload.get(claimName);
      return claim != null ? claim.asText() : null;
    }

    public Long getLongClaim(String claimName) {
      JsonNode claim = payload.get(claimName);
      return claim != null ? claim.asLong() : null;
    }

    @Override
    public String toString() {
      return "SimpleJWT{" +
          "header=" + header +
          ", payload=" + payload +
          ", signature='" + signature + '\'' +
          '}';
    }
  }

  public static SimpleJWT parse(String jwtString) {
    if (jwtString == null || jwtString.trim().isEmpty()) {
      throw new IllegalArgumentException("JWT string cannot be null or empty");
    }

    String[] parts = jwtString.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT format: expected 3 parts separated by dots");
    }

    try {

      String headerJson = base64UrlDecode(parts[0]);
      JsonNode header = objectMapper.readTree(headerJson);

      String payloadJson = base64UrlDecode(parts[1]);
      JsonNode payload = objectMapper.readTree(payloadJson);

      String signature = parts[2];

      return new SimpleJWT(header, payload, signature);

    } catch (SDJWTException e) {
      throw e;
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to parse JWT: " + e.getMessage(), e);
    }
  }

  private static String base64UrlDecode(String base64Url) {
    try {

      byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
      return new String(decoded, "UTF-8");
    } catch (SDJWTException e) {
      throw e;
    } catch (UnsupportedEncodingException e) {
      throw new SDJWTException("Failed to decode Base64URL: " + e.getMessage(), e);
    }
  }

  public static boolean isJWTFormat(String input) {
    if (input == null || input.trim().isEmpty()) {
      return false;
    }

    String[] parts = input.trim().split("\\.");
    if (parts.length != 3) {
      return false;
    }

    try {

      JsonNode header = objectMapper.readTree(base64UrlDecode(parts[0]));
      return header.has("alg") && header.has("typ");
    } catch (SDJWTException | IllegalArgumentException e) {
      return false;
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}