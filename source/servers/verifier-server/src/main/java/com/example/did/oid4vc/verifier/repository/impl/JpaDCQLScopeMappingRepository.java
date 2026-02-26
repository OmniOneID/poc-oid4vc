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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.dto.DCQLScopeMappingDto;
import org.omnione.did.oid4vc.oid4vp.repository.DCQLScopeMappingRepository;
import com.example.did.oid4vc.verifier.repository.entity.DCQLScopeMappingEntity;
import com.example.did.oid4vc.verifier.repository.impl.jpa.DCQLScopeMappingJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa", matchIfMissing = true)
public class JpaDCQLScopeMappingRepository implements DCQLScopeMappingRepository {

  private final DCQLScopeMappingJpaRepository jpaRepository;

  @Override
  public Optional<DCQLScopeMappingDto> findById(Long id) {
    return jpaRepository.findById(id).map(this::toDto);
  }

  @Override
  public Optional<DCQLScopeMappingDto> findByScope(String scope) {
    return jpaRepository.findByScope(scope).map(this::toDto);
  }

  @Override
  public List<DCQLScopeMappingDto> findAllEnabled() {
    return jpaRepository.findAllByEnabledTrue().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<DCQLScopeMappingDto> findAll() {
    return jpaRepository.findAll().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public DCQLScopeMappingDto save(DCQLScopeMappingDto dto) {
    DCQLScopeMappingEntity entity = toEntity(dto);
    DCQLScopeMappingEntity saved = jpaRepository.save(entity);
    log.debug("DCQL scope mapping saved: scope={}, id={}", saved.getScope(), saved.getId());
    return toDto(saved);
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
    log.debug("DCQL scope mapping deleted: id={}", id);
  }

  @Override
  @Transactional
  public void deleteByScope(String scope) {
    jpaRepository.deleteByScope(scope);
    log.debug("DCQL scope mapping deleted: scope={}", scope);
  }

  @Override
  public boolean existsByScope(String scope) {
    return jpaRepository.existsByScope(scope);
  }

  @Override
  public long count() {
    return jpaRepository.count();
  }

  // ========== Mapper Methods ==========
  private DCQLScopeMappingDto toDto(DCQLScopeMappingEntity entity) {
    return DCQLScopeMappingDto.builder()
        .id(entity.getId())
        .scope(entity.getScope())
        .dcqlQuery(entity.getDcqlQuery())
        .description(entity.getDescription())
        .enabled(entity.getEnabled())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  private DCQLScopeMappingEntity toEntity(DCQLScopeMappingDto dto) {
    return DCQLScopeMappingEntity.builder()
        .id(dto.getId())
        .scope(dto.getScope())
        .dcqlQuery(dto.getDcqlQuery())
        .description(dto.getDescription())
        .enabled(dto.getEnabled())
        .createdAt(dto.getCreatedAt())
        .updatedAt(dto.getUpdatedAt())
        .build();
  }
}