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

import java.security.KeyPair;
import java.util.List;

/**
 * 인증서 체인 로더 인터페이스
 * 
 * 개발: 메모리 기반 구현
 * 프로덕션: 파일 기반 구현
 */
public interface CertificateChainLoader {
    
    /**
     * x5chain 로드 (DS Certificate 1개 포함)
     * 
     * @return [DS Certificate] (1개 요소)
     */
    List<byte[]> loadX5Chain() throws Exception;
    
    /**
     * KeyPair 반환 (서명에 사용할 개인키)
     * 
     * @return KeyPair (공개키는 인증서에 포함, 개인키는 서명에 사용)
     */
    KeyPair getKeyPair() throws Exception;
    
    /**
     * 인증서 정보 출력 (디버깅용)
     */
    void printCertificateInfo() throws Exception;
}
