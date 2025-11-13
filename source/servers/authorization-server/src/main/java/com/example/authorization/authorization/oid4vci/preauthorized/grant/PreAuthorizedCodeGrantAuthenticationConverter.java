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

package com.example.authorization.authorization.oid4vci.preauthorized.grant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreAuthorizedCodeGrantAuthenticationConverter implements AuthenticationConverter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type (REQUIRED)
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE.getValue().equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        MultiValueMap<String, String> parameters = getParameters(request);

        // pre-authorized_code (REQUIRED)
        String preAuthorizedCode = parameters.getFirst("pre-authorized_code");
        if (!StringUtils.hasText(preAuthorizedCode) || parameters.get("pre-authorized_code").size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // tx_code (OPTIONAL)
        String txCode = parameters.getFirst("tx_code");
        if (StringUtils.hasText(txCode) && parameters.get("tx_code").size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        Map<String, Object> additionalParameters = new HashMap<>();

        String authDetailsJsonString = parameters.getFirst("authorization_details");
        if (StringUtils.hasText(authDetailsJsonString)) {
            try {
                List<Map<String, Object>> authorizationDetails =
                        objectMapper.readValue(authDetailsJsonString, new TypeReference<>() {});
                additionalParameters.put("authorization_details", authorizationDetails);
            } catch (JsonProcessingException e) {
                throw new OAuth2AuthenticationException("invalid_authorization_details_format");
            }
        }

        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals("pre-authorized_code") &&
                    !key.equals("tx_code") &&
                    !key.equals("authorization_details")) {
                additionalParameters.put(key, value.get(0));
            }
        });

        return new PreAuthorizedCodeGrantAuthenticationToken(clientPrincipal, preAuthorizedCode, txCode, additionalParameters);
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values.length > 0) {
                for (String value : values) {
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }
}
