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
import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SessionRepository.
 * Used as default when no other implementation is provided.
 */
@Slf4j
public class InMemorySessionRepository implements SessionRepository {

    private final Map<String, VerificationSession> sessionByState = new ConcurrentHashMap<>();
    private final Map<String, VerificationSession> sessionByRequestId = new ConcurrentHashMap<>();
    private final Map<String, VerificationSession> sessionByTransactionId = new ConcurrentHashMap<>();

    @Override
    public Optional<VerificationSession> findByState(String state) {
        return Optional.ofNullable(sessionByState.get(state));
    }

    @Override
    public Optional<VerificationSession> findByRequestId(String requestId) {
        return Optional.ofNullable(sessionByRequestId.get(requestId));
    }

    @Override
    public Optional<VerificationSession> findByTransactionId(String transactionId) {
        return Optional.ofNullable(sessionByTransactionId.get(transactionId));
    }

    @Override
    public Map<String, VerificationSession> findAll() {
        return new ConcurrentHashMap<>(sessionByState);
    }

    @Override
    public void saveByState(String state, VerificationSession session) {
        sessionByState.put(state, session);

        if (session.getRequestId() != null) {
            sessionByRequestId.put(session.getRequestId(), session);
        }
        if (session.getTransactionId() != null) {
            sessionByTransactionId.put(session.getTransactionId(), session);
        }

        log.debug("Saved session: state={}, requestId={}, transactionId={}",
                state, session.getRequestId(), session.getTransactionId());
    }

    @Override
    public boolean existsByState(String state) {
        return sessionByState.containsKey(state);
    }

    @Override
    public void clear() {
        sessionByState.clear();
        sessionByRequestId.clear();
        sessionByTransactionId.clear();
        log.debug("Cleared all sessions");
    }

    @Override
    public int count() {
        return sessionByState.size();
    }
}