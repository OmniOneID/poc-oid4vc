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

package com.example.authorization.authorization.oid4vci;

import com.example.authorization.authorization.oid4vci.credentialidentifier.service.CredentialIdentifierFeignService;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuthorizationDetailsService {

    private final CredentialIdentifierFeignService credentialIdentifierFeignService;
    private final Gson gson = new Gson();

    public AuthorizationDetailsService(CredentialIdentifierFeignService credentialIdentifierFeignService) {
        this.credentialIdentifierFeignService = credentialIdentifierFeignService;
    }

    public List<Map<String, Object>> enrichAuthorizationDetails(Object authDetails) {
        List<Map<String, Object>> parsedDetails;
        if (authDetails instanceof List) {
            parsedDetails = (List<Map<String, Object>>) authDetails;
        } else if (authDetails instanceof String) {
            try {
                String decodedDetails = URLDecoder.decode((String) authDetails, StandardCharsets.UTF_8.name());
                Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                parsedDetails = gson.fromJson(decodedDetails, listType);
            } catch (java.io.UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return List.of();
        }

        List<Map<String, Object>> approvedAuthDetailsList = new ArrayList<>();
        for (Map<String, Object> requestedDetail : parsedDetails) {
            String type = (String) requestedDetail.get("type");
            String configId = (String) requestedDetail.get("credential_configuration_id");

            List<String> credentialIdentifiers = credentialIdentifierFeignService.getCredentialIdentifier(configId);

            Map<String, Object> responseDetail = new java.util.HashMap<>();
            responseDetail.put("type", type);
            responseDetail.put("credential_configuration_id", configId);
            responseDetail.put("credential_identifiers", credentialIdentifiers);

            approvedAuthDetailsList.add(responseDetail);
        }
        return approvedAuthDetailsList;
    }
}
