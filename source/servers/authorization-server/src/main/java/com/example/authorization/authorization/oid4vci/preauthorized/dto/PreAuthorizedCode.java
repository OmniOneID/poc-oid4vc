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

package com.example.authorization.authorization.oid4vci.preauthorized.dto;

import java.time.Instant;
import java.util.Set;

public class PreAuthorizedCode {

    private final String value;
    private final Instant expiresAt;
    private final Set<String> scopes;
    private final String userPin;
    private final String cNonce;
    private boolean consumed;

    public PreAuthorizedCode(String value, Instant expiresAt, Set<String> scopes, String userPin, String cNonce) {
        this.value = value;
        this.expiresAt = expiresAt;
        this.scopes = scopes;
        this.userPin = userPin;
        this.cNonce = cNonce;
        this.consumed = false;
    }

    public String getValue() {
        return value;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public String getUserPin() {
        return userPin;
    }

    public String getCNonce() {
        return cNonce;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }
}