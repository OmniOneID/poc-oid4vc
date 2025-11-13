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

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TokenResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private Integer expiresIn;

    @SerializedName("c_nonce")
    private String cNonce; // Optional

    @SerializedName("c_nonce_expires_in")
    private Integer cNonceExpiresIn; // Optional

    @SerializedName("authorization_details")
    private List<AuthorizationDetails> authorizationDetails;

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getcNonce() {
        return cNonce;
    }

    public void setcNonce(String cNonce) {
        this.cNonce = cNonce;
    }

    public Integer getcNonceExpiresIn() {
        return cNonceExpiresIn;
    }

    public void setcNonceExpiresIn(Integer cNonceExpiresIn) {
        this.cNonceExpiresIn = cNonceExpiresIn;
    }

    public List<AuthorizationDetails> getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(List<AuthorizationDetails> authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }
}