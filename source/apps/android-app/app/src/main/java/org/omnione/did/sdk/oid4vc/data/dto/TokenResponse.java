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
    private String cNonce;

    @SerializedName("c_nonce_expires_in")
    private Integer cNonceExpiresIn;

    @SerializedName("authorization_details")
    private List<AuthorizationDetails> authorizationDetails;

    /**
     * Gets the access token.
     * @return the access token string.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Gets the token type.
     * @return the token type string.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type.
     * @param tokenType the token type string to set.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Gets the expiration time of the access token.
     * @return the expiration time in seconds.
     */
    public Integer getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the expiration time of the access token.
     * @param expiresIn the expiration time in seconds to set.
     */
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Gets the credential nonce.
     * @return the c_nonce string.
     */
    public String getcNonce() {
        return cNonce;
    }

    /**
     * Sets the credential nonce.
     * @param cNonce the c_nonce string to set.
     */
    public void setcNonce(String cNonce) {
        this.cNonce = cNonce;
    }

    /**
     * Gets the expiration time of the credential nonce.
     * @return the c_nonce expiration time in seconds.
     */
    public Integer getcNonceExpiresIn() {
        return cNonceExpiresIn;
    }

    /**
     * Sets the expiration time of the credential nonce.
     * @param cNonceExpiresIn the c_nonce expiration time in seconds to set.
     */
    public void setcNonceExpiresIn(Integer cNonceExpiresIn) {
        this.cNonceExpiresIn = cNonceExpiresIn;
    }

    /**
     * Gets the authorization details.
     * @return the list of AuthorizationDetails objects.
     */
    public List<AuthorizationDetails> getAuthorizationDetails() {
        return authorizationDetails;
    }

    /**
     * Sets the authorization details.
     * @param authorizationDetails the list of AuthorizationDetails objects to set.
     */
    public void setAuthorizationDetails(List<AuthorizationDetails> authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }
}
