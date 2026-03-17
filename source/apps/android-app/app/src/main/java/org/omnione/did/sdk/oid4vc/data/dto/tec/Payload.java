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

public class Payload {

    @SerializedName("iss")
    private String issuer;

    @SerializedName("aud")
    private String audience;

    @SerializedName("nonce")
    private String nonce;

    @SerializedName("iat")
    private long issuedAt;

    @SerializedName("exp")
    private long expiration;

    @SerializedName("vp")
    private VerifiablePresentation vp;

    /**
     * Gets the issuer of the payload.
     * @return the issuer identifier.
     */
    public String getIssuer() { return issuer; }
    /**
     * Gets the audience of the payload.
     * @return the audience identifier.
     */
    public String getAudience() { return audience; }
    /**
     * Gets the nonce of the payload.
     * @return the nonce string.
     */
    public String getNonce() { return nonce; }
    /**
     * Gets the issuance time of the payload.
     * @return the issuedAt timestamp.
     */
    public long getIssuedAt() { return issuedAt; }
    /**
     * Gets the expiration time of the payload.
     * @return the expiration timestamp.
     */
    public long getExpiration() { return expiration; }
    /**
     * Gets the verifiable presentation in the payload.
     * @return the verifiable presentation object.
     */
    public VerifiablePresentation getVp() { return vp; }

