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

package org.omnione.did.oid4vc.dcql.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.omnione.did.oid4vc.dcql.core.credential.CredentialAdapterRegistry;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;

public class DCQLQueryValidator {

  public static ValidationResult validate(DCQLQuery dcqlQuery) {
    ValidationResult result = new ValidationResult();

    if (dcqlQuery == null) {
      result.addError("DCQL query is null");
      return result;
    }

    validateBasicStructure(dcqlQuery, result);

    if (dcqlQuery.getCredentials() != null) {
      validateCredentials(dcqlQuery.getCredentials(), result);
    }

    if (dcqlQuery.getCredentialSets() != null) {
      validateCredentialSets(dcqlQuery.getCredentialSets(), dcqlQuery.getCredentials(), result);
    }

    validateConsistency(dcqlQuery, result);

    return result;
  }

  private static void validateBasicStructure(DCQLQuery dcqlQuery, ValidationResult result) {

    boolean hasCredentials =
        dcqlQuery.getCredentials() != null && !dcqlQuery.getCredentials().isEmpty();
    boolean hasCredentialSets =
        dcqlQuery.getCredentialSets() != null && !dcqlQuery.getCredentialSets().isEmpty();

    if (!hasCredentials && !hasCredentialSets) {
      result.addError("DCQL query must have either 'credentials' or 'credential_sets'");
    }

    if (hasCredentials && hasCredentialSets) {
      result.addWarning(
          "DCQL query has both 'credentials' and 'credential_sets' - credential_sets takes precedence");
    }
  }

  private static void validateCredentials(List<DCQLQuery.CredentialQuery> credentials,
      ValidationResult result) {
    if (credentials.isEmpty()) {
      result.addError("'credentials' array cannot be empty");
      return;
    }

    Set<String> credentialIds = new HashSet<>();

    for (int i = 0; i < credentials.size(); i++) {
      DCQLQuery.CredentialQuery credential = credentials.get(i);
      String context = "credentials[" + i + "]";

      validateCredential(credential, context, result);

      if (credential.getId() != null) {
        if (credentialIds.contains(credential.getId())) {
          result.addError("Duplicate credential ID: " + credential.getId());
        } else {
          credentialIds.add(credential.getId());
        }
      }
    }
  }

  private static void validateCredential(DCQLQuery.CredentialQuery credential, String context,
      ValidationResult result) {
    if (credential == null) {
      result.addError(context + " is null");
      return;
    }

    validateRequiredField(credential.getId(), "id", context, result);
    validateRequiredField(credential.getFormat(), "format", context, result);

    if (credential.getId() != null) {
      validateCredentialId(credential.getId(), context, result);
    }

    if (credential.getFormat() != null) {
      validateFormat(credential.getFormat(), context, result);
    }

    if (credential.getClaims() != null) {
      validateClaims(credential.getClaims(), context, result);
    }

    if (credential.getClaimSets() != null) {
      validateCredentialClaimSets(credential.getClaimSets(), context, result);

      if (credential.getClaims() != null) {
        result.addWarning(
            context + " has both 'claims' and 'claim_sets' - claim_sets takes precedence");
      }
    }

    if (credential.getMeta() != null) {
      validateMeta(credential.getMeta(), context, result);
    }
  }

  private static void validateClaims(List<DCQLQuery.ClaimQuery> claims, String context,
      ValidationResult result) {
    for (int i = 0; i < claims.size(); i++) {
      DCQLQuery.ClaimQuery claim = claims.get(i);
      String claimContext = context + ".claims[" + i + "]";

      if (claim == null) {
        result.addError(claimContext + " is null");
        continue;
      }

      if (claim.getPath() == null || claim.getPath().isEmpty()) {
        result.addError(claimContext + ".path is required and cannot be empty");
      } else {
        validatePath(claim.getPath(), claimContext + ".path", result);
      }

      if (claim.getValues() != null) {
        validateValues(claim.getValues(), claimContext + ".values", result);
      }
    }
  }

  private static void validatePath(List<Object> path, String context, ValidationResult result) {
    if (path == null) {
      result.addError(context + " cannot be null");
      return;
    }

    if (!DCQLPathProcessor.isValidPath(path)) {
      result.addError(context + " contains invalid elements");
    }

    for (int i = 0; i < path.size(); i++) {
      Object element = path.get(i);
      if (element == null) {

        continue;
      } else if (element instanceof String) {
        String strElement = (String) element;
        if (strElement.trim().isEmpty()) {
          result.addError(context + "[" + i + "] cannot be empty string");
        }
      } else if (!(element instanceof Integer)) {
        result.addError(context + "[" + i + "] must be string, integer, or null");
      }
    }
  }

