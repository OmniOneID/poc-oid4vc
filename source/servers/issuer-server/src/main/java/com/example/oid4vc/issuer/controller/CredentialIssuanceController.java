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

package com.example.oid4vc.issuer.controller;

import com.example.oid4vc.issuer.dto.credential.CredentialRequest;
import com.example.oid4vc.issuer.dto.credential.CredentialResponse;
import com.example.oid4vc.issuer.dto.credential.DeferredCredentialRequest;
import com.example.oid4vc.issuer.dto.credential.DeferredIssuanceResponse;
import com.example.oid4vc.issuer.dto.credentialoffer.CredentialOfferRequest;
import com.example.oid4vc.issuer.dto.credentialoffer.CredentialOfferResponse;
import com.example.oid4vc.issuer.api.dto.PreAuthorizeResponse;
import com.example.oid4vc.issuer.dto.credentialoffer.TestCredentialOfferResponse;
import com.example.oid4vc.issuer.dto.metadata.IssuerMetadataResponse;
import com.example.oid4vc.issuer.dto.nonce.NonceResponse;
import com.example.oid4vc.issuer.dto.notification.NotificationRequest;
import com.example.oid4vc.issuer.service.CredentialIssuerFeignService;
import com.example.oid4vc.issuer.service.CredentialService;
import com.example.oid4vc.issuer.temp.OfferStore;
import com.example.oid4vc.issuer.util.Constants;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class CredentialIssuanceController {

    private final OfferStore offerStore; // temp

    private final CredentialService credentialService;

    private final CredentialIssuerFeignService credentialIssuerFeignService;

    public CredentialIssuanceController(CredentialService credentialService, CredentialIssuerFeignService credentialIssuerFeignService, OfferStore offerStore) {
        this.credentialService = credentialService;
        this.credentialIssuerFeignService = credentialIssuerFeignService;
        this.offerStore = offerStore;  // temp
    }

    @GetMapping("/credential-offer/{request_id}")
    @ResponseBody
    public CredentialOfferResponse getCredentialOffer(
            @PathVariable String request_id) {
        CredentialOfferRequest request = new CredentialOfferRequest();
        request.setUserId("test");

        CredentialOfferResponse response = new CredentialOfferResponse();
        if (request_id.startsWith("p")) {
            PreAuthorizeResponse storedPreAuthorizeResponse = offerStore.consume(request_id);

            if (storedPreAuthorizeResponse == null) {
                throw new IllegalStateException("Invalid or expired offer request.");
            }
            log.info("Retrieved from session: requestId={}, preAuthorizedCode={}", request_id, storedPreAuthorizeResponse.getPreAuthorizedCode());
            response = credentialService.createCredentialOffer(request, storedPreAuthorizeResponse, Constants.PRE_AUTHORIZED_CODE_TYPE);
        } else if(request_id.startsWith("a")) {
            response = credentialService.createCredentialOffer(request, Constants.AUTHORIZATION_CODE_TYPE);
        }
        log.info("Retrieving authorization request for request_id: {}", request_id);

        log.info("createCredentialOffer response : {}", new Gson().toJson(response));

        return response;

    }

    @GetMapping("/.well-known/openid-credential-issuer")
    public IssuerMetadataResponse getIssuerMetadata() {
        log.info("getIssuerMetadata request");
        IssuerMetadataResponse response = credentialService.getIssuerMetadata();
        log.info("getIssuerMetadata response : {}", new Gson().toJson(response));
        return response;
    }

    @PostMapping("/credential")
    public ResponseEntity<Object> issueCredential(@RequestBody CredentialRequest request, @AuthenticationPrincipal Jwt accessToken) {
        log.info("credential request : {}", new Gson().toJson(request));
        Object response = credentialService.issueCredential(request, accessToken);
        log.info("credential response : {}", new Gson().toJson(response));
        if (response instanceof DeferredIssuanceResponse) {
            return ResponseEntity.accepted().body(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/deferred_credential")
    public CredentialResponse getDeferredCredential(@RequestBody DeferredCredentialRequest request, @AuthenticationPrincipal Jwt accessToken) {
        return credentialService.getDeferredCredential(request.getTransactionId(), accessToken);
    }

    @PostMapping("/nonce")
    public NonceResponse handleNonce() {
        return credentialService.handleNonce();
    }

    @PostMapping("/notification")
    public void handleNotification(@RequestBody NotificationRequest request) {
        credentialService.handleNotification(request);
    }

    @GetMapping("/credential-offer/test")
    @ResponseBody
    public TestCredentialOfferResponse getCredentialOfferForTest() {

        log.info("createCredentialOfferForTest request");
        PreAuthorizeResponse preAuthorizeResponse = credentialIssuerFeignService.getPreAuthorizedCode();
        CredentialOfferRequest request = new CredentialOfferRequest();
        request.setUserId("test");
        CredentialOfferResponse response = credentialService.createCredentialOffer(request, preAuthorizeResponse, Constants.PRE_AUTHORIZED_CODE_TYPE);
        TestCredentialOfferResponse testResponse = new TestCredentialOfferResponse();
        testResponse.setCredentialIssuer(response.getCredentialIssuer());
        testResponse.setGrants(response.getGrants());
        testResponse.setCredentialIssuer(response.getCredentialIssuer());
         if(preAuthorizeResponse != null)
            testResponse.setTxCode(preAuthorizeResponse.getUserPin());
        log.info("createCredentialOfferForTest response : {}", new Gson().toJson(testResponse));

        return testResponse;

    }

    // todo: When acting as an Authorization Server substitute
//    @PostMapping("/token")
//    public TokenResponse handleTokenRequest(@RequestBody TokenRequest request) {
//        return credentialService.handleTokenRequest(request);
//    }
}
