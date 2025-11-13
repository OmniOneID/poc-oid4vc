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

package com.example.authorization.authorization.oid4vci.preauthorized.service;

import com.example.authorization.authorization.oid4vci.preauthorized.dto.PreAuthorizedCode;
import com.example.authorization.authorization.repository.InMemoryPreAuthorizedCodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PreAuthorizationService {

    private final InMemoryPreAuthorizedCodeRepository repository;
    private static final int PRE_AUTHORIZED_CODE_EXPIRY_SECONDS = 600;
    private static final int USER_PIN_LENGTH = 4;

    public PreAuthorizationService(InMemoryPreAuthorizedCodeRepository repository) {
        this.repository = repository;
    }

    public PreAuthorizedCode create(Set<String> scopes, String cNonce) {
        String code = UUID.randomUUID().toString();
        String userPin = generateUserPin();
        Instant expiresAt = Instant.now().plusSeconds(PRE_AUTHORIZED_CODE_EXPIRY_SECONDS);
        PreAuthorizedCode preAuthorizedCode = new PreAuthorizedCode(code, expiresAt, scopes, userPin, cNonce);
        repository.save(preAuthorizedCode);
        return preAuthorizedCode;
    }

    public Optional<PreAuthorizedCode> findByCode(String code) {
        return repository.findByValue(code);
    }

    public void consume(String code) {
        Optional<PreAuthorizedCode> optionalCode = repository.findByValue(code);
        optionalCode.ifPresent(c -> {
            if (!c.isConsumed()) {
                c.setConsumed(true);
                repository.save(c);
            }
        });
    }

    private String generateUserPin() {
        int random = ThreadLocalRandom.current().nextInt((int) Math.pow(10, USER_PIN_LENGTH));
        return String.format("%0" + USER_PIN_LENGTH + "d", random);
    }

    public int getExpiresIn() {
        return PRE_AUTHORIZED_CODE_EXPIRY_SECONDS;
    }
}