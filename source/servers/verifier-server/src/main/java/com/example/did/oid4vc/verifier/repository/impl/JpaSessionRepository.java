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

import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;
import com.example.did.oid4vc.verifier.repository.entity.VerificationSessionEntity;
import com.example.did.oid4vc.verifier.repository.impl.jpa.VerificationSessionJpaRepository;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oid4vp.repository.type", havingValue = "jpa", matchIfMissing = true)
public class JpaSessionRepository implements SessionRepository {

  private final VerificationSessionJpaRepository jpaRepository;

  @Override
  public void saveByState(String state, VerificationSession session) {
    VerificationSessionEntity entity = toEntity(session);
    jpaRepository.save(entity);
    log.debug("Session saved to database for transactionId: {}, state: {}", session.getTransactionId(), state);
  }

  @Override
  public Optional<VerificationSession> findByState(String state) {
    return jpaRepository.findByState(state).map(this::toDto);
  }

  @Override
  public Optional<VerificationSession> findByRequestId(String requestId) {
    return jpaRepository.findByRequestId(requestId).map(this::toDto);
  }

  @Override
  public Optional<VerificationSession> findByTransactionId(String transactionId) {
    return jpaRepository.findById(transactionId).map(this::toDto);
  }

  @Override
  public Map<String, VerificationSession> findAll() {
    return jpaRepository.findAll().stream()
        .collect(Collectors.toMap(
            VerificationSessionEntity::getTransactionId,
            this::toDto
        ));
  }

  @Override
  public boolean existsByState(String state) {
    return jpaRepository.findByState(state).isPresent();
  }

  @Override
  public void clear() {
    jpaRepository.deleteAll();
    log.debug("All sessions cleared from database");
  }

  @Override
  public int count() {
    return (int) jpaRepository.count();
  }

  // ========== Mapper Methods ==========
  private VerificationSessionEntity toEntity(VerificationSession dto) {
    return VerificationSessionEntity.builder()
        .transactionId(dto.getTransactionId())
        .state(dto.getState())
        .nonce(dto.getNonce())
        .dcqlQuery(dto.getDcqlQuery())
        .responseMode(dto.getResponseMode())
        .requestId(dto.getRequestId())
        .status(dto.getStatus())
        .clientMetadata(dto.getClientMetadata())
        .requestUriFetchedAt(dto.getRequestUriFetchedAt())
        .createdAt(dto.getCreatedAt())
        .expiresAt(dto.getExpiresAt())
        .vpToken(dto.getVpToken())
        .build();
  }

  private VerificationSession toDto(VerificationSessionEntity entity) {
    VerificationSession dto = new VerificationSession();
    dto.setTransactionId(entity.getTransactionId());
    dto.setState(entity.getState());
    dto.setNonce(entity.getNonce());
    dto.setDcqlQuery(entity.getDcqlQuery());
    dto.setResponseMode(entity.getResponseMode());
    dto.setRequestId(entity.getRequestId());
    dto.setStatus(entity.getStatus());
    dto.setClientMetadata(entity.getClientMetadata());
    dto.setRequestUriFetchedAt(entity.getRequestUriFetchedAt());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setExpiresAt(entity.getExpiresAt());
    dto.setVpToken(entity.getVpToken());
    return dto;
  }
}