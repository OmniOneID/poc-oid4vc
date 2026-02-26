/*
 * Copyright 2026 OmniOne.
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



//package org.omnione.did.oid4vc.oid4vci.issuer;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
//import org.springframework.stereotype.Component;
//
//import java.util.Base64;
//import java.util.LinkedHashMap; // JSON/CBOR - LinkedHashMap
//import java.util.List;
//import java.util.Map;
//
//@Component("mso-mdoc")
//public class MdocIssuer implements CredentialIssuer {
//
//    private final ObjectMapper cborMapper;
//
//    public MdocIssuer() {
//        this.cborMapper = new ObjectMapper(new CBORFactory());
//    }
//
//    @Override
//    public Object issue(String userId, String credentialType, String cNonce) {
//        try {
//            Map<String, Object> claims = getClaimsForUser(userId, credentialType);
//
//            // Create a basic mDoc-like structure from the fetched claims
//            Map<String, Object> mdoc = Map.of(
//                    "docType", "org.iso.18013.5.1.mDL",
//                    "issuerSigned", Map.of(
//                            "nameSpaces", Map.of(
//                                    "org.iso.18013.5.1", claims
//                            )
//                    )
//            );
//
//            // Convert the structure to CBOR
//            byte[] cborBytes = cborMapper.writeValueAsBytes(mdoc);
//
//            // Return the CBOR as a Base64 encoded string
//            return Base64.getEncoder().encodeToString(cborBytes);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Failed to encode credential to CBOR", e);
//        }
//    }
//
//    private Map<String, Object> getClaimsForUser(String userId, String credentialType) {
//        // credentialType is "org.iso.18013.5.1.mDL" sample
//        Map<String, Object> claims = new LinkedHashMap<>();
//        claims.put("family_name", "Kim");
//        claims.put("given_name", "Raon");
//        claims.put("birth_date", "2000-01-01");
//        claims.put("issue_date", "2023-08-15");
//        claims.put("expiry_date", "2033-08-14");
//        claims.put("document_number", "25-123456-78");
//        claims.put("driving_privileges", List.of("B1", "B2"));
//        return claims;
//    }
//}
