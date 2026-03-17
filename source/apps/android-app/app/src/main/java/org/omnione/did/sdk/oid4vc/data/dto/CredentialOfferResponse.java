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

    /**
     * Gets the credential issuer URL.
     * @return the credential issuer string.
     */
    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    /**
     * Gets the list of credential configuration IDs.
     * @return the list of configuration ID strings.
     */
    public List<String> getCredentialConfigurationIds() {
        return credentialConfigurationIds;
    }

    /**
     * Gets the grants associated with the credential offer.
     * @return the grants object.
     */
    public Grants getGrants() {
        return grants;
    }

    public static class Grants {

        @SerializedName("urn:ietf:params:oauth:grant-type:pre-authorized_code")
        private PreAuthorizedCodeGrant preAuthorizedCodeGrant;

        @SerializedName("authorization_code")
        private AuthorizationCodeGrant authorizationCodeGrant;

        /**
         * Gets the pre-authorized code grant.
         * @return the pre-authorized code grant object.
         */
        public PreAuthorizedCodeGrant getPreAuthorizedCodeGrant() {
            return preAuthorizedCodeGrant;
        }

        /**
         * Sets the pre-authorized code grant.
         * @param preAuthorizedCodeGrant the pre-authorized code grant object to set.
         */
        public void setPreAuthorizedCodeGrant(PreAuthorizedCodeGrant preAuthorizedCodeGrant) {
            this.preAuthorizedCodeGrant = preAuthorizedCodeGrant;
        }

        /**
         * Gets the authorization code grant.
         * @return the authorization code grant object.
         */
        public AuthorizationCodeGrant getAuthorizationCodeGrant() {
            return authorizationCodeGrant;
        }

        /**
         * Sets the authorization code grant.
         * @param authorizationCodeGrant the authorization code grant object to set.
         */
        public void setAuthorizationCodeGrant(AuthorizationCodeGrant authorizationCodeGrant) {
            this.authorizationCodeGrant = authorizationCodeGrant;
        }
    }

    public static class PreAuthorizedCodeGrant {

        @SerializedName("pre-authorized_code")
        private String preAuthorizedCode;

        @SerializedName("tx_code")
        private TxCode txCode;

        /**
         * Gets the pre-authorized code.
         * @return the pre-authorized code string.
         */
        public String getPreAuthorizedCode() {
            return preAuthorizedCode;
        }

        /**
         * Gets the transaction code information.
         * @return the TxCode object.
         */
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

        /**
         * Gets the input mode for the transaction code.
         * @return the input mode string.
         */
        public String getInputMode() {
            return inputMode;
        }

        /**
         * Gets the length of the transaction code.
         * @return the length of the code.
         */
        public int getLength() {
            return length;
        }

        /**
         * Gets the description of the transaction code.
         * @return the description string.
         */
        public String getDescription() {
            return description;
        }
    }


    public static class AuthorizationCodeGrant {

        @SerializedName("issuer_state")
        private String issuerState;

        /**
         * Gets the issuer state for the authorization code grant.
         * @return the issuer state string.
         */
        public String getIssuerState() {
            return issuerState;
        }
    }
}
