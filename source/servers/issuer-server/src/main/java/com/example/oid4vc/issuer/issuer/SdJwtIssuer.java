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

package com.example.oid4vc.issuer.issuer;

import com.example.oid4vc.issuer.service.SharedStateService;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.omnione.did.oid4vc.oid4vci.core.OID4VCIssuer;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

//@Component("VerifiableIdSD")
public class SdJwtIssuer implements CredentialIssuer {

    private final SharedStateService sharedStateService;

    public SdJwtIssuer(SharedStateService sharedStateService) {
        this.sharedStateService = sharedStateService;
    }

        @Override
        public Object issue(String userId, String credentialType, String cNonce) {
            // Claims information to be included in the VC should be retrieved based on userId and credentialType (typically from a DB or other services)
            //todo: Should use a real key
            String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
            String PUBLIC_KEY= "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";


            PrivateKey issuerPrivateKey = getPrivateKeyObject(Base64.decode(PRIVATE_KEY));
            PublicKey issuerPublicKey = getPublicKeyObject(unCompressPublicKey(Base64.decode(PUBLIC_KEY)));
            PrivateKey holderPrivateKey = getPrivateKeyObject(Base64.decode(PRIVATE_KEY));
            PublicKey holderPublicKey = getPublicKeyObject(unCompressPublicKey(Base64.decode(PUBLIC_KEY)));

            OID4VCIssuer issuer = new OID4VCIssuer(
                    issuerPrivateKey,
                    "did:omn:issuer"
            );

            Map<String, Object> identityInfo = getClaimsForUser(userId, credentialType);
            String identityVC = issuer.issueCredential(
                    "https://credentials.gov.kr/identity_credential",
                    identityInfo,
                    holderPublicKey
            );
            return identityVC;
        }

    private PrivateKey getPrivateKeyObject(byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] unCompressPublicKey(byte[] compressedPublicKey) {
        Security.addProvider(new BouncyCastleProvider());
        byte[] uncompressPublicKey = null;
        ECNamedCurveParameterSpec ecParams = ECNamedCurveTable.getParameterSpec("Secp256r1");
        ECPoint uncompressedPoint = ecParams.getCurve().decodePoint(compressedPublicKey);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(uncompressedPoint, ecParams);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            uncompressPublicKey = keyFactory.generatePublic(pubKeySpec).getEncoded();
            System.out.println("pubKey : " + Base64.toBase64String(uncompressPublicKey));
            return uncompressPublicKey;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | java.security.spec.InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    //sd jwt
    private Map<String, Object> getClaimsForUser(String userId, String credentialType) {
//        if (!Constants.VC_TYPE_UNIVERSITY_DEGREE.equals(credentialType) && !"jwt_vc".equals(credentialType)) {
//            throw new IllegalArgumentException("Unsupported credential type for this issuer: " + credentialType);
//        }
//
//        Map<String, Object> userData = sharedStateService.getUsers().get(userId);
//        if (userData == null) {
//            throw new IllegalArgumentException("User not found: " + userId);
//        }

        return Map.of(
                "given_name", "Raon",
                "family_name", "Kim",
                "birth_date", "1990-01-01",
                "gender", "male",
                "nationality", "KR",
                "id_number", "900101-1234567",
                "address", Map.of(
                        "country", "Republic of Korea",
                        "region", "Seoul",
                        "locality", "Gangnam-gu",
                        "street_address", "Teheran-ro 123"
                ),
                "phone_number", "+82-10-1234-5678",
                "email", "raonkim@raoncorp.com"
        );
    }
}
