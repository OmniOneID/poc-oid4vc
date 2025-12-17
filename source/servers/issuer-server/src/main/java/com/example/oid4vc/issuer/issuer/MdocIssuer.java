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

import com.example.oid4vc.issuer.util.*;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * mDL Issuer - IA(Issuing Authority) 측 구현
 * 
 * 책임:
 * ✅ IssuerSigned만 생성 (MSO, Digest, COSE Sign1 서명)
 * ❌ DeviceSigned는 생성하지 않음 (Device 책임)
 * 
 * 표준 근거:
 * - Section 9.1.2: Issuer Data Authentication (IA 책임)
 * - Section 9.1.3: mdoc Authentication (Device 책임)
 * - Section 9.1.3.4: "The mdoc shall generate this structure"
 */
@Component("org.iso.18013.5.1.mDL")
public class MdocIssuer implements CredentialIssuer {
    
    private static final Logger logger = LoggerFactory.getLogger(MdocIssuer.class);
    
    private final PrivateKey issuerPrivateKey;
    private final PublicKey issuerPublicKey;
    private final PublicKey devicePublicKey;
    
    public MdocIssuer() {
        try {
            // BouncyCastle 제공자 등록
            Security.addProvider(new BouncyCastleProvider());
            
            // TODO: Properties에서 키 경로 로드
            String ISSUER_PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
            String ISSUER_PUBLIC_KEY = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";
            
            this.issuerPrivateKey = getPrivateKeyObject(Base64.decode(ISSUER_PRIVATE_KEY));
            this.issuerPublicKey = getPublicKeyObject(
                unCompressPublicKey(Base64.decode(ISSUER_PUBLIC_KEY))
            );
            
            // Device의 공개키는 IA가 관리
            // (개인키는 Device만 관리, 표준 요구사항)
            this.devicePublicKey = this.issuerPublicKey;  // 임시 (실제는 다른 키)
            
            logger.info("MdocIssuer initialized successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MdocIssuer", e);
        }
    }
    
    /**
     * mDL 발급
     * 
     * IA의 책임:
     * 1. mDL 데이터 준비
     * 2. IssuerSignedItem 배열 생성
     * 3. MSO 생성 및 COSE Sign1 서명
     * 4. IssuerSigned 완성
     * 5. IssuerSigned만 포함된 Document 반환
     * 
     * Device의 책임 (별도 구현):
     * 1. DeviceAuth 생성 (Device 개인키 사용)
     * 2. DeviceSigned 생성
     * 3. Document에 DeviceSigned 추가
     * 
     * Section 9.1.2, 9.1.3 참고
     */
    @Override
    public Object issue(String userId, String credentialType, String cNonce) {
        try {
            logger.info("Issuing mDL for userId: {}", userId);
            
            // ============================================
            // IA 책임 구간: IssuerSigned 생성
            // ============================================
            
            // 1️⃣ mDL 데이터 준비 (Section 7.2)
            Map<String, Object> claims = getClaimsForUser(userId, credentialType);
            byte[] portraitBytes = loadPortrait(userId);
            
            MdocNamespaceBuilder namespaceBuilder = new MdocNamespaceBuilder()
                .withClaims(claims)
                .withIssueDate(LocalDate.now())
                .withExpiryDate(LocalDate.now().plusYears(10))
                .withIssuingCountry("KR")
                .withIssuingAuthority("RoK Ministry of Interior")
                .withUnDistinguishingSign("ROK")
                .withPortrait(portraitBytes);
            
            Map<String, Object> mdocNamespace = namespaceBuilder.build();
            logger.debug("Built mDL namespace with {} elements", mdocNamespace.size());
            
            // 2️⃣ IssuerSignedItem 배열 생성 (Section 8.3.2.1.2.2)
            List<Map<String, Object>> issuerSignedItems = 
                IssuerSignedItemBuilder.buildItems(mdocNamespace);
            logger.debug("Built {} IssuerSignedItems", issuerSignedItems.size());
            
            // 3️⃣ Digest 계산 (Section 9.1.2.5)
            Map<Integer, byte[]> valueDigests = 
                IssuerSignedItemBuilder.calculateDigests(issuerSignedItems);
            logger.debug("Calculated {} digests for mdoc namespace", valueDigests.size());
            
            // 4️⃣ MSO 생성 (Section 9.1.2.4)
            Map<String, Object> mso = MobileSecurityObjectBuilder.buildMso(
                valueDigests,
                devicePublicKey,
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(10)
            );
            logger.debug("Built Mobile Security Object (MSO)");
            
            // 5️⃣ MSO를 COSE Sign1로 서명 (IA의 개인키 사용)
            // Section 9.1.2.4: "issuing authority infrastructure then digitally signs 
            // the MSO using a private key that is kept secret by [the IA]"
            byte[] msoBytes = CborHelper.encode(mso);
            byte[] issuerAuthCose = CoseHelper.coseSign1Sign(msoBytes, issuerPrivateKey);
            logger.debug("Generated IssuerAuth (COSE Sign1) with size: {} bytes", 
                issuerAuthCose.length);
            
            // 6️⃣ IssuerSigned만 포함된 Document 생성
            Map<String, Object> document = MdocDocumentBuilder.buildDocument(
                issuerSignedItems,
                issuerAuthCose
            );
            logger.debug("Completed IssuerSigned structure");
            
            // ============================================
            // IA 책임 구간 종료
            // ============================================
            
            // 7️⃣ CBOR 인코딩 및 Base64 변환
            byte[] mdocBytes = CborHelper.encode(document);
            String result = Base64.toBase64String(mdocBytes);
            
            logger.info("Successfully issued mDL (size: {} bytes)", mdocBytes.length);
            logger.info("✅ Document contains IssuerSigned (IA responsibility)");
            logger.info("⏳ Device will add DeviceSigned (Device responsibility)");
            logger.info("📋 See ISO/IEC 18013-5:2021 Section 9.1.3.4");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to issue mDL", e);
            throw new RuntimeException("Failed to issue mDL: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> getClaimsForUser(String userId, String credentialType) {
        return Map.of(
            "given_name", "Raon",
            "family_name", "Kim",
            "birth_date", "1990-01-01",
            "gender", "male",
            "nationality", "KR",
            "id_number", "900101-1234567"
        );
    }
    
    private byte[] loadPortrait(String userId) {
        // TODO: 실제로는 파일 또는 DB에서 로드
        // JPEG 매직 넘버: 0xFF 0xD8
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 
                          0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
    }
    
    private PrivateKey getPrivateKeyObject(byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key", e);
        }
    }
    
    private PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key", e);
        }
    }
    
    private byte[] unCompressPublicKey(byte[] compressedPublicKey) {
        try {
            ECNamedCurveParameterSpec ecParams = ECNamedCurveTable.getParameterSpec("Secp256r1");
            ECPoint uncompressedPoint = ecParams.getCurve().decodePoint(compressedPublicKey);
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(uncompressedPoint, ecParams);
            
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            return keyFactory.generatePublic(pubKeySpec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to uncompress public key", e);
        }
    }
}
