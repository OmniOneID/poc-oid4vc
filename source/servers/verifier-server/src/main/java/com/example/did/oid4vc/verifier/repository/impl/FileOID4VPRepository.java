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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;
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

/**
 * File-based implementation of OID4VPRepository.
 * Loads configuration from JSON file at startup.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "file")
public class FileOID4VPRepository implements OID4VPRepository {

    private static final String CONFIG_TYPE_OID4VP = "OID4VP";

    private final ObjectMapper objectMapper;

    @Value("${oid4vp.repository.file.config-path:classpath:oid4vp/oid4vp-config.json}")
    private Resource configFile;

    private final Map<Long, OID4VPConfigDto> configById = new ConcurrentHashMap<>();
    private final Map<String, OID4VPConfigDto> configByType = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    private void loadFromFile() {
        if (configFile == null || !configFile.exists()) {
            log.warn("OID4VP config file not found: {}", configFile);
            return;
        }

        try {
            String configJson = new String(configFile.getInputStream().readAllBytes());
            
            OID4VPConfigDto dto = OID4VPConfigDto.builder()
                    .id(idGenerator.getAndIncrement())
                    .type(CONFIG_TYPE_OID4VP)
                    .config(configJson)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            configById.put(dto.getId(), dto);
            configByType.put(dto.getType(), dto);

            log.info("Loaded OID4VP config from file: {}", configFile.getFilename());
        } catch (IOException e) {
            log.error("Failed to load OID4VP config from file: {}", configFile, e);
            throw new RuntimeException("Failed to load OID4VP config from file", e);
        }
    }

    @Override
    public Optional<OID4VPConfigDto> findByType(String type) {
        return Optional.ofNullable(configByType.get(type));
    }

    @Override
    public Optional<OID4VPConfigDto> findById(Long id) {
        return Optional.ofNullable(configById.get(id));
    }

    @Override
    public List<OID4VPConfigDto> findAll() {
        return new ArrayList<>(configById.values());
    }

    @Override
    public OID4VPConfigDto save(OID4VPConfigDto dto) {
        log.warn("FileOID4VPRepository is read-only. Save operation updates in-memory only.");
        
        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            dto.setId(idGenerator.getAndIncrement());
            dto.setCreatedAt(now);
        }
        dto.setUpdatedAt(now);

        configById.put(dto.getId(), dto);
        configByType.put(dto.getType(), dto);

        return dto;
    }

    @Override
    public void deleteById(Long id) {
        log.warn("FileOID4VPRepository is read-only. Delete operation updates in-memory only.");
        
        OID4VPConfigDto removed = configById.remove(id);
        if (removed != null) {
            configByType.remove(removed.getType());
        }
    }

    @Override
    public boolean existsByType(String type) {
        return configByType.containsKey(type);
    }

    @Override
    public long count() {
        return configById.size();
    }
}