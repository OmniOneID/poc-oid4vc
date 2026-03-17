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

public class CredentialRequest {

    @SerializedName("credential_configuration_id")
    private String credentialConfigurationId;
    @SerializedName("credential_identifier")
    private Object credentialIdentifier;
    @SerializedName("proofs")
    private Proofs proofs;

    /**
     * Gets the credential configuration ID.
     * @return the credential configuration ID string.
     */
    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    /**
     * Sets the credential configuration ID.
     * @param credentialConfigurationId the credential configuration ID string to set.
     */
    public void setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
    }

    /**
     * Gets the credential identifier.
     * @return the credential identifier object.
     */
    public Object getCredentialIdentifier() {
        return credentialIdentifier;
    }

    /**
     * Sets the credential identifier.
     * @param credentialIdentifier the credential identifier object to set.
     */
    public void setCredentialIdentifier(Object credentialIdentifier) {
        this.credentialIdentifier = credentialIdentifier;
    }

    /**
     * Gets the proofs associated with the credential request.
     * @return the proofs object.
     */
    public Proofs getProofs() {
        return proofs;
    }

    /**
     * Sets the proofs associated with the credential request.
     * @param proofs the proofs object to set.
     */
    public void setProofs(Proofs proofs) {
        this.proofs = proofs;
    }

}
