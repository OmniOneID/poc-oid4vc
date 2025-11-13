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

    @SerializedName("format")
    private String format;
    @SerializedName("doc_type")
    private String docType;
    @SerializedName("credential_identifier")
    private Object credentialIdentifier;
    @SerializedName("proof")
    private Proof proof;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String doc_type) {
        this.docType = doc_type;
    }

    public Object getCredentialIdentifier() {
        return credentialIdentifier;
    }

    public void setCredentialIdentifier(Object credentialIdentifier) {
        this.credentialIdentifier = credentialIdentifier;
    }

    public Proof getProof() {
        return proof;
    }

    public void setProof(Proof proof) {
        this.proof = proof;
    }

}