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

package org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.grant;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

public class PreAuthorizedCodeGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    public static final AuthorizationGrantType PRE_AUTHORIZED_CODE = new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");

    private final String preAuthorizedCode;
    private final String txCode;
    private String userId;

    public PreAuthorizedCodeGrantAuthenticationToken(Authentication clientPrincipal, String preAuthorizedCode, @Nullable String txCode, @Nullable Map<String, Object> additionalParameters) {
        super(PRE_AUTHORIZED_CODE, clientPrincipal, additionalParameters);
        this.preAuthorizedCode = preAuthorizedCode;
        this.txCode = txCode;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return this.userId != null ? this.userId : super.getName();
    }

    public String getPreAuthorizedCode() {
        return this.preAuthorizedCode;
    }

    @Nullable
    public String getTxCode() {
        return this.txCode;
    }
}
