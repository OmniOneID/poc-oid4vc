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

package org.omnione.did.oid4vc.authorization.authorization.config;

import org.omnione.did.oid4vc.authorization.authorization.config.Oid4vcAuthServerProperties;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.AuthorizationDetailsService;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.CustomTokenResponseHandler;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.credentialidentifier.api.IssuerFeign;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.credentialidentifier.service.CredentialIdentifierFeignService;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.grant.PreAuthorizedCodeGrantAuthenticationConverter;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.grant.PreAuthorizedCodeGrantAuthenticationProvider;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.grant.PreAuthorizedCodeGrantAuthenticationToken;
import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.service.PreAuthorizationService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AutoConfiguration // @Configuration 대신 사용
@EnableWebSecurity
@EnableConfigurationProperties(Oid4vcAuthServerProperties.class)
@EnableFeignClients(clients = {IssuerFeign.class})
@ComponentScan(basePackages = "org.omnione.did.oid4vc.authorization.authorization")
public class Oid4vcAuthServerAutoConfiguration {

    private static final KeyPair rsaKeyPair = generateRsaKey();

    @Bean
    @Order(1)
    @ConditionalOnMissingBean(name = "authorizationServerSecurityFilterChain")
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            RegisteredClientRepository registeredClientRepository,
            PreAuthorizedCodeGrantAuthenticationProvider preAuthorizedCodeGrantAuthenticationProvider,
            CustomTokenResponseHandler customTokenResponseHandler) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        authorizationServerConfigurer
                .authorizationServerMetadataEndpoint(metadataEndpoint ->
                        metadataEndpoint.authorizationServerMetadataCustomizer(metadata -> {
                            metadata.claim("token_endpoint_auth_methods_supported", List.of("none", "urn:ietf:params:oauth:client-assertion-type:jwt-client-attestation"));
                            metadata.claim("grant_types_supported", List.of("authorization_code", "refresh_token", "urn:ietf:params:oauth:grant-type:pre-authorized_code"));
                        })
                )
                .clientAuthentication(clientAuthentication ->
                        clientAuthentication
                                .authenticationConverter(request -> {
                                    String clientId = request.getParameter("client_id");
                                    String clientAssertionType = request.getParameter("client_assertion_type");

                                    if ("urn:ietf:params:oauth:client-assertion-type:jwt-client-attestation".equals(clientAssertionType)) {
                                        if (!StringUtils.hasText(clientId)) {
                                            clientId = "oid4vci-android";
                                        }
                                        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, null);
                                    }

                                    if ("oid4vci-android".equals(clientId) || "oid4vci-ios".equals(clientId)) {
                                        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, null);
                                    }
                                    return null;
                                })
                                .authenticationProvider(new AuthenticationProvider() {
                                    @Override
                                    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                                        OAuth2ClientAuthenticationToken clientAuth = (OAuth2ClientAuthenticationToken) authentication;
                                        if (ClientAuthenticationMethod.NONE.equals(clientAuth.getClientAuthenticationMethod())) {
                                            String clientId = clientAuth.getPrincipal().toString();
                                            if ("oid4vci-android".equals(clientId) || "oid4vci-ios".equals(clientId)) {
                                                RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
                                                if (registeredClient != null) {
                                                    return new OAuth2ClientAuthenticationToken(registeredClient, clientAuth.getClientAuthenticationMethod(), null);
                                                }
                                            }
                                        }
                                        return null;
                                    }

                                    @Override
                                    public boolean supports(Class<?> authentication) {
                                        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
                                    }
                                })
                );

        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .apply(authorizationServerConfigurer);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint(tokenEndpoint ->
                        tokenEndpoint
                                .accessTokenRequestConverter(new PreAuthorizedCodeGrantAuthenticationConverter())
                                .authenticationProvider(preAuthorizedCodeGrantAuthenticationProvider)
                                .accessTokenResponseHandler(customTokenResponseHandler)
                );

        return http.build();
    }

    /**
     * Default Security Filter Chain (Login Page etc.)
     * 앱에서 별도의 SecurityConfig를 정의하면 이 빈은 생성되지 않습니다.
     */
    @Bean
    @Order(3)
    @ConditionalOnMissingBean(name = "defaultSecurityFilterChain")
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/", "/pre-authorize", "/auth/callback", "/login").permitAll()
                                .requestMatchers("/images/**", "/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                                .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/pre-authorize"))
                .formLogin(form -> form.loginPage("/login"))
                .oauth2Login(oauth2 -> oauth2.loginPage("/login"));
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * UserDetailsService
     * 앱에서 DB 기반 UserDetailsService를 등록하면 이 In-Memory 빈은 무시됩니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var userDetails = User.builder()
                .username("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    @ConditionalOnMissingBean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * RegisteredClientRepository
     * 앱에서 DB 기반 ClientRepository를 등록하면 이 In-Memory 빈은 무시됩니다.
     * 하드코딩된 값 대신 Oid4vcAuthServerProperties를 사용합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder,
            Oid4vcAuthServerProperties properties) { // Properties 주입

        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oid4vci-client")
                .clientSecret(passwordEncoder.encode("secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .redirectUri("http://localhost:8081/auth/callback")
                .redirectUri(properties.getClients().getRedirectUrl()) // 프로퍼티 사용
                .scope(OidcScopes.OPENID)
                .scope("profile")
                .scope("email")
                .scope("UniversityDegree")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();

        RegisteredClient androidClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oid4vci-android")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .clientAuthenticationMethod(new ClientAuthenticationMethod("urn:ietf:params:oauth:client-assertion-type:jwt-client-attestation"))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
                .redirectUri("org.omnione.did.sdk.oid4vc://callback")
                .scope(OidcScopes.OPENID)
                .scope("UniversityDegree")
                .scope("eu.europa.ec.eudi.pid_vc_sd_jwt")
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .build();

        RegisteredClient iosClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oid4vci-ios")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .clientAuthenticationMethod(new ClientAuthenticationMethod("urn:ietf:params:oauth:client-assertion-type:jwt-client-attestation"))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
                .redirectUri("oid4vc-app://callback")
                .scope(OidcScopes.OPENID)
                .scope("UniversityDegree")
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(webClient, androidClient, iosClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public JWKSource<SecurityContext> jwkSource() {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoClassDefFoundError | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return keyPair;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationServerSettings authorizationServerSettings(Oid4vcAuthServerProperties properties) {
        return AuthorizationServerSettings.builder()
                .issuer(properties.getIssuerUrl()) // 프로퍼티 사용
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        return new JwtGenerator(new NimbusJwtEncoder(jwkSource));
    }

    @Bean
    @ConditionalOnMissingBean
    public PreAuthorizedCodeGrantAuthenticationProvider preAuthorizedCodeGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            PreAuthorizationService preAuthorizationService,
            AuthorizationDetailsService authorizationDetailsService) {
        return new PreAuthorizedCodeGrantAuthenticationProvider(authorizationService, tokenGenerator, preAuthorizationService, authorizationDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomTokenResponseHandler customTokenResponseHandler(OAuth2AuthorizationService authorizationService, AuthorizationDetailsService authorizationDetailsService) {
        return new CustomTokenResponseHandler(authorizationService, authorizationDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationDetailsService authorizationDetailsService(CredentialIdentifierFeignService credentialIdentifierFeignService) {
        return new AuthorizationDetailsService(credentialIdentifierFeignService);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            JwtClaimsSet.Builder claims = context.getClaims();

            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                // Set subject from the principal name stored in the Authorization object
                if (context.getAuthorization() != null) {
                    claims.subject(context.getAuthorization().getPrincipalName());
                }

                if (context.getAuthorizationGrantType().equals(AuthorizationGrantType.AUTHORIZATION_CODE) ||
                        context.getAuthorizationGrantType().equals(AuthorizationGrantType.REFRESH_TOKEN)) {
                    
                    Authentication principal = context.getPrincipal();
                    claims.claim("authorities", principal.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .collect(Collectors.toList()));

                } else if (context.getAuthorizationGrantType().equals(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)) {
                    claims.claim("flow_type", "oid4vci_pre-authorized_code");
                }
            }
        };
    }
}
