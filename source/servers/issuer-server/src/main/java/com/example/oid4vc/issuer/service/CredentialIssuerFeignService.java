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

import com.example.oid4vc.issuer.api.AuthorizationFeign;
import com.example.oid4vc.issuer.api.dto.PreAuthorizeResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuerFeignService {

    private final AuthorizationFeign authorizationFeign;

    public PreAuthorizeResponse getPreAuthorizedCode() {
        // refresh token / client setting..
        List<String> degreeList = Collections.singletonList("UniversityDegree");

        try {
            PreAuthorizeResponse response = authorizationFeign.sendPreAuthorize(degreeList);

            log.info("Successfully received response from authorization server.");
            log.info("code: {}, userPin: {}", response.getPreAuthorizedCode(), response.getUserPin());

            return response;
        } catch (FeignException e) {
            log.error("An error occurred while sending notification: {}", e.getMessage());
        }
        return null;
    }
}
