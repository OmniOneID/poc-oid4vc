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

package org.omnione.did.oid4vc.oid4vci.service.store.memory;

import org.omnione.did.oid4vc.oid4vci.api.dto.PreAuthorizeResponse;
import org.omnione.did.oid4vc.oid4vci.service.store.CredentialOfferStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCredentialOfferStore implements CredentialOfferStore {

    private final Map<String, PreAuthorizeResponse> store = new ConcurrentHashMap<>();

    @Override
    public void save(String requestId, PreAuthorizeResponse response) {
        store.put(requestId, response);
    }

    @Override
    public PreAuthorizeResponse consume(String requestId) {
        return store.remove(requestId);
    }
}