    /**
     * Sets the issuer of the payload.
     * @param issuer the issuer identifier to set.
     */
    public void setIssuer(String issuer) { this.issuer = issuer; }
    /**
     * Sets the audience of the payload.
     * @param audience the audience identifier to set.
     */
    public void setAudience(String audience) { this.audience = audience; }
    /**
     * Sets the nonce of the payload.
     * @param nonce the nonce string to set.
     */
    public void setNonce(String nonce) { this.nonce = nonce; }
    /**
     * Sets the issuance time of the payload.
     * @param issuedAt the issuedAt timestamp to set.
     */
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }
    /**
     * Sets the expiration time of the payload.
     * @param expiration the expiration timestamp to set.
     */
    public void setExpiration(long expiration) { this.expiration = expiration; }
    /**
     * Sets the verifiable presentation in the payload.
     * @param vp the verifiable presentation object to set.
     */
    public void setVp(VerifiablePresentation vp) { this.vp = vp; }

    public static class VerifiablePresentation {
        @SerializedName("@context")
        private List<String> context;

        @SerializedName("type")
        private List<String> type;

        @SerializedName("verifiableCredential")
        private List<VerifiableCredential> verifiableCredential;

        @SerializedName("proof")
        private Proof vpProof;

        /**
         * Gets the context of the verifiable presentation.
         * @return the list of context strings.
         */
        public List<String> getContext() { return context; }
        /**
         * Gets the type of the verifiable presentation.
         * @return the list of type strings.
         */
        public List<String> getType() { return type; }
        /**
         * Gets the list of verifiable credentials in the presentation.
         * @return the list of verifiable credentials.
         */
        public List<VerifiableCredential> getVerifiableCredential() { return verifiableCredential; }
        /**
         * Gets the proof of the verifiable presentation.
         * @return the proof object.
         */
        public Proof getVpProof() { return vpProof; }

        /**
         * Sets the context of the verifiable presentation.
         * @param context the list of context strings to set.
         */
        public void setContext(List<String> context) { this.context = context; }
        /**
         * Sets the type of the verifiable presentation.
         * @param type the list of type strings to set.
         */
        public void setType(List<String> type) { this.type = type; }
        /**
         * Sets the list of verifiable credentials in the presentation.
         * @param verifiableCredential the list of verifiable credentials to set.
         */
        public void setVerifiableCredential(List<VerifiableCredential> verifiableCredential) {
            this.verifiableCredential = verifiableCredential;
        }
        /**
         * Sets the proof of the verifiable presentation.
         * @param vpProof the proof object to set.
         */
        public void setVpProof(Proof vpProof) { this.vpProof = vpProof; }
    }

    public static class VerifiableCredentialItem {
        @SerializedName("@context")
        private List<String> context;

        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private List<String> type;

        @SerializedName("issuer")
        private String issuer;

        @SerializedName("issuanceDate")
        private String issuanceDate;

        @SerializedName("credentialSubject")
        private CredentialSubject vcCredentialSubject;

        @SerializedName("proof")
        private Proof vcProof;

        /**
         * Gets the context of the verifiable credential item.
         * @return the list of context strings.
         */
        public List<String> getContext() { return context; }
        /**
         * Gets the ID of the verifiable credential item.
         * @return the credential ID.
         */
        public String getId() { return id; }
        /**
         * Gets the type of the verifiable credential item.
         * @return the list of type strings.
         */
        public List<String> getType() { return type; }
        /**
         * Gets the issuer of the verifiable credential item.
         * @return the issuer identifier.
         */
        public String getIssuer() { return issuer; }
        /**
         * Gets the issuance date of the verifiable credential item.
         * @return the issuance date string.
         */
        public String getIssuanceDate() { return issuanceDate; }
        /**
         * Gets the subject of the verifiable credential item.
         * @return the credential subject object.
         */
        public CredentialSubject getVcCredentialSubject() { return vcCredentialSubject; }
        /**
         * Gets the proof of the verifiable credential item.
         * @return the proof object.
         */
        public Proof getVcProof() { return vcProof; }

        /**
         * Sets the context of the verifiable credential item.
         * @param context the list of context strings to set.
         */
        public void setContext(List<String> context) { this.context = context; }
        /**
         * Sets the ID of the verifiable credential item.
         * @param id the credential ID to set.
         */
        public void setId(String id) { this.id = id; }
        /**
         * Sets the type of the verifiable credential item.
         * @param type the list of type strings to set.
         */
        public void setType(List<String> type) { this.type = type; }
        /**
         * Sets the issuer of the verifiable credential item.
         * @param issuer the issuer identifier to set.
         */
        public void setIssuer(String issuer) { this.issuer = issuer; }
        /**
         * Sets the issuance date of the verifiable credential item.
         * @param issuanceDate the issuance date string to set.
         */
        public void setIssuanceDate(String issuanceDate) { this.issuanceDate = issuanceDate; }
        /**
         * Sets the subject of the verifiable credential item.
         * @param vcCredentialSubject the credential subject object to set.
         */
        public void setVcCredentialSubject(CredentialSubject vcCredentialSubject) { this.vcCredentialSubject = vcCredentialSubject; }
        /**
         * Sets the proof of the verifiable credential item.
         * @param vcProof the proof object to set.
         */
        public void setVcProof(Proof vcProof) { this.vcProof = vcProof; }
    }

    public static class CredentialSubject {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("degree")
        private String degree;

        /**
         * Gets the ID of the credential subject.
         * @return the subject ID.
         */
        public String getId() { return id; }
        /**
         * Gets the name of the credential subject.
         * @return the subject name.
         */
        public String getName() { return name; }
        /**
         * Gets the degree of the credential subject.
         * @return the subject degree.
         */
        public String getDegree() { return degree; }

        /**
         * Sets the ID of the credential subject.
         * @param id the subject ID to set.
         */
        public void setId(String id) { this.id = id; }
        /**
         * Sets the name of the credential subject.
         * @param name the subject name to set.
         */
        public void setName(String name) { this.name = name; }
        /**
         * Sets the degree of the credential subject.
         * @param degree the subject degree to set.
         */
        public void setDegree(String degree) { this.degree = degree; }
    }

    public static class Proof {
        @SerializedName("type")
        private String type;

        @SerializedName("cryptosuite")
        private String cryptosuite;

        @SerializedName("created")
        private String created;

        @SerializedName("verificationMethod")
        private String verificationMethod;

        @SerializedName("proofPurpose")
        private String proofPurpose;

        @SerializedName("challenge")
        private String challenge;

        @SerializedName("domain")
        private String domain;

        @SerializedName("proofValue")
        private String proofValue;

        /**
         * Gets the type of the proof.
         * @return the proof type.
         */
        public String getType() { return type; }
        /**
         * Gets the cryptosuite of the proof.
         * @return the cryptosuite string.
         */
        public String getCryptosuite() { return cryptosuite; }
        /**
         * Gets the creation date of the proof.
         * @return the creation date string.
         */
        public String getCreated() { return created; }
        /**
         * Gets the verification method of the proof.
         * @return the verification method string.
         */
        public String getVerificationMethod() { return verificationMethod; }
        /**
         * Gets the purpose of the proof.
         * @return the proof purpose string.
         */
        public String getProofPurpose() { return proofPurpose; }
        /**
         * Gets the challenge associated with the proof.
         * @return the challenge string.
         */
        public String getChallenge() { return challenge; }
        /**
         * Gets the domain associated with the proof.
         * @return the domain string.
         */
        public String getDomain() { return domain; }
        /**
         * Gets the value of the proof.
         * @return the proof value string.
         */
        public String getProofValue() { return proofValue; }

        /**
         * Sets the type of the proof.
         * @param type the proof type to set.
         */
        public void setType(String type) { this.type = type; }
        /**
         * Sets the cryptosuite of the proof.
         * @param cryptosuite the cryptosuite string to set.
         */
        public void setCryptosuite(String cryptosuite) { this.cryptosuite = cryptosuite; }
        /**
         * Sets the creation date of the proof.
         * @param created the creation date string to set.
         */
        public void setCreated(String created) { this.created = created; }
        /**
         * Sets the verification method of the proof.
         * @param verificationMethod the verification method string to set.
         */
        public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
        /**
         * Sets the purpose of the proof.
         * @param proofPurpose the proof purpose string to set.
         */
        public void setProofPurpose(String proofPurpose) { this.proofPurpose = proofPurpose; }
        /**
         * Sets the challenge associated with the proof.
         * @param challenge the challenge string to set.
         */
        public void setChallenge(String challenge) { this.challenge = challenge; }
        /**
         * Sets the domain associated with the proof.
         * @param domain the domain string to set.
         */
        public void setDomain(String domain) { this.domain = domain; }
        /**
         * Sets the value of the proof.
         * @param proofValue the proof value string to set.
         */
        public void setProofValue(String proofValue) { this.proofValue = proofValue; }
    }
}
