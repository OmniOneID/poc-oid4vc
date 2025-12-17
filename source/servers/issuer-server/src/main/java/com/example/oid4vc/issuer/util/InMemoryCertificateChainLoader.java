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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 메모리 기반 인증서 체인 로더
 * 
 * 개발/테스트 환경에서 파일 I/O 없이 직접 인증서 제공
 */
public class InMemoryCertificateChainLoader implements CertificateChainLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(
        InMemoryCertificateChainLoader.class
    );
    
    private final byte[] dsCertificate;
    private final KeyPair keyPair;
    
    public InMemoryCertificateChainLoader(KeyPair keyPair) throws Exception {
        this.keyPair = keyPair;
        this.dsCertificate = TestCertificateGenerator.generateTestDSCertificate(keyPair);
        
        logger.info("✅ InMemoryCertificateChainLoader initialized");
        logger.info("   Mode: Development (in-memory certificate)");
        logger.info("   Certificate public key matches private key used for signing");
    }
    
    /**
     * x5chain 로드 (메모리에서)
     * 
     * @return [DS Certificate] (1개 요소)
     */
    @Override
    public List<byte[]> loadX5Chain() throws Exception {
        List<byte[]> x5chain = new ArrayList<>();
        x5chain.add(dsCertificate);
        
        logger.debug("Returning x5chain from memory (1 certificate: DS)");
        return x5chain;
    }
    
    /**
     * KeyPair 반환 (서명에 사용할 개인키)
     * 
     * @return KeyPair (공개키는 인증서에 포함, 개인키는 서명에 사용)
     */
    @Override
    public KeyPair getKeyPair() throws Exception {
        return keyPair;
    }
    
    /**
     * 인증서 정보 출력 (디버깅용)
     */
    @Override
    public void printCertificateInfo() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
            new ByteArrayInputStream(dsCertificate)
        );
        
        logger.info("==== Certificate Information ====");
        logger.info("Subject: {}", cert.getSubjectX500Principal());
        logger.info("Issuer: {}", cert.getIssuerX500Principal());
        logger.info("Valid From: {}", cert.getNotBefore());
        logger.info("Valid Until: {}", cert.getNotAfter());
        logger.info("Size: {} bytes", dsCertificate.length);
        logger.info("===================================");
    }
}
