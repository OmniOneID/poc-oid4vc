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

package org.omnione.did.sdk.oid4vc.data.dto;

public class TokenRequest {

    private String grant_type;
    private String client_id;

    /**
     * Constructs a new TokenRequest with the specified grant type and client ID.
     * @param grant_type the grant type for the token request.
     * @param client_id the client ID for the token request.
     */
    public TokenRequest(String grant_type, String client_id) {
        this.grant_type = grant_type;
        this.client_id = client_id;
    }
    /**
     * Gets the grant type of the token request.
     * @return the grant type string.
     */
    public String getGrant_type() {
        return grant_type;
    }

    /**
     * Gets the client ID of the token request.
     * @return the client ID string.
     */
    public String getClient_id() {
        return client_id;
    }
}
