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

package com.example.oid4vc.issuer.config;

import com.example.oid4vc.issuer.util.CertificateChainLoader;
import com.example.oid4vc.issuer.util.InMemoryCertificateChainLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;

/**
 * 인증서 체인 로더 설정
 * 
 * 개발/테스트 환경: 메모리 기반 (동적 생성)
 */
@Configuration
public class CertificateLoaderConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(
        CertificateLoaderConfiguration.class
    );
    
    /**
     * 개발/테스트 환경: 메모리 기반 인증서 로더
     */
    @Bean
    public CertificateChainLoader certificateChainLoader() throws Exception {
        logger.info("==========================================");
        logger.info("📜 Certificate Configuration");
        logger.info("==========================================");
        logger.info("🔧 Mode: IN-MEMORY (Development/Test)");
        logger.info("✅ Generating test certificate...");
        
        return new InMemoryCertificateChainLoader(
            com.example.oid4vc.issuer.util.TestCertificateGenerator.generateTestDSKeyPair()
        );
    }
}
