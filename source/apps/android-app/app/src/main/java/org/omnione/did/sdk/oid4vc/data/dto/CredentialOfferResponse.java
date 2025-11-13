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

public class CredentialOfferResponse {

    @SerializedName("credential_issuer")
    private String credentialIssuer;

    @SerializedName("credential_configuration_ids")
    private List<String> credentialConfigurationIds;

    @SerializedName("grants")
    private Grants grants;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public List<String> getCredentialConfigurationIds() {
        return credentialConfigurationIds;
    }

    public Grants getGrants() {
        return grants;
    }

    public static class Grants {

        @SerializedName("urn:ietf:params:oauth:grant-type:pre-authorized_code")
        private PreAuthorizedCodeGrant preAuthorizedCodeGrant;

        @SerializedName("authorization_code")
        private AuthorizationCodeGrant authorizationCodeGrant;

        public PreAuthorizedCodeGrant getPreAuthorizedCodeGrant() {
            return preAuthorizedCodeGrant;
        }

        public void setPreAuthorizedCodeGrant(PreAuthorizedCodeGrant preAuthorizedCodeGrant) {
            this.preAuthorizedCodeGrant = preAuthorizedCodeGrant;
        }

        public AuthorizationCodeGrant getAuthorizationCodeGrant() {
            return authorizationCodeGrant;
        }

        public void setAuthorizationCodeGrant(AuthorizationCodeGrant authorizationCodeGrant) {
            this.authorizationCodeGrant = authorizationCodeGrant;
        }
    }

    public static class PreAuthorizedCodeGrant {

        @SerializedName("pre-authorized_code")
        private String preAuthorizedCode;

        @SerializedName("tx_code")
        private TxCode txCode;

        public String getPreAuthorizedCode() {
            return preAuthorizedCode;
        }

        public TxCode getTxCode() {
            return txCode;
        }
    }

    public static class TxCode {

        @SerializedName("input_mode")
        private String inputMode;

        @SerializedName("length")
        private int length;

        @SerializedName("description")
        private String description;

        public String getInputMode() {
            return inputMode;
        }

        public int getLength() {
            return length;
        }

        public String getDescription() {
            return description;
        }
    }


    public static class AuthorizationCodeGrant {

        @SerializedName("issuer_state")
        private String issuerState;

        public String getIssuerState() {
            return issuerState;
        }
    }
}