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

public class CredentialResponse {
    @SerializedName("credentials")
    private List<Credential> credentials;

    /**
     * Gets the list of credentials.
     * @return the list of Credential objects.
     */
    public List<Credential> getCredentials() {
        return credentials;
    }

    /**
     * Sets the list of credentials.
     * @param credentials the list of Credential objects to set.
     */
    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }
}
