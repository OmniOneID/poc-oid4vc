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

package org.omnione.did.sdk.oid4vc.data.dto.tec;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class VerifiableCredential {

    @SerializedName("@context")
    private List<String> context;

    @SerializedName("type")
    private List<String> type;

    @SerializedName("id")
    private String id;

    @SerializedName("issuer")
    private Issuer issuer;

    @SerializedName("issuanceDate")
    private String issuanceDate;

    @SerializedName("validFrom")
    private String validFrom;

    @SerializedName("validUntil")
    private String validUntil;

    @SerializedName("credentialSchema")
    private CredentialSchema credentialSchema;

    @SerializedName("credentialSubject")
    private CredentialSubject credentialSubject;

    @SerializedName("evidence")
    private List<Evidence> evidence;

    @SerializedName("proof")
    private Proof proof;

    @SerializedName("encoding")
    private String encoding;

    @SerializedName("formatVersion")
    private String formatVersion;

    @SerializedName("language")
    private String language;


    public List<String> getContext() { return context; }
    public List<String> getType() { return type; }
    public String getId() { return id; }
    public Issuer getIssuer() { return issuer; }
    public String getIssuanceDate() { return issuanceDate; }
    public String getValidFrom() { return validFrom; }
    public String getValidUntil() { return validUntil; }
    public CredentialSchema getCredentialSchema() { return credentialSchema; }
    public CredentialSubject getCredentialSubject() { return credentialSubject; }
    public List<Evidence> getEvidence() { return evidence; }
    public Proof getProof() { return proof; }
    public String getEncoding() { return encoding; }
    public String getFormatVersion() { return formatVersion; }
    public String getLanguage() { return language; }

    public static class Issuer {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
    }

    public static class CredentialSchema {
        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        public String getId() { return id; }
        public String getType() { return type; }
    }

    public static class CredentialSubject {
        @SerializedName("id")
        private String id;

        @SerializedName("claims")
        private List<Claim> claims;

        public String getId() { return id; }
        public List<Claim> getClaims() { return claims; }
    }

    public static class Claim {
        @SerializedName("caption")
        private String caption;

        @SerializedName("code")
        private String code;

        @SerializedName("format")
        private String format;

        @SerializedName("hideValue")
        private boolean hideValue;

        @SerializedName("type")
        private String type;

        @SerializedName("value")
        private String value;

        public String getCaption() { return caption; }
        public String getCode() { return code; }
        public String getFormat() { return format; }
        public boolean isHideValue() { return hideValue; }
        public String getType() { return type; }
        public String getValue() { return value; }
    }

    public static class Evidence {
        @SerializedName("type")
        private String type;

        @SerializedName("verifier")
        private String verifier;

        @SerializedName("evidenceDocument")
        private String evidenceDocument;

        @SerializedName("subjectPresence")
        private String subjectPresence;

        @SerializedName("documentPresence")
        private String documentPresence;

        public String getType() { return type; }
        public String getVerifier() { return verifier; }
        public String getEvidenceDocument() { return evidenceDocument; }
        public String getSubjectPresence() { return subjectPresence; }
        public String getDocumentPresence() { return documentPresence; }
    }

    public static class Proof {
        @SerializedName("type")
        private String type;

        @SerializedName("created")
        private String created;

        @SerializedName("proofPurpose")
        private String proofPurpose;

        @SerializedName("verificationMethod")
        private String verificationMethod;

        @SerializedName("proofValue")
        private String proofValue;

        @SerializedName("proofValueList")
        private List<String> proofValueList;

        @SerializedName("nonce")
        private String nonce;

        public String getType() { return type; }
        public String getCreated() { return created; }
        public String getProofPurpose() { return proofPurpose; }
        public String getVerificationMethod() { return verificationMethod; }
        public String getProofValue() { return proofValue; }
        public List<String> getProofValueList() { return proofValueList; }
        public String getNonce() { return nonce; }
    }
}