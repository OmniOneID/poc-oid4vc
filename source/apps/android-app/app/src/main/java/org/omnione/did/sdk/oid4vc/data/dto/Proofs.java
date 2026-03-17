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

public class Proofs {

    @SerializedName("di_vp")
    private List<String> diVp;

    @SerializedName("jwt")
    private List<String> jwt;

    @SerializedName("attestation")
    private List<String> attestation;

    /**
     * Gets the list of decentralized identifier verifiable presentations.
     * @return the list of di_vp strings.
     */
    public List<String> getDiVp() {
        return diVp;
    }

    /**
     * Sets the list of decentralized identifier verifiable presentations.
     * @param diVp the list of di_vp strings to set.
     */
    public void setDiVp(List<String> diVp) {
        this.diVp = diVp;
    }

    /**
     * Gets the list of JSON Web Tokens.
     * @return the list of jwt strings.
     */
    public List<String> getJwt() {
        return jwt;
    }

    /**
     * Sets the list of JSON Web Tokens.
     * @param jwt the list of jwt strings to set.
     */
    public void setJwt(List<String> jwt) {
        this.jwt = jwt;
    }

    /**
     * Gets the list of attestations.
     * @return the list of attestation strings.
     */
    public List<String> getAttestation() {
        return attestation;
    }

    /**
     * Sets the list of attestations.
     * @param attestation the list of attestation strings to set.
     */
    public void setAttestation(List<String> attestation) {
        this.attestation = attestation;
    }
}
