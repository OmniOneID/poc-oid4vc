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

import org.omnione.did.oid4vc.oid4vci.dto.credential.CredentialRequest;
import org.omnione.did.oid4vc.oid4vci.service.store.SessionStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore implements SessionStore {

    private final Map<String, String> issuerStates = new ConcurrentHashMap<>();
    private final Map<String, String> preAuthorizedCodes = new ConcurrentHashMap<>();
    private final Map<String, CredentialRequest> deferredCredentialStore = new ConcurrentHashMap<>();

    @Override
    public void saveIssuerState(String state, String userId) {
        issuerStates.put(state, userId);
    }

    @Override
    public String consumeIssuerState(String state) {
        return issuerStates.remove(state);
    }

    @Override
    public void savePreAuthorizedCode(String code, String userId) {
        preAuthorizedCodes.put(code, userId);
    }

    @Override
    public String consumePreAuthorizedCode(String code) {
        return preAuthorizedCodes.remove(code);
    }

    @Override
    public void saveDeferredCredentialRequest(String transactionId, CredentialRequest request) {
        deferredCredentialStore.put(transactionId, request);
    }

    @Override
    public CredentialRequest consumeDeferredCredentialRequest(String transactionId) {
        return deferredCredentialStore.remove(transactionId);
    }
}
