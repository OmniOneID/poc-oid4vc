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

package com.example.oid4vc.issuer.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SharedStateService {

    private final Map<String, String> cNonceStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    public SharedStateService() throws JOSEException {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        this.signer = new MACSigner(secret);
        this.verifier = new MACVerifier(secret);

        users.put("user123", Map.of("name", "John Doe", "degree", "Master of Science"));
    }

    public Map<String, String> getCNonceStore() {
        return cNonceStore;
    }

    public Map<String, Map<String, Object>> getUsers() {
        return users;
    }

    public JWSSigner getSigner() {
        return signer;
    }

    public JWSVerifier getVerifier() {
        return verifier;
    }
}
