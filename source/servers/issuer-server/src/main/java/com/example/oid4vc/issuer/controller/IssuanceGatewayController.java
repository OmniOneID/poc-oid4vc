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

import com.example.oid4vc.issuer.api.dto.PreAuthorizeResponse;
import com.example.oid4vc.issuer.dto.qr.QrImageData;
import com.example.oid4vc.issuer.service.CredentialIssuerFeignService;
import com.example.oid4vc.issuer.service.CredentialService;
import com.example.oid4vc.issuer.temp.OfferStore;
import com.google.zxing.WriterException;
import com.nimbusds.jose.shaded.gson.Gson;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.example.oid4vc.issuer.util.QrMaker.makeQrImage;

@Slf4j
@Controller
public class IssuanceGatewayController {

    private final CredentialService credentialService;
    private final CredentialIssuerFeignService credentialIssuerFeignService;
    private final OfferStore offerStore; // temp
    private final String issuerBaseUrl;

    public IssuanceGatewayController(CredentialService credentialService,
                                     CredentialIssuerFeignService credentialIssuerFeignService,
                                     OfferStore offerStore,
                                     @Value("${issuer.base-url}") String issuerBaseUrl) {
        this.credentialService = credentialService;
        this.credentialIssuerFeignService = credentialIssuerFeignService;
        this.offerStore = offerStore;
        this.issuerBaseUrl = issuerBaseUrl;
    }

    @GetMapping("/oid4vci/test")
    public String issue(Model model) {
        return "issue";
    }

    @PostMapping("/qr-data/generate-qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateQrData(@RequestBody Map<String, String> data) throws IOException, WriterException, NoSuchAlgorithmException {
        String userId = data.get("userId");
        String grantType = data.get("grantType");

        return generateCredentialOfferUri(userId, grantType);

    }

    public ResponseEntity<Map<String, Object>> generateCredentialOfferUri(String userId, String grantType)
            throws IOException, WriterException, NoSuchAlgorithmException {

        String requestId = generateOfferId(userId, grantType);
        Map<String, Object> response = new HashMap<>();

        if ("pre-authorized_code".equals(grantType)) {
            // --- Pre-Authorized Code Flow ---
            Map<String, Object> errorResponse = new HashMap<>();
            try {
                PreAuthorizeResponse preAuthorizeResponse = credentialIssuerFeignService.getPreAuthorizedCode();
                if (preAuthorizeResponse == null) {
                    log.error("Failed to get pre-authorized code. The response from the Authorization Server was null.");
                    errorResponse.put("error", "Authorization Server Communication Error");
                    errorResponse.put("message", "Did not receive a response from the authorization server.");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
                }
                offerStore.save(requestId, preAuthorizeResponse);
                response.put("txCode", preAuthorizeResponse.getUserPin());
            } catch (FeignException e) {
                log.error("Failed to get pre-authorized code from Authorization Server", e);
                errorResponse.put("error", "Authorization Server Unavailable");
                errorResponse.put("message", "Could not connect to the authorization server. Please try again later.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }

        } else if ("authorization_code".equals(grantType)) {
            // --- Authorization Code Flow ---
        } else {
            throw new IllegalArgumentException("Unsupported grant type: " + grantType);
        }

        String requestUri = issuerBaseUrl + "/credential-offer/" + requestId;
        String authUrl = "openid-credential-offer://?credential_offer_uri=" + requestUri;

        QrImageData qrImageData = makeQrImage(authUrl);

        response.put("qrImage", qrImageData.getQrImage());
        response.put("qrData", authUrl);

        return ResponseEntity.ok(response);
    }

    private String generateOfferId(String userId, String grantType) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // for sast test..
        byte[] salt = "oid4vci".getBytes();
        md.update(salt);
        byte[] offerIdBytes =  md.digest(userId.getBytes());
        String offerId = "";
        if ("pre-authorized_code".equals(grantType)) {
            offerId = "p" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(offerIdBytes);
        } else if ("authorization_code".equals(grantType)) {
            offerId = "a" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(offerIdBytes);
        }
        return offerId;
    }

    // hmac sha256
    private byte[] generateOfferIdByHmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 failure", e);
        }
    }

    // todo: credential Identifier list - Figure out how to handle this
    @ResponseBody
    @GetMapping("/get-credential-identifier")
    public List<String> getCredentialIdentifier(@RequestParam("credentialConfigurationId") String credentialConfigurationId) {
        log.info("getCredentialIdentifier request");
        List<String> response = new ArrayList<>();
        if(credentialConfigurationId.equals("StudentID"))
            response = Arrays.asList("TEC", "UCR");
        else if(credentialConfigurationId.equals("VerifiableIdSD"))
            response = Arrays.asList("NationalID", "mDL");

        log.info("getCredentialIdentifier response : {}", new Gson().toJson(response));
        return response;
    }

}
