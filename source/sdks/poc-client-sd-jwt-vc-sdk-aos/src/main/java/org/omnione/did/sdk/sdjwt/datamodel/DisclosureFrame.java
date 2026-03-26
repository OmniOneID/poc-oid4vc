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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;

public class DisclosureFrame {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String SD_KEY = "_sd";

  private final List<String> sdFieldNames;
  private final Map<String, DisclosureFrame> nestedFrames;

  public DisclosureFrame() {
    this.sdFieldNames = new ArrayList<>();
    this.nestedFrames = new LinkedHashMap<>();
  }

  public DisclosureFrame(List<String> sdFieldNames) {
    this();
    if (sdFieldNames != null) {
      this.sdFieldNames.addAll(sdFieldNames);
    }
  }

  public DisclosureFrame addSdField(String fieldName) {
    if (fieldName == null || fieldName.trim().isEmpty()) {
      throw new SDJWTException("Field name cannot be null or empty");
    }
    if (SD_KEY.equals(fieldName)) {
      throw new SDJWTException("Field name '_sd' is reserved");
    }
    if (!sdFieldNames.contains(fieldName)) {
      sdFieldNames.add(fieldName);
    }
    return this;
  }

  public DisclosureFrame addSdFields(List<String> fieldNames) {
    if (fieldNames != null) {
      fieldNames.forEach(this::addSdField);
    }
    return this;
  }

  public DisclosureFrame addNestedFrame(String nestedFieldName, DisclosureFrame nestedFrame) {
    if (nestedFieldName == null || nestedFieldName.trim().isEmpty()) {
      throw new SDJWTException("Nested field name cannot be null or empty");
    }
    if (SD_KEY.equals(nestedFieldName)) {
      throw new SDJWTException("Field name '_sd' is reserved");
    }
    if (nestedFrame == null) {
      throw new SDJWTException("Nested frame cannot be null");
    }
    nestedFrames.put(nestedFieldName, nestedFrame);
    return this;
  }

  public List<String> getSdFieldNames() {
    return Collections.unmodifiableList(sdFieldNames);
  }

  public Map<String, DisclosureFrame> getNestedFrames() {
    return Collections.unmodifiableMap(nestedFrames);
  }

  public boolean hasSelectiveDisclosures() {
    return !sdFieldNames.isEmpty() || !nestedFrames.isEmpty();
  }

  public boolean isSdField(String fieldName) {
    return sdFieldNames.contains(fieldName);
  }

  public boolean hasNestedFrame(String fieldName) {
    return nestedFrames.containsKey(fieldName);
  }

  public DisclosureFrame getNestedFrame(String fieldName) {
    return nestedFrames.get(fieldName);
  }

  public static DisclosureFrame fromMap(Map<String, Object> frameMap) {
    if (frameMap == null) {
      return new DisclosureFrame();
    }

    DisclosureFrame frame = new DisclosureFrame();

    for (Map.Entry<String, Object> entry : frameMap.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (SD_KEY.equals(key)) {
        if (!(value instanceof List)) {
          throw new SDJWTException("'_sd' must be a list");
        }
        @SuppressWarnings("unchecked")
        List<String> sdList = (List<String>) value;
        for (Object item : sdList) {
          if (!(item instanceof String)) {
            throw new SDJWTException("'_sd' list must contain only strings");
          }
          frame.addSdField((String) item);
        }
      } else {
        if (value instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> nestedMap = (Map<String, Object>) value;
          DisclosureFrame nestedFrame = fromMap(nestedMap);
          frame.addNestedFrame(key, nestedFrame);
        } else {
          throw new SDJWTException(
              "Nested values in disclosure frame must be objects (maps), got: " + key);
        }
      }
    }

    return frame;
  }

  public static DisclosureFrame fromJson(String jsonString) {
    if (jsonString == null || jsonString.trim().isEmpty()) {
      throw new SDJWTException("JSON string cannot be null or empty");
    }

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> frameMap = OBJECT_MAPPER.readValue(jsonString, Map.class);
      return fromMap(frameMap);
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to parse disclosure frame from JSON", e);
    }
  }

  public void validate(Map<String, Object> claims) {
    if (claims == null) {
      throw new SDJWTException("Claims cannot be null");
    }

    for (String sdField : sdFieldNames) {
      if (!claims.containsKey(sdField)) {
        throw new SDJWTException(
            "Disclosure frame references non-existent claim: " + sdField);
      }
    }

    for (Map.Entry<String, DisclosureFrame> nestedEntry : nestedFrames.entrySet()) {
      String nestedFieldName = nestedEntry.getKey();
      DisclosureFrame nestedFrame = nestedEntry.getValue();

      if (!claims.containsKey(nestedFieldName)) {
        throw new SDJWTException(
            "Disclosure frame references non-existent nested claim: " + nestedFieldName);
      }

      Object nestedValue = claims.get(nestedFieldName);
      if (!(nestedValue instanceof Map)) {
        throw new SDJWTException(
            "Claim '" + nestedFieldName + "' is not an object, cannot apply nested frame");
      }

      Map<String, Object> nestedClaims = (Map<String, Object>) nestedValue;
      nestedFrame.validate(nestedClaims);
    }
  }

  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();

    if (!sdFieldNames.isEmpty()) {
      result.put(SD_KEY, new ArrayList<>(sdFieldNames));
    }

    for (Map.Entry<String, DisclosureFrame> nestedEntry : nestedFrames.entrySet()) {
      result.put(nestedEntry.getKey(), nestedEntry.getValue().toMap());
    }

    return result;
  }

  public String toJson() {
    try {
      return OBJECT_MAPPER.writeValueAsString(toMap());
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to serialize disclosure frame to JSON", e);
    }
  }

  @Override
  public String toString() {
    return "DisclosureFrame{" +
        "sdFieldNames=" + sdFieldNames +
        ", nestedFrames=" + nestedFrames.keySet() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DisclosureFrame that = (DisclosureFrame) o;
    return Objects.equals(sdFieldNames, that.sdFieldNames) &&
        Objects.equals(nestedFrames, that.nestedFrames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sdFieldNames, nestedFrames);
  }
}
