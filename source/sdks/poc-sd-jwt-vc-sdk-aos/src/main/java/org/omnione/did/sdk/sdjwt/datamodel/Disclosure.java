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

package org.omnione.did.sdk.sdjwt.datamodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;
import org.omnione.did.sdk.sdjwt.util.Base64UrlUtils;
import org.omnione.did.sdk.sdjwt.util.HashUtils;
import org.omnione.did.sdk.sdjwt.util.SaltGenerator;

public class Disclosure {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String salt;
  private final String claimName;
  private final Object claimValue;
  private String originalDisclosure;

  public Disclosure(String salt, String claimName, Object claimValue) {
    if (salt == null || salt.trim().isEmpty()) {
      throw new SDJWTException("Salt cannot be null or empty");
    }
    if (claimName == null || claimName.trim().isEmpty()) {
      throw new SDJWTException("Claim name cannot be null or empty for object property");
    }

    this.salt = salt;
    this.claimName = claimName;
    this.claimValue = claimValue;
  }

  public Disclosure(Object claimValue) {
    this.salt = SaltGenerator.generate();
    this.claimName = null;
    this.claimValue = claimValue;
  }

  public Disclosure(String salt, Object claimValue) {
    if (salt == null || salt.trim().isEmpty()) {
      throw new SDJWTException("Salt cannot be null or empty");
    }

    this.salt = salt;
    this.claimName = null;
    this.claimValue = claimValue;
  }

  public static Disclosure forObjectProperty(String claimName, Object claimValue) {
    if (claimName == null || claimName.trim().isEmpty()) {
      throw new SDJWTException("Claim name cannot be null or empty for object property");
    }

    String salt = SaltGenerator.generate();
    return new Disclosure(salt, claimName, claimValue);
  }

  public String getSalt() {
    return salt;
  }

  public String getClaimName() {
    return claimName;
  }

  public Object getClaimValue() {
    return claimValue;
  }

  public boolean isArrayElement() {
    return claimName == null;
  }

  public String getDisclosure() {
    try {
      ArrayNode array = OBJECT_MAPPER.createArrayNode();
      array.add(salt);

      if (claimName != null) {

        array.add(claimName);
      }

      array.addPOJO(claimValue);

      String json = OBJECT_MAPPER.writeValueAsString(array);
      byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

      return Base64UrlUtils.encode(jsonBytes);

    } catch (SDJWTException e) {
      throw e;
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to create disclosure", e);
    }
  }

  public String digest() {
    return digest(HashUtils.getDefaultHashAlgorithm());
  }

  public String digest(String hashAlgorithm) {
    if (!HashUtils.isSupportedHashAlgorithm(hashAlgorithm)) {
      throw new SDJWTException("Unsupported hash algorithm: " + hashAlgorithm);
    }

    String disclosure = (originalDisclosure != null) ? originalDisclosure : getDisclosure();
    return HashUtils.computeDigest(disclosure, hashAlgorithm);
  }

  public Map<String, Object> toArrayElement() {
    return toArrayElement(HashUtils.getDefaultHashAlgorithm());
  }

  public Map<String, Object> toArrayElement(String hashAlgorithm) {
    Map<String, Object> element = new HashMap<>();
    element.put("...", digest(hashAlgorithm));
    return element;
  }

  public static Disclosure parse(String disclosureString) {
    if (disclosureString == null || disclosureString.trim().isEmpty()) {
      throw new SDJWTException("Disclosure string cannot be null or empty");
    }

    try {
      byte[] decodedBytes = Base64UrlUtils.decode(disclosureString);
      String json = new String(decodedBytes, StandardCharsets.UTF_8);

      JsonNode arrayNode = OBJECT_MAPPER.readTree(json);

      if (!arrayNode.isArray()) {
        throw new SDJWTException("Disclosure must be a JSON array");
      }

      if (arrayNode.size() < 2 || arrayNode.size() > 3) {
        throw new SDJWTException("Disclosure array must have 2 or 3 elements");
      }

      String salt = arrayNode.get(0).asText();

      Disclosure disclosure;
      if (arrayNode.size() == 2) {

        Object claimValue = OBJECT_MAPPER.treeToValue(arrayNode.get(1), Object.class);
        disclosure = new Disclosure(salt, claimValue);
      } else {

        String claimName = arrayNode.get(1).asText();
        Object claimValue = OBJECT_MAPPER.treeToValue(arrayNode.get(2), Object.class);
        disclosure = new Disclosure(salt, claimName, claimValue);
      }
      disclosure.originalDisclosure = disclosureString;
      return disclosure;

    } catch (SDJWTException e) {
      throw e;
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to parse disclosure: " + e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return getDisclosure();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Disclosure that = (Disclosure) o;
    return Objects.equals(salt, that.salt) &&
        Objects.equals(claimName, that.claimName) &&
        Objects.equals(claimValue, that.claimValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(salt, claimName, claimValue);
  }
}
