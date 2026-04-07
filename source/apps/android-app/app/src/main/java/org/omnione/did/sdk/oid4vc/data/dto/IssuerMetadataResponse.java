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
import java.util.Map;

public class IssuerMetadataResponse {

    @SerializedName("credential_issuer")
    private String credentialIssuer;

    @SerializedName("authorization_servers")
    private List<String> authorizationServers;

    @SerializedName("credential_endpoint")
    private String credentialEndpoint;

    @SerializedName("credential_configurations_supported")
    private Map<String, CredentialConfiguration> credentialConfigurationsSupported;

    /**
     * Gets the credential issuer URL.
     * @return the credential issuer string.
     */
    public String getCredentialIssuer() { return credentialIssuer; }
    /**
     * Gets the list of authorization server URLs.
     * @return the list of authorization server strings.
     */
    public List<String> getAuthorizationServers() { return authorizationServers; }
    /**
     * Gets the credential endpoint URL.
     * @return the credential endpoint string.
     */
    public String getCredentialEndpoint() { return credentialEndpoint; }
    /**
     * Gets the map of supported credential configurations.
     * @return the map of credential configurations.
     */
    public Map<String, CredentialConfiguration> getCredentialConfigurationsSupported() { return credentialConfigurationsSupported; }

    public static class CredentialConfiguration {
        @SerializedName("format")
        private String format;

        @SerializedName("doctype")
        private String doctype;

        /**
         * Gets the format of the credential configuration.
         * @return the format string.
         */
        public String getFormat() { return format; }
        /**
         * Gets the document type of the credential configuration.
         * @return the doctype string.
         */
        public String getDoctype() { return doctype; }
    }
}
