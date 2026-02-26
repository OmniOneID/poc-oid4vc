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

package org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.grant;

import org.omnione.did.oid4vc.authorization.authorization.oid4vci.AuthorizationDetailsService;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.dto.PreAuthorizedCode;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.service.PreAuthorizationService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.*;

public class PreAuthorizedCodeGrantAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final PreAuthorizationService preAuthorizationService;
    private final AuthorizationDetailsService authorizationDetailsService;

    public PreAuthorizedCodeGrantAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
                                                        PreAuthorizationService preAuthorizationService,
                                                        AuthorizationDetailsService authorizationDetailsService) {
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.preAuthorizationService = preAuthorizationService;
        this.authorizationDetailsService = authorizationDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PreAuthorizedCodeGrantAuthenticationToken preAuthorizedCodeGrantAuthentication =
                (PreAuthorizedCodeGrantAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(preAuthorizedCodeGrantAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        String code = preAuthorizedCodeGrantAuthentication.getPreAuthorizedCode();
        PreAuthorizedCode preAuthorizedCode = preAuthorizationService.findByCode(code)
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Pre-authorized code not found.", null)));

        if (preAuthorizedCode.isConsumed() || preAuthorizedCode.isExpired()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Pre-authorized code is invalid or expired.", null));
        }

        String txCode = preAuthorizedCodeGrantAuthentication.getTxCode();
        if (preAuthorizedCode.getUserPin() != null && !Objects.equals(preAuthorizedCode.getUserPin(), txCode)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid transaction code.", null));
        }

        preAuthorizationService.consume(code);

        String principalName = preAuthorizedCode.getUserId() != null ? preAuthorizedCode.getUserId() : clientPrincipal.getName();
        preAuthorizedCodeGrantAuthentication.setUserId(preAuthorizedCode.getUserId());

        Map<String, Object> additionalParameters = preAuthorizedCodeGrantAuthentication.getAdditionalParameters();
        Object authDetails = additionalParameters.get("authorization_details");

        Set<String> authorizedScopes;
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(principalName)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE);

        if (authDetails != null) {
            List<Map<String, Object>> approvedAuthDetailsList = authorizationDetailsService.enrichAuthorizationDetails(authDetails);
            authorizationBuilder.attribute("authorization_details", approvedAuthDetailsList);
            authorizedScopes = Collections.emptySet();
        } else {
            if (preAuthorizedCode.getScopes() == null || preAuthorizedCode.getScopes().isEmpty()) {
                authorizedScopes = new HashSet<>(registeredClient.getScopes());
            } else {
                authorizedScopes = new HashSet<>(preAuthorizedCode.getScopes());
                authorizedScopes.retainAll(registeredClient.getScopes());
            }
        }

        authorizationBuilder.authorizedScopes(authorizedScopes);

        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(preAuthorizedCodeGrantAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
                .authorizedScopes(authorizedScopes)
                .build();

        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "The token generator failed to generate the access token.", null));
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());

        if (generatedAccessToken instanceof ClaimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, ((ClaimAccessor) generatedAccessToken).getClaims()));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        this.authorizationService.save(authorization);

        Map<String, Object> responseAdditionalParameters = new HashMap<>();
        if (preAuthorizedCode.getCNonce() != null) {
            responseAdditionalParameters.put("c_nonce", preAuthorizedCode.getCNonce());
        }

        return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, null, responseAdditionalParameters);
    }
    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthorizedCodeGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }
}
