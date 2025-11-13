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

package com.example.authorization.authorization.oid4vci.credentialidentifier.service;

import com.example.authorization.authorization.oid4vci.credentialidentifier.api.IssuerFeign;
import com.nimbusds.jose.shaded.gson.Gson;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIdentifierFeignService {

    private final IssuerFeign issuerFeign;

    public List<String> getCredentialIdentifier(String credentialConfigurationId) {
        // Settings for refresh token issuance or registered client..
//        List<String> degreeList = Collections.singletonList("UniversityDegree");

        try {
            // todo: For now, hardcoded based on the student ID VC...
            List<String> response = issuerFeign.sendCredentialIdentifier(credentialConfigurationId);

            log.info("Successfully received response from issuer server.");
            log.info("credential_identifiers: {}", new Gson().toJson(response));

            return response;
        } catch (FeignException e) {
            log.error("An error occurred while sending notification: {}", e.getMessage());
            throw new RuntimeException("Failed to get credential identifier from issuer", e);
        }
    }
}
