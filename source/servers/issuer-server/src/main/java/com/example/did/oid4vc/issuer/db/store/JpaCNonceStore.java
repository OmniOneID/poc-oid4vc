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

package com.example.did.oid4vc.issuer.db.store;

import com.example.did.oid4vc.issuer.db.entity.Oid4vcCnonceEntity;
import com.example.did.oid4vc.issuer.db.repository.Oid4vcCnonceRepository;
import org.omnione.did.oid4vc.oid4vci.service.store.CNonceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.datasource.url")
public class JpaCNonceStore implements CNonceStore {

    private final Oid4vcCnonceRepository repository;

    @Override
    public void save(String cNonce) {
        Oid4vcCnonceEntity entity = new Oid4vcCnonceEntity();
        entity.setCnonce(cNonce);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(10)); 
        
        repository.save(entity);
    }

    @Override
    public boolean contains(String cNonce) {
        return repository.findByCnonce(cNonce).isPresent();
    }

    @Override
    public void remove(String cNonce) {
        repository.findByCnonce(cNonce).ifPresent(repository::delete);
    }
}
