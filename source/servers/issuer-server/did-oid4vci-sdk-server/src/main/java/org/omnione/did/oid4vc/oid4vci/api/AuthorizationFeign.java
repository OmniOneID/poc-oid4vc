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

package org.omnione.did.oid4vc.oid4vci.api;

import org.omnione.did.oid4vc.oid4vci.api.dto.PreAuthorizeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "credential-issuer", url = "${clients.auth-server.url}")
public interface AuthorizationFeign {

    @PostMapping("/pre-authorize")
    PreAuthorizeResponse sendPreAuthorize(@RequestBody List<String> request, @RequestParam("userId") String userId);

}
