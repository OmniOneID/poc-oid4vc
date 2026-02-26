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

package com.example.did.oid4vc.verifier.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLScopeMappingDto;
import org.omnione.did.oid4vc.oid4vp.repository.DCQLScopeMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * File-based implementation of DCQLScopeMappingRepository.
 * Loads scope mappings from JSON file at startup.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "file")
public class FileDCQLScopeMappingRepository implements DCQLScopeMappingRepository {

    private final ObjectMapper objectMapper;

    @Value("${oid4vp.repository.file.scope-mapping-path:classpath:oid4vp/dcql-scope-mappings.json}")
    private Resource scopeMappingFile;

    private final Map<Long, DCQLScopeMappingDto> mappingById = new ConcurrentHashMap<>();
    private final Map<String, DCQLScopeMappingDto> mappingByScope = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    private void loadFromFile() {
        if (scopeMappingFile == null || !scopeMappingFile.exists()) {
            log.warn("DCQL scope mapping file not found: {}", scopeMappingFile);
            return;
        }

        try {
            List<DCQLScopeMappingDto> mappings = objectMapper.readValue(
                    scopeMappingFile.getInputStream(),
                    new TypeReference<List<DCQLScopeMappingDto>>() {}
            );

            LocalDateTime now = LocalDateTime.now();
            for (DCQLScopeMappingDto dto : mappings) {
                if (dto.getId() == null) {
                    dto.setId(idGenerator.getAndIncrement());
                }
                if (dto.getCreatedAt() == null) {
                    dto.setCreatedAt(now);
                }
                if (dto.getUpdatedAt() == null) {
                    dto.setUpdatedAt(now);
                }
                if (dto.getEnabled() == null) {
                    dto.setEnabled(true);
                }

                mappingById.put(dto.getId(), dto);
                mappingByScope.put(dto.getScope(), dto);
            }

            log.info("Loaded {} DCQL scope mappings from file: {}", mappings.size(), scopeMappingFile.getFilename());
        } catch (IOException e) {
            log.error("Failed to load DCQL scope mappings from file: {}", scopeMappingFile, e);
            throw new RuntimeException("Failed to load DCQL scope mappings from file", e);
        }
    }

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
                .filter(dto -> Boolean.TRUE.equals(dto.getEnabled()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DCQLScopeMappingDto> findAll() {
        return new ArrayList<>(mappingById.values());
    }

    @Override
    public DCQLScopeMappingDto save(DCQLScopeMappingDto dto) {
        log.warn("FileDCQLScopeMappingRepository is read-only. Save operation updates in-memory only.");

        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            dto.setId(idGenerator.getAndIncrement());
            dto.setCreatedAt(now);
        }
        dto.setUpdatedAt(now);

        mappingById.put(dto.getId(), dto);
        mappingByScope.put(dto.getScope(), dto);

        return dto;
    }

    @Override
    public void deleteById(Long id) {
        log.warn("FileDCQLScopeMappingRepository is read-only. Delete operation updates in-memory only.");

        DCQLScopeMappingDto removed = mappingById.remove(id);
        if (removed != null) {
            mappingByScope.remove(removed.getScope());
        }
    }

    @Override
    public void deleteByScope(String scope) {
        log.warn("FileDCQLScopeMappingRepository is read-only. Delete operation updates in-memory only.");

        DCQLScopeMappingDto removed = mappingByScope.remove(scope);
        if (removed != null) {
            mappingById.remove(removed.getId());
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