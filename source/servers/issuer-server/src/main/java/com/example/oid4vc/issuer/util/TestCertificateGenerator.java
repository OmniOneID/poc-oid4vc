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

package com.example.oid4vc.issuer.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 테스트용 인증서와 개인키 생성기
 * 
 * EC P-256 테스트 인증서와 개인키를 런타임에 생성
 * 개발/테스트 환경에서만 사용
 */
public class TestCertificateGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCertificateGenerator.class);
    
    /**
     * Test DS KeyPair 생성
     * EC P-256 KeyPair를 새로 생성
     * 
     * @return EC P-256 KeyPair
     */
    public static KeyPair generateTestDSKeyPair() throws Exception {
        logger.info("🔧 Generating test DS KeyPair (EC P-256)");
        
        Security.addProvider(new BouncyCastleProvider());
        
        // EC P-256 KeyPair 생성
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC", "BC");
        ECNamedCurveParameterSpec ecParams = ECNamedCurveTable.getParameterSpec("P-256");
        keyPairGen.initialize(ecParams);
        
        KeyPair keyPair = keyPairGen.generateKeyPair();
        
        logger.info("✅ Generated test DS KeyPair");
        logger.info("   Algorithm: EC P-256");
        logger.info("   Private Key: Ready for signing");
        logger.info("   Public Key: Ready for certificate");
        
        return keyPair;
    }
    
    /**
     * Test DS 인증서 생성
     * 주어진 KeyPair로 자체 서명된 X.509 인증서 생성
     * 
     * @param keyPair EC P-256 KeyPair
     * @return DER 형식 인증서 바이트
     */
    public static byte[] generateTestDSCertificate(KeyPair keyPair) throws Exception {
        logger.info("🔧 Generating test DS certificate");
        
        Security.addProvider(new BouncyCastleProvider());
        
        // 인증서 정보
        X500Name subjectName = new X500Name(
            "CN=Test mDL DS, O=Test Organization, C=KR"
        );
        X500Name issuerName = subjectName;  // 자체 서명
        
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        
        // 유효 기간
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notBefore = now;
        LocalDateTime notAfter = now.plusYears(10);
        
        Date notBeforeDate = Date.from(notBefore.atZone(ZoneId.systemDefault()).toInstant());
        Date notAfterDate = Date.from(notAfter.atZone(ZoneId.systemDefault()).toInstant());
        
        // X.509 인증서 빌더
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
            keyPair.getPublic().getEncoded()
        );
        
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            notBeforeDate,
            notAfterDate,
            subjectName,
            keyPair.getPublic()
        );
        
        // 서명 생성 (SHA256withECDSA)
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());
        
        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate certificate = new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);
        
        // DER 인코딩
        byte[] certBytes = certificate.getEncoded();
        
        logger.info("✅ Generated test DS certificate");
        logger.info("   Subject: {}", certificate.getSubjectX500Principal());
        logger.info("   Serial: {}", certificate.getSerialNumber());
        logger.info("   Valid From: {}", certificate.getNotBefore());
        logger.info("   Valid Until: {}", certificate.getNotAfter());
        logger.info("   Size: {} bytes", certBytes.length);
        
        return certBytes;
    }
}
