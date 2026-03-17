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

public class AuthorizationDetails {

    @SerializedName("type")
    private String type;

    @SerializedName("credential_configuration_id")
    private String credentialConfigurationId;

    @SerializedName("credential_identifiers")
    private List<String> credentialIdentifiers;

    /**
     * Constructs a new AuthorizationDetails with the specified type, configuration ID, and identifiers.
     * @param type the type of authorization details.
     * @param credentialConfigurationId the ID of the credential configuration.
     * @param credentialIdentifiers the list of credential identifiers.
     */
    public AuthorizationDetails(String type, String credentialConfigurationId, List<String> credentialIdentifiers) {
        this.type = type;
        this.credentialConfigurationId = credentialConfigurationId;
        this.credentialIdentifiers = credentialIdentifiers;
    }

    /**
     * Gets the type of authorization details.
     * @return the type string.
     */
    public String getType() { return type; }
    /**
     * Gets the credential configuration ID.
     * @return the configuration ID string.
     */
    public String getCredentialConfigurationId() { return credentialConfigurationId; }

    /**
     * Sets the type of authorization details.
     * @param type the type string to set.
     */
    public void setType(String type) { this.type = type; }
    /**
     * Sets the credential configuration ID.
     * @param credentialConfigurationId the configuration ID string to set.
     */
    public void setCredentialConfigurationId(String credentialConfigurationId) { this.credentialConfigurationId = credentialConfigurationId; }

    /**
     * Gets the list of credential identifiers.
     * @return the list of credential identifier strings.
     */
    public List<String> getCredentialIdentifiers() {
        return credentialIdentifiers;
    }

    /**
     * Sets the list of credential identifiers.
     * @param credentialIdentifiers the list of credential identifier strings to set.
     */
    public void setCredentialIdentifiers(List<String> credentialIdentifiers) {
        this.credentialIdentifiers = credentialIdentifiers;
    }
}
