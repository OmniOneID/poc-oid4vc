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

package com.example.oid4vc.verifier.service;

import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
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
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeToDCQLMapper {

  private final ObjectMapper objectMapper;
  private final Map<String, DCQLQuery> scopeMappings = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    loadScopeMappings();
  }

  public DCQLQuery scopeToDCQL(String scope) {
    if (scope == null || scope.trim().isEmpty()) {
      return null;
    }

    String[] scopes = scope.trim().split("\\s+");
    List<DCQLQuery> queries = new ArrayList<>();

    for (String scopeValue : scopes) {
      DCQLQuery query = scopeMappings.get(scopeValue);
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

  public void registerScopeMapping(String scopeValue, DCQLQuery dcqlQuery) {
    validateUniqueIdentifiers(dcqlQuery);
    scopeMappings.put(scopeValue, dcqlQuery);
    log.info("Registered scope mapping: {} -> {}", scopeValue, dcqlQuery);
  }

  private DCQLQuery mergeDCQLQueries(List<DCQLQuery> queries) {
    List<DCQLQuery.CredentialQuery> mergedCredentials = new ArrayList<>();
    List<DCQLQuery.CredentialSet> mergedCredentialSets = new ArrayList<>();

    Set<String> usedCredentialIds = new HashSet<>();
    Set<String> usedClaimIds = new HashSet<>();

    for (DCQLQuery query : queries) {
      if (query.getCredentials() != null) {
        for (DCQLQuery.CredentialQuery cred : query.getCredentials()) {
          if (usedCredentialIds.contains(cred.getId())) {
            throw new IllegalStateException(
                "Credential ID collision detected: " + cred.getId());
          }
          usedCredentialIds.add(cred.getId());

          if (cred.getClaims() != null) {
            for (DCQLQuery.ClaimQuery claim : cred.getClaims()) {
              if (claim.getId() != null && usedClaimIds.contains(claim.getId())) {
                throw new IllegalStateException(
                    "Claim ID collision detected: " + claim.getId());
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

  private void validateUniqueIdentifiers(DCQLQuery query) {
    Set<String> credentialIds = new HashSet<>();
    Set<String> claimIds = new HashSet<>();

    if (query.getCredentials() != null) {
      for (DCQLQuery.CredentialQuery cred : query.getCredentials()) {
        if (!credentialIds.add(cred.getId())) {
          throw new IllegalArgumentException(
              "Duplicate credential ID in query: " + cred.getId());
        }

        if (cred.getClaims() != null) {
          for (DCQLQuery.ClaimQuery claim : cred.getClaims()) {
            if (claim.getId() != null && !claimIds.add(claim.getId())) {
              throw new IllegalArgumentException(
                  "Duplicate claim ID in query: " + claim.getId());
            }
          }
        }

        if (cred.getClaimSets() != null) {
          for (DCQLQuery.ClaimSet claimSet : cred.getClaimSets()) {
            if (!claimIds.add(claimSet.getId())) {
              throw new IllegalArgumentException(
                  "Duplicate claim set ID in query: " + claimSet.getId());
            }
          }
        }
      }
    }
  }

  private void loadScopeMappings() {
    registerDefaultScopeMappings();
  }

  private void registerDefaultScopeMappings() {
    DCQLQuery nationalIdQuery = DCQLQuery.builder()
        .credentials(Arrays.asList(
            DCQLQuery.CredentialQuery.builder()
                .id("national_id")
                .format("vc+sd-jwt")
                .meta(Map.of(
                    "vct_values", Arrays.asList("https://credentials.gov.kr/identity_credential")
                ))
                .build()
        ))
        .build();

    registerScopeMapping("national_id", nationalIdQuery);
  }
}