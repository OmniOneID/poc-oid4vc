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

package org.omnione.did.oid4vc.authorization.authorization.oid4vci;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomTokenResponseHandler implements AuthenticationSuccessHandler {

    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenHttpResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

    private final OAuth2AuthorizationService authorizationService;
    private final AuthorizationDetailsService authorizationDetailsService;

    public CustomTokenResponseHandler(OAuth2AuthorizationService authorizationService, AuthorizationDetailsService authorizationDetailsService) {
        this.authorizationService = authorizationService;
        this.authorizationDetailsService = authorizationDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication = (OAuth2AccessTokenAuthenticationToken) authentication;

        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();

        OAuth2Authorization authorization = this.authorizationService.findByToken(
                accessToken.getTokenValue(),
                OAuth2TokenType.ACCESS_TOKEN
        );

        Map<String, Object> additionalParameters = new HashMap<>(accessTokenAuthentication.getAdditionalParameters());

        if (authorization != null) {
            Object authDetails = null;
            if (authorization.getAuthorizationGrantType().equals(AuthorizationGrantType.AUTHORIZATION_CODE)) {
                Object authorizationRequestAttr = authorization.getAttribute(org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.class.getName());
                if (authorizationRequestAttr instanceof org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest) {
                    org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest authorizationRequest = (org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest) authorizationRequestAttr;
                    authDetails = authorizationRequest.getAdditionalParameters().get("authorization_details");
                }
            } else {
                authDetails = authorization.getAttribute("authorization_details");
            }

            if (authDetails != null) {
                List<Map<String, Object>> approvedAuthDetailsList = authorizationDetailsService.enrichAuthorizationDetails(authDetails);
                additionalParameters.put("authorization_details", approvedAuthDetailsList);
            }
        }

        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                .tokenType(accessToken.getTokenType())
                .scopes(accessToken.getScopes());
        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        }
        if (refreshToken != null) {
            builder.refreshToken(refreshToken.getTokenValue());
        }

        builder.additionalParameters(additionalParameters);

        OAuth2AccessTokenResponse accessTokenResponse = builder.build();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.accessTokenHttpResponseConverter.write(accessTokenResponse, null, httpResponse);
    }
}
