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

package com.example.authorization.authorization.oid4vci.preauthorized.controller;

import com.example.authorization.authorization.oid4vci.preauthorized.dto.PreAuthorizedCodeResponse;
import com.example.authorization.authorization.oid4vci.preauthorized.dto.PreAuthorizedCode;
import com.example.authorization.authorization.oid4vci.preauthorized.service.PreAuthorizationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class PreAuthorizationController {

    private final PreAuthorizationService preAuthorizationService;

    public PreAuthorizationController(PreAuthorizationService preAuthorizationService) {
        this.preAuthorizationService = preAuthorizationService;
    }

    @PostMapping("/pre-authorize")
    public PreAuthorizedCodeResponse preAuthorize(@RequestBody Set<String> scopes, @RequestParam(required = false) String cNonce) {
        PreAuthorizedCode preAuthorizedCode = preAuthorizationService.create(scopes, cNonce);
        return new PreAuthorizedCodeResponse(
                preAuthorizedCode.getValue(),
                preAuthorizationService.getExpiresIn(),
                preAuthorizedCode.getUserPin()
        );
    }
}
