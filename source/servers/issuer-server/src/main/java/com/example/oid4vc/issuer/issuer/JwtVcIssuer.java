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
//import com.example.oid4vc.issuer.service.SharedStateService;
//import com.example.oid4vc.issuer.util.Constants;
//import com.nimbusds.jose.JOSEException;
//import com.nimbusds.jose.JWSAlgorithm;
//import com.nimbusds.jose.JWSHeader;
//import com.nimbusds.jwt.JWTClaimsSet;
//import com.nimbusds.jwt.SignedJWT;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//import java.util.Map;
//import java.util.UUID;
//
//@Component("jwt_vc")
//public class JwtVcIssuer implements CredentialIssuer {
//
//    private final SharedStateService sharedStateService;
//
//    public JwtVcIssuer(SharedStateService sharedStateService) {
//        this.sharedStateService = sharedStateService;
//    }
//
//    @Override
//    public Object issue(String userId, String credentialType, String cNonce) {
//        try {
//            Map<String, Object> credentialSubjectClaims = getClaimsForUser(userId, credentialType);
//
//            JWTClaimsSet vcClaims = new JWTClaimsSet.Builder()
//                    .issuer(Constants.CREDENTIAL_ISSUER)
//                    .subject(userId)
//                    .expirationTime(new Date(new Date().getTime() + 365L * 24 * 60 * 60 * 1000))
//                    .jwtID(UUID.randomUUID().toString())
//                    .claim("vc", Map.of(
//                            "@context", new String[]{Constants.VC_CONTEXT},
//                            "type", new String[]{Constants.VC_TYPE_VERIFIABLE_CREDENTIAL, Constants.VC_TYPE_UNIVERSITY_DEGREE},
//                            "credentialSubject", credentialSubjectClaims
//                    ))
//                    .claim("nonce", cNonce)
//                    .build();
//
//            SignedJWT signedVC = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), vcClaims);
//            signedVC.sign(sharedStateService.getSigner());
//
//            return signedVC.serialize();
//        } catch (JOSEException e) {
//            throw new RuntimeException("Failed to sign the VC JWT", e);
//        }
//    }
//
//    private Map<String, Object> getClaimsForUser(String userId, String credentialType) {
//        if (!Constants.VC_TYPE_UNIVERSITY_DEGREE.equals(credentialType) && !"jwt_vc".equals(credentialType)) {
//            throw new IllegalArgumentException("Unsupported credential type for this issuer: " + credentialType);
//        }
//
//        Map<String, Object> userData = sharedStateService.getUsers().get(userId);
//        if (userData == null) {
//            throw new IllegalArgumentException("User not found: " + userId);
//        }
//
//        return Map.of(
//                "name", userData.get("name"),
//                "degree", userData.get("degree")
//        );
//    }
//}