  private static void validateValues(List<Object> values, String context, ValidationResult result) {
    if (values.isEmpty()) {
      result.addWarning(context + " is empty - no value restrictions will be applied");
    }

    Set<Class<?>> valueTypes = new HashSet<>();
    for (Object value : values) {
      if (value != null) {
        valueTypes.add(value.getClass());
      }
    }

    if (valueTypes.size() > 1) {
      result.addWarning(context + " contains mixed value types - may cause matching issues");
    }
  }

  private static void validateCredentialSets(List<DCQLQuery.CredentialSet> credentialSets,
      List<DCQLQuery.CredentialQuery> credentials,
      ValidationResult result) {
    if (credentialSets.isEmpty()) {
      result.addError("'credential_sets' array cannot be empty");
      return;
    }

    Set<String> availableCredentialIds = new HashSet<>();
    if (credentials != null) {
      credentials.stream()
          .map(DCQLQuery.CredentialQuery::getId)
          .filter(Objects::nonNull)
          .forEach(availableCredentialIds::add);
    }

    for (int i = 0; i < credentialSets.size(); i++) {
      DCQLQuery.CredentialSet credentialSet = credentialSets.get(i);
      String context = "credential_sets[" + i + "]";

      validateCredentialSet(credentialSet, context, availableCredentialIds, result);
    }
  }

  private static void validateCredentialSet(DCQLQuery.CredentialSet credentialSet,
      String context,
      Set<String> availableCredentialIds,
      ValidationResult result) {
    if (credentialSet == null) {
      result.addError(context + " is null");
      return;
    }

    if (credentialSet.getOptions() == null || credentialSet.getOptions().isEmpty()) {
      result.addError(context + ".options is required and cannot be empty");
      return;
    }

    for (int i = 0; i < credentialSet.getOptions().size(); i++) {
      List<String> option = credentialSet.getOptions().get(i);
      String optionContext = context + ".options[" + i + "]";

      if (option == null || option.isEmpty()) {
        result.addError(optionContext + " cannot be null or empty");
        continue;
      }

      for (String credentialId : option) {
        if (!availableCredentialIds.contains(credentialId)) {
          result.addError(optionContext + " references undefined credential ID: " + credentialId);
        }
      }
    }
  }

  private static void validateConsistency(DCQLQuery dcqlQuery, ValidationResult result) {

  }

  private static void validateRequiredField(String value, String fieldName, String context,
      ValidationResult result) {
    if (value == null || value.trim().isEmpty()) {
      result.addError(context + "." + fieldName + " is required");
    }
  }

  private static void validateCredentialId(String id, String context, ValidationResult result) {

    if (!id.matches("^[a-zA-Z0-9_-]+$")) {
      result.addError(
          context + ".id must contain only alphanumeric characters, underscores, and hyphens");
    }
  }

  private static void validateFormat(String format, String context, ValidationResult result) {

    Set<String> supportedFormats = CredentialAdapterRegistry.getInstance().getAllSupportedFormats();

    // Also accept common formats that may be added later
    Set<String> knownFormats = new HashSet<>(supportedFormats);
    knownFormats.addAll(Set.of("jwt_vc_json", "jwt_vc", "ldp_vc", "mso_mdoc"));

    if (!knownFormats.contains(format)) {
      result.addWarning(context + ".format '" + format + "' may not be supported");
    }
  }

  private static void validateMeta(Map<String, Object> meta, String context,
      ValidationResult result) {

    if (meta.isEmpty()) {
      result.addWarning(context + ".meta is empty");
    }

    if (meta.containsKey("vct_values")) {
      Object vctValues = meta.get("vct_values");
      if (!(vctValues instanceof List)) {
        result.addError(context + ".meta.vct_values must be an array");
      }
    }
  }

  private static void validateCredentialClaimSets(List<DCQLQuery.ClaimSet> claimSets,
      String context, ValidationResult result) {

    if (claimSets.isEmpty()) {
      result.addError(context + ".claim_sets cannot be empty");
    }

    for (int i = 0; i < claimSets.size(); i++) {
      DCQLQuery.ClaimSet claimSet = claimSets.get(i);
      if (claimSet == null) {
        result.addError(context + ".claim_sets[" + i + "] cannot be null");
        continue;
      }

      if (claimSet.getClaims() == null || claimSet.getClaims().isEmpty()) {
        result.addError(context + ".claim_sets[" + i + "].claims cannot be null or empty");
      }
    }
  }

  public static class ValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String error) {
      errors.add(error);
    }

    public void addWarning(String warning) {
      warnings.add(warning);
    }

    public List<String> getErrors() {
      return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
      return Collections.unmodifiableList(warnings);
    }

    public boolean isValid() {
      return errors.isEmpty();
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public String getSummary() {
      return String.format("Validation: %d errors, %d warnings", errors.size(), warnings.size());
    }
  }
}