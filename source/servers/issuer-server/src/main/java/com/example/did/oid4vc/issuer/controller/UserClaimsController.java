/*
 * Copyright 2026 OmniOne.
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

package com.example.did.oid4vc.issuer.controller;

import org.omnione.did.oid4vc.oid4vci.config.IssuerSdkProperties;
import org.omnione.did.oid4vc.oid4vci.service.UserClaimsStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
public class UserClaimsController {

    private final UserClaimsStore userClaimsStore;
    private final IssuerSdkProperties issuerSdkProperties;

    @GetMapping("/claims-page")
    public String claimsPage(Model model) {
        Set<String> identifiers = issuerSdkProperties.getIdentifiersByConfigId("VerifiableIdSD");
        List<String> sortedIdentifiers = new ArrayList<>(identifiers);
        Collections.sort(sortedIdentifiers);
        model.addAttribute("credentialTypes", sortedIdentifiers);
        return "claims-editor";
    }

    @PostMapping("/api/claims/save")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveClaims(@RequestBody ClaimsRequest request) {
        userClaimsStore.saveClaims(request.getUserId(), request.getCredentialType(), request.getClaims());
        return ResponseEntity.ok(Map.of("message", "Claims saved successfully"));
    }

    @GetMapping("/api/claims/list")
    @ResponseBody
    public ResponseEntity<Map<String, Map<String, Object>>> listAllClaims() {
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .body(userClaimsStore.getAllEntries());
    }

    @GetMapping("/api/claims/get")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getClaims(@RequestParam String userId, @RequestParam String credentialType) {
        Map<String, Object> claims = userClaimsStore.getClaims(userId, credentialType);
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .body(claims);
    }

    @lombok.Data
    public static class ClaimsRequest {
        private String userId;
        private String credentialType;
        private Map<String, Object> claims;
    }
}
