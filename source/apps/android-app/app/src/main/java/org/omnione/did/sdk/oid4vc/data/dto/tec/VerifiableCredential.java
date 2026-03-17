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

    /**
     * Gets the context of the verifiable credential.
     * @return the list of context strings.
     */
    public List<String> getContext() { return context; }
    /**
     * Gets the type of the verifiable credential.
     * @return the list of type strings.
     */
    public List<String> getType() { return type; }
    /**
     * Gets the ID of the verifiable credential.
     * @return the credential ID.
     */
    public String getId() { return id; }
    /**
     * Gets the issuer of the verifiable credential.
     * @return the issuer object.
     */
    public Issuer getIssuer() { return issuer; }
    /**
     * Gets the issuance date of the verifiable credential.
     * @return the issuance date string.
     */
    public String getIssuanceDate() { return issuanceDate; }
    /**
     * Gets the start date of the credential's validity.
     * @return the validFrom date string.
     */
    public String getValidFrom() { return validFrom; }
    /**
     * Gets the expiration date of the credential's validity.
     * @return the validUntil date string.
     */
    public String getValidUntil() { return validUntil; }
    /**
     * Gets the schema of the verifiable credential.
     * @return the credential schema object.
     */
    public CredentialSchema getCredentialSchema() { return credentialSchema; }
    /**
     * Gets the subject of the verifiable credential.
     * @return the credential subject object.
     */
    public CredentialSubject getCredentialSubject() { return credentialSubject; }
    /**
     * Gets the evidence associated with the verifiable credential.
     * @return the list of evidence objects.
     */
    public List<Evidence> getEvidence() { return evidence; }
    /**
     * Gets the proof of the verifiable credential.
     * @return the proof object.
     */
    public Proof getProof() { return proof; }
    /**
     * Gets the encoding of the verifiable credential.
     * @return the encoding string.
     */
    public String getEncoding() { return encoding; }
    /**
     * Gets the format version of the verifiable credential.
     * @return the format version string.
     */
    public String getFormatVersion() { return formatVersion; }
    /**
     * Gets the language of the verifiable credential.
     * @return the language string.
     */
    public String getLanguage() { return language; }

    public static class Issuer {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        /**
         * Gets the ID of the issuer.
         * @return the issuer ID.
         */
        public String getId() { return id; }
        /**
         * Gets the name of the issuer.
         * @return the issuer name.
         */
        public String getName() { return name; }
    }

    public static class CredentialSchema {
        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        /**
         * Gets the ID of the credential schema.
         * @return the schema ID.
         */
        public String getId() { return id; }
        /**
         * Gets the type of the credential schema.
         * @return the schema type.
         */
        public String getType() { return type; }
    }

    public static class CredentialSubject {
        @SerializedName("id")
        private String id;

        @SerializedName("claims")
        private List<Claim> claims;

        /**
         * Gets the ID of the credential subject.
         * @return the subject ID.
         */
        public String getId() { return id; }
        /**
         * Gets the claims associated with the credential subject.
         * @return the list of claims.
         */
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

        /**
         * Gets the caption of the claim.
         * @return the claim caption.
         */
        public String getCaption() { return caption; }
        /**
         * Gets the code of the claim.
         * @return the claim code.
         */
        public String getCode() { return code; }
        /**
         * Gets the format of the claim.
         * @return the claim format.
         */
        public String getFormat() { return format; }
        /**
         * Checks if the claim value should be hidden.
         * @return true if the value is hidden, false otherwise.
         */
        public boolean isHideValue() { return hideValue; }
        /**
         * Gets the type of the claim.
         * @return the claim type.
         */
        public String getType() { return type; }
        /**
         * Gets the value of the claim.
         * @return the claim value.
         */
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

        /**
         * Gets the type of the evidence.
         * @return the evidence type.
         */
        public String getType() { return type; }
        /**
         * Gets the verifier of the evidence.
         * @return the verifier ID.
         */
        public String getVerifier() { return verifier; }
        /**
         * Gets the evidence document.
         * @return the evidence document string.
         */
        public String getEvidenceDocument() { return evidenceDocument; }
        /**
         * Gets the subject presence information.
         * @return the subject presence string.
         */
        public String getSubjectPresence() { return subjectPresence; }
        /**
         * Gets the document presence information.
         * @return the document presence string.
         */
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

        /**
         * Gets the type of the proof.
         * @return the proof type.
         */
        public String getType() { return type; }
        /**
         * Gets the creation date of the proof.
         * @return the creation date string.
         */
        public String getCreated() { return created; }
        /**
         * Gets the purpose of the proof.
         * @return the proof purpose string.
         */
        public String getProofPurpose() { return proofPurpose; }
        /**
         * Gets the verification method of the proof.
         * @return the verification method string.
         */
        public String getVerificationMethod() { return verificationMethod; }
        /**
         * Gets the value of the proof.
         * @return the proof value string.
         */
        public String getProofValue() { return proofValue; }
        /**
         * Gets the list of proof values.
         * @return the list of proof value strings.
         */
        public List<String> getProofValueList() { return proofValueList; }
        /**
         * Gets the nonce associated with the proof.
         * @return the nonce string.
         */
        public String getNonce() { return nonce; }
    }
}
