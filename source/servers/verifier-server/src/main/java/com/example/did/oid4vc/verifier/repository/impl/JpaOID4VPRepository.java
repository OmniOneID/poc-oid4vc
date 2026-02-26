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

import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;
import com.example.did.oid4vc.verifier.repository.entity.OID4VPEntity;
import com.example.did.oid4vc.verifier.repository.impl.jpa.OID4VPJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa", matchIfMissing = true)
public class JpaOID4VPRepository implements OID4VPRepository {

    private final OID4VPJpaRepository jpaRepository;

    @Override
    public Optional<OID4VPConfigDto> findByType(String type) {
        return jpaRepository.findByType(type).map(this::toDto);
    }

    @Override
    public Optional<OID4VPConfigDto> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDto);
    }

    @Override
    public List<OID4VPConfigDto> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public OID4VPConfigDto save(OID4VPConfigDto dto) {
        OID4VPEntity entity = toEntity(dto);
        OID4VPEntity saved = jpaRepository.save(entity);
        log.debug("OID4VP configuration saved: type={}, id={}", saved.getType(), saved.getId());
        return toDto(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("OID4VP configuration deleted: id={}", id);
    }

    @Override
    public boolean existsByType(String type) {
        return jpaRepository.existsByType(type);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    // ========== Mapper Methods ==========
    private OID4VPConfigDto toDto(OID4VPEntity entity) {
        return OID4VPConfigDto.builder()
            .id(entity.getId())
            .type(entity.getType())
            .config(entity.getConfig())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private OID4VPEntity toEntity(OID4VPConfigDto dto) {
        return OID4VPEntity.builder()
            .id(dto.getId())
            .type(dto.getType())
            .config(dto.getConfig())
            .createdAt(dto.getCreatedAt())
            .updatedAt(dto.getUpdatedAt())
            .build();
    }
}