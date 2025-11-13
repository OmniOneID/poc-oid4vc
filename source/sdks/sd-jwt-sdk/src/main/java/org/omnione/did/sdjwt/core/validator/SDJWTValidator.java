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

package org.omnione.did.sdjwt.core.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.exception.SDJWTException;
import org.omnione.did.sdjwt.util.Base64UrlUtils;
import org.omnione.did.sdjwt.util.HashUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class SDJWTValidator {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static class ValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final Map<String, Object> metadata;

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings,
        Map<String, Object> metadata) {
      this.valid = valid;
      this.errors = new ArrayList<>(errors);
      this.warnings = new ArrayList<>(warnings);
      this.metadata = new LinkedHashMap<>(metadata);
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
      return Collections.unmodifiableList(warnings);
    }

    public Map<String, Object> getMetadata() {
      return Collections.unmodifiableMap(metadata);
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }
  }

  public ValidationResult validate(SDJWT sdjwt) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Map<String, Object> metadata = new LinkedHashMap<>();

    if (sdjwt == null) {
      errors.add("SD-JWT cannot be null");
      return new ValidationResult(false, errors, warnings, metadata);
    }

    validateCredentialJWT(sdjwt.getCredentialJwt(), errors, warnings, metadata);

    validateDisclosures(sdjwt.getDisclosures(), errors, warnings, metadata);

    if (sdjwt.hasKeyBindingJwt()) {
      validateKeyBindingJWT(sdjwt.getKeyBindingJwt(), errors, warnings, metadata);
    }

    crossValidateDisclosures(sdjwt, errors, warnings, metadata);

    metadata.put("credentialJwtPresent", true);
    metadata.put("disclosureCount", sdjwt.getDisclosureCount());
    metadata.put("keyBindingJwtPresent", sdjwt.hasKeyBindingJwt());

    boolean valid = errors.isEmpty();
    return new ValidationResult(valid, errors, warnings, metadata);
  }

  public ValidationResult validateDisclosure(Disclosure disclosure) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Map<String, Object> metadata = new LinkedHashMap<>();

    if (disclosure == null) {
      errors.add("Disclosure cannot be null");
      return new ValidationResult(false, errors, warnings, metadata);
    }

    if (disclosure.getSalt() == null || disclosure.getSalt().trim().isEmpty()) {
      errors.add("Disclosure salt cannot be null or empty");
    } else {
      if (!Base64UrlUtils.isValid(disclosure.getSalt())) {
        errors.add("Disclosure salt is not valid base64url");
      }
    }

    if (!disclosure.isArrayElement()) {
      if (disclosure.getClaimName() == null || disclosure.getClaimName().trim().isEmpty()) {
        errors.add("Object property disclosure must have a claim name");
      }
    }

    try {
      String disclosureString = disclosure.getDisclosure();
      metadata.put("disclosureString", disclosureString);
      metadata.put("disclosureLength", disclosureString.length());
    } catch (SDJWTException e) {
      errors.add("Failed to generate disclosure string: " + e.getMessage());
    }

    try {
      String digest = disclosure.digest();
      metadata.put("digest", digest);
    } catch (SDJWTException e) {
      errors.add("Failed to generate disclosure digest: " + e.getMessage());
    }

    metadata.put("isArrayElement", disclosure.isArrayElement());
    metadata.put("claimName", disclosure.getClaimName());
    metadata.put("saltLength", disclosure.getSalt() != null ? disclosure.getSalt().length() : 0);

    boolean valid = errors.isEmpty();
    return new ValidationResult(valid, errors, warnings, metadata);
  }

  private void validateCredentialJWT(String credentialJwt, List<String> errors,
      List<String> warnings, Map<String, Object> metadata) {
    if (credentialJwt == null || credentialJwt.trim().isEmpty()) {
      errors.add("Credential JWT cannot be null or empty");
      return;
    }

    String[] parts = credentialJwt.split("\\.");
    if (parts.length != 3) {
      errors.add("Credential JWT must have exactly 3 parts (header.payload.signature)");
      return;
    }

    try {
      String headerJson = Base64UrlUtils.decodeToString(parts[0]);
      JsonNode header = OBJECT_MAPPER.readTree(headerJson);

      if (!header.has("alg")) {
        errors.add("Credential JWT header missing 'alg' claim");
      }

      if (header.has("typ")) {
        String typ = header.get("alg").asText();
        if (!"vc+sd-jwt".equals(typ) && !"JWT".equals(typ)) {
          warnings.add("Credential JWT type is not 'vc+sd-jwt' or 'JWT': " + typ);
        }
      }

      metadata.put("credentialJwtAlgorithm", header.has("alg") ? header.get("alg").asText() : null);
      metadata.put("credentialJwtType", header.has("typ") ? header.get("typ").asText() : null);

    } catch (SDJWTException | IllegalArgumentException e) {
      errors.add("Failed to parse credential JWT header: " + e.getMessage());
      return;
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try {
      String payloadJson = Base64UrlUtils.decodeToString(parts[1]);
      JsonNode payload = OBJECT_MAPPER.readTree(payloadJson);

      if (payload.has("_sd_alg")) {
        String sdAlg = payload.get("_sd_alg").asText();
        if (!HashUtils.isSupportedHashAlgorithm(sdAlg)) {
          errors.add("Unsupported _sd_alg: " + sdAlg);
        }
        metadata.put("sdHashAlgorithm", sdAlg);
      } else {
        warnings.add("Credential JWT payload missing '_sd_alg' claim");
      }

      if (payload.has("_sd")) {
        JsonNode sdArray = payload.get("_sd");
        if (!sdArray.isArray()) {
          errors.add("_sd claim must be an array");
        } else {
          metadata.put("sdArraySize", sdArray.size());
        }
      }

      metadata.put("hasIssuer", payload.has("iss"));
      metadata.put("hasSubject", payload.has("sub"));
      metadata.put("hasAudience", payload.has("aud"));
      metadata.put("hasExpiration", payload.has("exp"));
      metadata.put("hasIssuedAt", payload.has("iat"));

    } catch (SDJWTException | IllegalArgumentException | JsonProcessingException e) {
      errors.add("Failed to parse credential JWT payload: " + e.getMessage());
    }
  }

  private void validateDisclosures(List<Disclosure> disclosures, List<String> errors,
      List<String> warnings, Map<String, Object> metadata) {
    if (disclosures == null) {
      warnings.add("Disclosures list is null");
      return;
    }

    int validDisclosures = 0;
    int arrayElementDisclosures = 0;
    int objectPropertyDisclosures = 0;
    Set<String> claimNames = new HashSet<>();

    for (int i = 0; i < disclosures.size(); i++) {
      Disclosure disclosure = disclosures.get(i);
      ValidationResult result = validateDisclosure(disclosure);

      if (result.isValid()) {
        validDisclosures++;

        if (disclosure.isArrayElement()) {
          arrayElementDisclosures++;
        } else {
          objectPropertyDisclosures++;
          String claimName = disclosure.getClaimName();
          if (claimNames.contains(claimName)) {
            warnings.add("Duplicate claim name in disclosures: " + claimName);
          }
          claimNames.add(claimName);
        }
      } else {
        for (String error : result.getErrors()) {
          errors.add("Disclosure " + i + ": " + error);
        }
      }
    }

    metadata.put("validDisclosures", validDisclosures);
    metadata.put("arrayElementDisclosures", arrayElementDisclosures);
    metadata.put("objectPropertyDisclosures", objectPropertyDisclosures);
    metadata.put("uniqueClaimNames", claimNames.size());
  }

  private void validateKeyBindingJWT(String keyBindingJwt, List<String> errors,
      List<String> warnings, Map<String, Object> metadata) {
    if (keyBindingJwt == null || keyBindingJwt.trim().isEmpty()) {
      warnings.add("Key binding JWT is empty");
      return;
    }

    String[] parts = keyBindingJwt.split("\\.");
    if (parts.length != 3) {
      errors.add("Key binding JWT must have exactly 3 parts");
      return;
    }

    try {

      String headerJson = Base64UrlUtils.decodeToString(parts[0]);
      JsonNode header = OBJECT_MAPPER.readTree(headerJson);

      if (!header.has("alg")) {
        errors.add("Key binding JWT header missing 'alg' claim");
      }

      if (header.has("typ")) {
        String typ = header.get("typ").asText();
        if (!"kb+jwt".equals(typ) && !"JWT".equals(typ)) {
          warnings.add("Key binding JWT type is not 'kb+jwt' or 'JWT': " + typ);
        }
      }

      String payloadJson = Base64UrlUtils.decodeToString(parts[1]);
      JsonNode payload = OBJECT_MAPPER.readTree(payloadJson);

      metadata.put("keyBindingJwtAlgorithm", header.has("alg") ? header.get("alg").asText() : null);
      metadata.put("keyBindingJwtHasAudience", payload.has("aud"));
      metadata.put("keyBindingJwtHasNonce", payload.has("nonce"));
      metadata.put("keyBindingJwtHasIssuedAt", payload.has("iat"));

    } catch (SDJWTException | IllegalArgumentException | JsonProcessingException e) {
      errors.add("Failed to parse key binding JWT: " + e.getMessage());
    }
  }

  private void crossValidateDisclosures(SDJWT sdjwt, List<String> errors, List<String> warnings,
      Map<String, Object> metadata) {
    try {

      String[] jwtParts = sdjwt.getCredentialJwt().split("\\.");
      String payloadJson = Base64UrlUtils.decodeToString(jwtParts[1]);
      JsonNode payload = OBJECT_MAPPER.readTree(payloadJson);

      if (!payload.has("_sd")) {
        if (!sdjwt.getDisclosures().isEmpty()) {
          warnings.add("SD-JWT has disclosures but credential JWT has no _sd array");
        }
        return;
      }

      JsonNode sdArray = payload.get("_sd");
      if (!sdArray.isArray()) {
        return;
      }

      String hashAlgorithm = HashUtils.getDefaultHashAlgorithm();
      if (payload.has("_sd_alg")) {
        hashAlgorithm = payload.get("_sd_alg").asText();
      }

      Set<String> disclosureDigests = new HashSet<>();
      for (Disclosure disclosure : sdjwt.getDisclosures()) {
        if (!disclosure.isArrayElement()) {
          disclosureDigests.add(disclosure.digest(hashAlgorithm));
        }
      }

      Set<String> sdArrayDigests = new HashSet<>();
      for (JsonNode digestNode : sdArray) {
        if (digestNode.isTextual()) {
          String digest = digestNode.asText();
          sdArrayDigests.add(digest);

          if (!disclosureDigests.contains(digest)) {
            warnings.add("_sd array contains digest not found in disclosures: " + digest);
          }
        }
      }

      for (String disclosureDigest : disclosureDigests) {
        if (!sdArrayDigests.contains(disclosureDigest)) {
          warnings.add("Disclosure digest not found in _sd array: " + disclosureDigest);
        }
      }

      metadata.put("sdArrayDigests", sdArrayDigests.size());
      metadata.put("disclosureDigests", disclosureDigests.size());
      metadata.put("digestsMatch", sdArrayDigests.equals(disclosureDigests));

    } catch (SDJWTException | IllegalArgumentException e) {
      warnings.add("Failed to cross-validate disclosures: " + e.getMessage());
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}