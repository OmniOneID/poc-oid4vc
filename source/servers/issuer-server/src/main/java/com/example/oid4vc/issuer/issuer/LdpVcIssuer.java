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

//package com.example.oid4vc.issuer.issuer;
//
//import com.danubetech.dataintegrity.signer.Ed25519Signature2020LdSigner;
//import com.example.oid4vc.issuer.util.Constants;
//import foundation.identity.jsonld.JsonLDObject;
//import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
//import org.springframework.security.crypto.codec.Hex;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//import java.util.*;
//
//@Component("ldp-vc")
//public class LdpVcIssuer implements CredentialIssuer {
//
//    private static final byte[] seed32 = Hex.decode("984b589e121040156838303f107e13150be4a80fc5088ccba0b0bdc9b1d89090");
//    private static final URI verificationMethod = URI.create("did:example:123#key-1");
//
//    @Override
//    public Object issue(String userId, String credentialType, String cNonce) {
//        try {
//            Map<String, Object> claims = getClaimsForUser(userId, credentialType);
//
//            Map<String, Object> vcMap = new LinkedHashMap<>();
//            vcMap.put("@context", Arrays.asList(Constants.VC_CONTEXT, "https://w3id.org/security/suites/ed25519-2020/v1"));
//            vcMap.put("id", "urn:uuid:" + UUID.randomUUID().toString());
//            vcMap.put("type", Arrays.asList(Constants.VC_TYPE_VERIFIABLE_CREDENTIAL, "VerifiableId"));
//            vcMap.put("issuer", Constants.CREDENTIAL_ISSUER);
//            vcMap.put("issuanceDate", new Date().toInstant().toString());
//
//            Map<String, Object> credentialSubject = new LinkedHashMap<>(claims);
//            credentialSubject.put("id", userId);
//            vcMap.put("credentialSubject", credentialSubject);
//
//            JsonLDObject jsonLdObject = JsonLDObject.fromJsonObject(vcMap);
//
//            Ed25519PrivateKeyParameters priv = new Ed25519PrivateKeyParameters(seed32, 0);
//            byte[] pub32 = priv.generatePublicKey().getEncoded();
//
//            byte[] sk64 = new byte[64];
//            System.arraycopy(seed32, 0, sk64, 0, 32);
//            System.arraycopy(pub32, 0, sk64, 32, 32);
//            Ed25519Signature2020LdSigner signer = new Ed25519Signature2020LdSigner(sk64);
//            signer.setCreated(new Date());
//            signer.setProofPurpose("assertionMethod");
//            signer.setVerificationMethod(verificationMethod);
//
//            signer.setNonce(cNonce);
//
//            signer.sign(jsonLdObject);
//
//            return jsonLdObject.toJson(true);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to issue ldp-vc credential", e);
//        }
//    }
//
//    private Map<String, Object> getClaimsForUser(String userId, String credentialType) {
//        Map<String, Object> claims = new LinkedHashMap<>();
//        claims.put("degree", "Master of Science");
//        claims.put("major", "Computer Science");
//        return claims;
//    }
//}
