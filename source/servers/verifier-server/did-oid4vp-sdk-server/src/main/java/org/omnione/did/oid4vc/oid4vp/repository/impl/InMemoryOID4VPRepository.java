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
import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of OID4VPRepository.
 * Used as default when no other implementation is provided.
 */
@Slf4j
public class InMemoryOID4VPRepository implements OID4VPRepository {

    private final Map<Long, OID4VPConfigDto> configById = new ConcurrentHashMap<>();
    private final Map<String, OID4VPConfigDto> configByType = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

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
        LocalDateTime now = LocalDateTime.now();

        if (dto.getId() == null) {
            dto.setId(idGenerator.getAndIncrement());
            dto.setCreatedAt(now);
        }
        dto.setUpdatedAt(now);

        configById.put(dto.getId(), dto);
        configByType.put(dto.getType(), dto);

        log.debug("Saved OID4VP config: id={}, type={}", dto.getId(), dto.getType());
        return dto;
    }

    @Override
    public void deleteById(Long id) {
        OID4VPConfigDto removed = configById.remove(id);
        if (removed != null) {
            configByType.remove(removed.getType());
            log.debug("Deleted OID4VP config: id={}, type={}", id, removed.getType());
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