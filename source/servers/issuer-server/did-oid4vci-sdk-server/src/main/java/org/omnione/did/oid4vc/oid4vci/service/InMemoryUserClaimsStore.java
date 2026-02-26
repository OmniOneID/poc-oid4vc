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

package org.omnione.did.oid4vc.oid4vci.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnMissingBean(name = "jpaUserClaimsStore")
public class InMemoryUserClaimsStore implements UserClaimsStore {
    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    @Override
    public void saveClaims(String userId, String credentialType, Map<String, Object> claims) {
        store.put(userId + ":" + credentialType, claims);
    }

    @Override
    public Map<String, Object> getClaims(String userId, String credentialType) {
        return store.get(userId + ":" + credentialType);
    }

    @Override
    public Map<String, Map<String, Object>> getAllEntries() {
        return new HashMap<>(store);
    }
}
