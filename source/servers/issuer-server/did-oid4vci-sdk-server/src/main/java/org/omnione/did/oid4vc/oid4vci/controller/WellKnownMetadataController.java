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

package org.omnione.did.oid4vc.oid4vci.controller;

import org.omnione.did.oid4vc.oid4vci.dto.metadata.IssuerMetadataResponse;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIException;
import org.omnione.did.oid4vc.oid4vci.service.CredentialService;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WellKnownMetadataController {

    private final CredentialService credentialService;

    @GetMapping("/.well-known/openid-credential-issuer")
    public IssuerMetadataResponse getIssuerMetadata() throws OID4VCIException {
        log.info("getIssuerMetadata request");
        IssuerMetadataResponse response = credentialService.getIssuerMetadata();
        log.info("getIssuerMetadata response : {}", new Gson().toJson(response));
        return response;
    }
}
