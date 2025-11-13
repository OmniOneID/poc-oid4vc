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

// todo: For TEC, delete later?
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

    public String getIssuer() { return issuer; }
    public String getAudience() { return audience; }
    public String getNonce() { return nonce; }
    public long getIssuedAt() { return issuedAt; }
    public long getExpiration() { return expiration; }
    public VerifiablePresentation getVp() { return vp; }

    public void setIssuer(String issuer) { this.issuer = issuer; }
    public void setAudience(String audience) { this.audience = audience; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }
    public void setExpiration(long expiration) { this.expiration = expiration; }
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

        public List<String> getContext() { return context; }
        public List<String> getType() { return type; }
        public List<VerifiableCredential> getVerifiableCredential() { return verifiableCredential; }
        public Proof getVpProof() { return vpProof; }

        public void setContext(List<String> context) { this.context = context; }
        public void setType(List<String> type) { this.type = type; }
        public void setVerifiableCredential(List<VerifiableCredential> verifiableCredential) {
            this.verifiableCredential = verifiableCredential;
        }
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


        public List<String> getContext() { return context; }
        public String getId() { return id; }
        public List<String> getType() { return type; }
        public String getIssuer() { return issuer; }
        public String getIssuanceDate() { return issuanceDate; }
        public CredentialSubject getVcCredentialSubject() { return vcCredentialSubject; }
        public Proof getVcProof() { return vcProof; }

        public void setContext(List<String> context) { this.context = context; }
        public void setId(String id) { this.id = id; }
        public void setType(List<String> type) { this.type = type; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public void setIssuanceDate(String issuanceDate) { this.issuanceDate = issuanceDate; }
        public void setVcCredentialSubject(CredentialSubject vcCredentialSubject) { this.vcCredentialSubject = vcCredentialSubject; }
        public void setVcProof(Proof vcProof) { this.vcProof = vcProof; }
    }

    public static class CredentialSubject {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("degree")
        private String degree;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDegree() { return degree; }

        public void setId(String id) { this.id = id; }
        public void setName(String name) { this.name = name; }
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

        public String getType() { return type; }
        public String getCryptosuite() { return cryptosuite; }
        public String getCreated() { return created; }
        public String getVerificationMethod() { return verificationMethod; }
        public String getProofPurpose() { return proofPurpose; }
        public String getChallenge() { return challenge; }
        public String getDomain() { return domain; }
        public String getProofValue() { return proofValue; }

        public void setType(String type) { this.type = type; }
        public void setCryptosuite(String cryptosuite) { this.cryptosuite = cryptosuite; }
        public void setCreated(String created) { this.created = created; }
        public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
        public void setProofPurpose(String proofPurpose) { this.proofPurpose = proofPurpose; }
        public void setChallenge(String challenge) { this.challenge = challenge; }
        public void setDomain(String domain) { this.domain = domain; }
        public void setProofValue(String proofValue) { this.proofValue = proofValue; }
    }
}