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

package org.omnione.did.oid4vc.oid4vp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.DCQLScopeMappingRepository;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLScopeMappingDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeToDCQLMapperService {

  private final ObjectMapper objectMapper;
  private final DCQLScopeMappingRepository dcqlScopeMappingRepository;
  private final AtomicReference<Map<String, DCQLQuery>> cachedMappings = new AtomicReference<>(new ConcurrentHashMap<>());

  @PostConstruct
  public void init() {
    loadFromDatabase();
  }

  /**
   * Get scope mappings (cached).
   */
  public Map<String, DCQLQuery> getScopeMappings() {
    Map<String, DCQLQuery> mappings = cachedMappings.get();
    if (mappings == null || mappings.isEmpty()) {
      loadFromDatabase();
      mappings = cachedMappings.get();
    }
    return mappings;
  }

  /**
   * Load scope mappings from database and cache them.
   */
  protected void loadFromDatabase() {
    try {
      List<DCQLScopeMappingDto> dtos = dcqlScopeMappingRepository.findAllEnabled();
      Map<String, DCQLQuery> mappings = new ConcurrentHashMap<>();

      for (DCQLScopeMappingDto dto : dtos) {
        try {
          DCQLQuery dcqlQuery = objectMapper.readValue(dto.getDcqlQuery(), DCQLQuery.class);
          validateUniqueIdentifiers(dcqlQuery);
          mappings.put(dto.getScope(), dcqlQuery);
          log.debug("Loaded scope mapping: {} -> {}", dto.getScope(), dto.getDescription());
        } catch (JsonProcessingException e) {
          log.error("Failed to parse DCQL query for scope: {}", dto.getScope(), e);
        } catch (OID4VPException e) {
          log.error("Invalid DCQL query for scope: {}", dto.getScope(), e);
        }
      }

      cachedMappings.set(mappings);
      log.info("Loaded {} scope mappings from database", mappings.size());
    } catch (RuntimeException e) {
      log.error("Failed to load scope mappings from database", e);
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_LOAD_FAILED, e));
    }
  }

  /**
   * Reload scope mappings from database.
   */
  public void reloadMappings() {
    log.info("Reloading scope mappings from database");
    loadFromDatabase();
  }

  /**
   * Clear cached mappings.
   */
  public void clearCache() {
    cachedMappings.set(new ConcurrentHashMap<>());
    log.info("Scope mappings cache cleared");
  }

  public DCQLQuery scopeToDCQL(String scope) throws OID4VPException {
    if (scope == null || scope.trim().isEmpty()) {
      return null;
    }

    Map<String, DCQLQuery> mappings = cachedMappings.get();
    String[] scopes = scope.trim().split("\\s+");
    List<DCQLQuery> queries = new ArrayList<>();

    for (String scopeValue : scopes) {
      DCQLQuery query = mappings.get(scopeValue);
      if (query != null) {
        queries.add(query);
      }
    }

    if (queries.isEmpty()) {
      return null;
    }

    if (queries.size() == 1) {
      return queries.get(0);
    }

    return mergeDCQLQueries(queries);
  }

  /**
   * Save scope mapping to database and update cache.
   * Similar to VerifierConfigService.saveOID4VPConfig pattern.
   */
  public void saveScopeMapping(String scope, DCQLQuery dcqlQuery, String description)
      throws OID4VPException {
    try {
      validateUniqueIdentifiers(dcqlQuery);
      String dcqlJson = objectMapper.writeValueAsString(dcqlQuery);

      DCQLScopeMappingDto dto = dcqlScopeMappingRepository.findByScope(scope)
          .map(existing -> {
            existing.setDcqlQuery(dcqlJson);
            existing.setDescription(description);
            return existing;
          })
          .orElse(DCQLScopeMappingDto.builder()
              .scope(scope)
              .dcqlQuery(dcqlJson)
              .description(description)
              .enabled(true)
              .build());

      dcqlScopeMappingRepository.save(dto);

      // Update cache
      Map<String, DCQLQuery> mappings = new ConcurrentHashMap<>(cachedMappings.get());
      mappings.put(scope, dcqlQuery);
      cachedMappings.set(mappings);

      log.info("Scope mapping saved and cache updated: scope={}", scope);
    } catch (JsonProcessingException e) {
      log.error("Failed to save scope mapping: {}", scope, e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_SAVE_FAILED, scope, e);
    }
  }

  /**
   * Save scope mapping to database and update cache (without description).
   */
  public void saveScopeMapping(String scope, DCQLQuery dcqlQuery) throws OID4VPException {
    saveScopeMapping(scope, dcqlQuery, null);
  }

  /**
   * Delete scope mapping from database and update cache.
   */
  public void deleteScopeMapping(String scope) {
    try {
      dcqlScopeMappingRepository.deleteByScope(scope);

      // Update cache
      Map<String, DCQLQuery> mappings = new ConcurrentHashMap<>(cachedMappings.get());
      mappings.remove(scope);
      cachedMappings.set(mappings);

      log.info("Scope mapping deleted and cache updated: scope={}", scope);
    } catch (RuntimeException e) {
      log.error("Failed to delete scope mapping: {}", scope, e);
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_DELETE_FAILED, scope, e));
    }
  }

  /**
   * Enable or disable a scope mapping and update cache.
   */
  public void setEnabled(String scope, boolean enabled) {
    try {
      DCQLScopeMappingDto dto = dcqlScopeMappingRepository.findByScope(scope)
          .orElseThrow(() -> new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_NOT_FOUND, scope));

      dto.setEnabled(enabled);
      dcqlScopeMappingRepository.save(dto);

      // Update cache
      Map<String, DCQLQuery> mappings = new ConcurrentHashMap<>(cachedMappings.get());
      if (enabled) {
        DCQLQuery dcqlQuery = objectMapper.readValue(dto.getDcqlQuery(), DCQLQuery.class);
        mappings.put(scope, dcqlQuery);
      } else {
        mappings.remove(scope);
      }
      cachedMappings.set(mappings);

      log.info("Scope mapping {} and cache updated: scope={}", enabled ? "enabled" : "disabled", scope);
    } catch (OID4VPException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse DCQL query for scope: {}", scope, e);
      throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_UPDATE_FAILED, scope, e));
    }
  }

  public DCQLQuery mergeDCQLQueries(List<DCQLQuery> queries) throws OID4VPException {
    List<DCQLQuery.CredentialQuery> mergedCredentials = new ArrayList<>();
    List<DCQLQuery.CredentialSet> mergedCredentialSets = new ArrayList<>();

    Set<String> usedCredentialIds = new HashSet<>();
    Set<String> usedClaimIds = new HashSet<>();

    for (DCQLQuery query : queries) {
      if (query.getCredentials() != null) {
        for (DCQLQuery.CredentialQuery cred : query.getCredentials()) {
          if (usedCredentialIds.contains(cred.getId())) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_DUPLICATE_CREDENTIAL_ID, cred.getId());
          }
          usedCredentialIds.add(cred.getId());

          if (cred.getClaims() != null) {
            for (DCQLQuery.ClaimQuery claim : cred.getClaims()) {
              if (claim.getId() != null && usedClaimIds.contains(claim.getId())) {
                throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_DUPLICATE_CLAIM_ID, claim.getId());
              }
              if (claim.getId() != null) {
                usedClaimIds.add(claim.getId());
              }
            }
          }

          mergedCredentials.add(cred);
        }
      }

      if (query.getCredentialSets() != null && !query.getCredentialSets().isEmpty()) {
        mergedCredentialSets.addAll(query.getCredentialSets());
      }
    }

    DCQLQuery.DCQLQueryBuilder builder = DCQLQuery.builder()
        .credentials(mergedCredentials);

    if (!mergedCredentialSets.isEmpty()) {
      builder.credentialSets(mergedCredentialSets);
    }

    return builder.build();
  }

  public void validateUniqueIdentifiers(DCQLQuery query) throws OID4VPException {
    Set<String> credentialIds = new HashSet<>();
    Set<String> claimIds = new HashSet<>();

    if (query.getCredentials() != null) {
      for (DCQLQuery.CredentialQuery cred : query.getCredentials()) {
        if (!credentialIds.add(cred.getId())) {
          throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_DUPLICATE_CREDENTIAL_ID, cred.getId());
        }

        if (cred.getClaims() != null) {
          for (DCQLQuery.ClaimQuery claim : cred.getClaims()) {
            if (claim.getId() != null && !claimIds.add(claim.getId())) {
              throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_DUPLICATE_CLAIM_ID, claim.getId());
            }
          }
        }

        if (cred.getClaimSets() != null) {
          for (DCQLQuery.ClaimSet claimSet : cred.getClaimSets()) {
            if (!claimIds.add(claimSet.getId())) {
              throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_DUPLICATE_CLAIM_ID, claimSet.getId());
            }
          }
        }
      }
    }
  }

  // ===== CRUD Methods for REST API =====

  /**
   * Get all scope mappings as list of maps (for REST API).
   */
  public List<Map<String, Object>> getAllScopeMappings() {
    List<DCQLScopeMappingDto> dtos = dcqlScopeMappingRepository.findAll();
    return dtos.stream()
        .map(this::dtoToMap)
        .collect(Collectors.toList());
  }

  /**
   * Get scope mapping by ID as map (for REST API).
   */
  public Map<String, Object> getScopeMappingById(Long id) {
    return dcqlScopeMappingRepository.findById(id)
        .map(this::dtoToMap)
        .orElse(null);
  }

  /**
   * Create new scope mapping (for REST API).
   */
  public Map<String, Object> createScopeMapping(String scope, String dcqlJson, String description)
      throws OID4VPException {
    // Check for duplicate scope
    if (dcqlScopeMappingRepository.existsByScope(scope)) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_ALREADY_EXISTS, scope);
    }

    // Validate and parse DCQL JSON
    DCQLQuery dcqlQuery = parseDcqlQuery(dcqlJson);
    validateUniqueIdentifiers(dcqlQuery);

    // Save to database
    DCQLScopeMappingDto dto = DCQLScopeMappingDto.builder()
        .scope(scope)
        .dcqlQuery(dcqlJson)
        .description(description)
        .enabled(true)
        .build();

    DCQLScopeMappingDto saved = dcqlScopeMappingRepository.save(dto);
    log.info("Created new scope mapping: scope={}, id={}", scope, saved.getId());

    // Update cache
    reloadMappings();

    return dtoToMap(saved);
  }

  /**
   * Update scope mapping by ID (for REST API).
   */
  public Map<String, Object> updateScopeMappingById(Long id, String scope, String dcqlJson,
      String description, Boolean enabled) throws OID4VPException {

    Optional<DCQLScopeMappingDto> dtoOpt = dcqlScopeMappingRepository.findById(id);
    if (dtoOpt.isEmpty()) {
      return null;
    }

    DCQLScopeMappingDto dto = dtoOpt.get();
    String oldScope = dto.getScope();

    // Check for duplicate scope if scope is being changed
    if (scope != null && !scope.equals(oldScope) && dcqlScopeMappingRepository.existsByScope(scope)) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_ALREADY_EXISTS, scope);
    }

    // Update fields
    if (scope != null && !scope.trim().isEmpty()) {
      dto.setScope(scope);
    }
    if (dcqlJson != null && !dcqlJson.trim().isEmpty()) {
      DCQLQuery dcqlQuery = parseDcqlQuery(dcqlJson);
      validateUniqueIdentifiers(dcqlQuery);
      dto.setDcqlQuery(dcqlJson);
    }
    if (description != null) {
      dto.setDescription(description);
    }
    if (enabled != null) {
      dto.setEnabled(enabled);
    }

    DCQLScopeMappingDto saved = dcqlScopeMappingRepository.save(dto);
    log.info("Updated scope mapping: id={}, scope={}", id, saved.getScope());

    // Update cache
    reloadMappings();

    return dtoToMap(saved);
  }

  /**
   * Delete scope mapping by ID (for REST API).
   */
  public boolean deleteScopeMappingById(Long id) {
    Optional<DCQLScopeMappingDto> dtoOpt = dcqlScopeMappingRepository.findById(id);
    if (dtoOpt.isEmpty()) {
      return false;
    }

    String scope = dtoOpt.get().getScope();
    dcqlScopeMappingRepository.deleteById(id);
    log.info("Deleted scope mapping: id={}, scope={}", id, scope);

    // Update cache
    reloadMappings();

    return true;
  }

  /**
   * Toggle enabled status by ID (for REST API).
   */
  public Map<String, Object> toggleEnabledById(Long id) throws OID4VPException {
    try {
      Optional<DCQLScopeMappingDto> dtoOpt = dcqlScopeMappingRepository.findById(id);
      if (dtoOpt.isEmpty()) {
        return null;
      }

      DCQLScopeMappingDto dto = dtoOpt.get();
      dto.setEnabled(!dto.getEnabled());

      DCQLScopeMappingDto saved = dcqlScopeMappingRepository.save(dto);
      log.info("Toggled scope mapping enabled: id={}, scope={}, enabled={}",
          id, saved.getScope(), saved.getEnabled());

      // Update cache
      reloadMappings();

      return dtoToMap(saved);
    } catch (RuntimeException e) {
      log.error("Failed to toggle scope mapping enabled: {}", id, e);
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_SCOPE_UPDATE_FAILED, id.toString(), e);
    }
  }

  /**
   * Validate DCQL query JSON (for REST API).
   */
  public Map<String, Object> validateDcqlQueryJson(String dcqlJson) {
    Map<String, Object> result = new LinkedHashMap<>();
    try {
      DCQLQuery dcqlQuery = parseDcqlQuery(dcqlJson);
      validateUniqueIdentifiers(dcqlQuery);

      result.put("valid", true);
      result.put("parsedQuery", objectMapper.convertValue(dcqlQuery, Map.class));

      // Add warnings for empty fields
      List<String> warnings = new ArrayList<>();
      if (dcqlQuery.getCredentials() == null || dcqlQuery.getCredentials().isEmpty()) {
        warnings.add("credentials array is empty");
      }
      if (!warnings.isEmpty()) {
        result.put("warnings", warnings);
      }

    } catch (OID4VPException e) {
      result.put("valid", false);
      result.put("error", e.getMessage());
    } catch (IllegalArgumentException e) {
      result.put("valid", false);
      result.put("error", "Invalid JSON format: " + e.getMessage());
    }
    return result;
  }

  /**
   * Parse DCQL JSON string to DCQLQuery object.
   */
  private DCQLQuery parseDcqlQuery(String dcqlJson) throws OID4VPException {
    try {
      return objectMapper.readValue(dcqlJson, DCQLQuery.class);
    } catch (JsonProcessingException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_DCQL_PARSE_FAILED,
          "Invalid DCQL JSON format: " + e.getMessage());
    }
  }

  /**
   * Convert DTO to map for REST API response.
   */
  private Map<String, Object> dtoToMap(DCQLScopeMappingDto dto) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", dto.getId());
    map.put("scope", dto.getScope());

    // Parse dcqlQuery JSON to object
    try {
      Map<String, Object> dcqlMap = objectMapper.readValue(dto.getDcqlQuery(), Map.class);
      map.put("dcqlQuery", dcqlMap);
    } catch (JsonProcessingException e) {
      // If parsing fails, return as string
      map.put("dcqlQuery", dto.getDcqlQuery());
      map.put("dcqlQueryParseError", e.getMessage());
    }

    map.put("description", dto.getDescription());
    map.put("enabled", dto.getEnabled());
    map.put("createdAt", dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : null);
    map.put("updatedAt", dto.getUpdatedAt() != null ? dto.getUpdatedAt().toString() : null);

    return map;
  }
}