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

package org.omnione.did.oid4vc.oid4vp.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLScopeMappingDto;
import org.omnione.did.oid4vc.oid4vp.repository.DCQLScopeMappingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of DCQLScopeMappingRepository.
 * Used as default when no other implementation is provided.
 */
@Slf4j
public class InMemoryDCQLScopeMappingRepository implements DCQLScopeMappingRepository {

    private final Map<Long, DCQLScopeMappingDto> mappingById = new ConcurrentHashMap<>();
    private final Map<String, DCQLScopeMappingDto> mappingByScope = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<DCQLScopeMappingDto> findById(Long id) {
        return Optional.ofNullable(mappingById.get(id));
    }

    @Override
    public Optional<DCQLScopeMappingDto> findByScope(String scope) {
        return Optional.ofNullable(mappingByScope.get(scope));
    }

    @Override
    public List<DCQLScopeMappingDto> findAllEnabled() {
      return mappingById.values().stream()
          .filter(dto -> dto.getEnabled() != null && dto.getEnabled())
          .collect(Collectors.toList());
    }

    @Override
    public List<DCQLScopeMappingDto> findAll() {
        return new ArrayList<>(mappingById.values());
    }

    @Override
    public DCQLScopeMappingDto save(DCQLScopeMappingDto dto) {
        LocalDateTime now = LocalDateTime.now();

        if (dto.getId() == null) {
            dto.setId(idGenerator.getAndIncrement());
            dto.setCreatedAt(now);
        }
        dto.setUpdatedAt(now);

        mappingById.put(dto.getId(), dto);
        mappingByScope.put(dto.getScope(), dto);

        log.debug("Saved DCQL scope mapping: id={}, scope={}", dto.getId(), dto.getScope());
        return dto;
    }

    @Override
    public void deleteById(Long id) {
        DCQLScopeMappingDto removed = mappingById.remove(id);
        if (removed != null) {
            mappingByScope.remove(removed.getScope());
            log.debug("Deleted DCQL scope mapping: id={}, scope={}", id, removed.getScope());
        }
    }

    @Override
    public void deleteByScope(String scope) {
        DCQLScopeMappingDto removed = mappingByScope.remove(scope);
        if (removed != null) {
            mappingById.remove(removed.getId());
            log.debug("Deleted DCQL scope mapping: scope={}, id={}", scope, removed.getId());
        }
    }

    @Override
    public boolean existsByScope(String scope) {
        return mappingByScope.containsKey(scope);
    }

    @Override
    public long count() {
        return mappingById.size();
    }
}