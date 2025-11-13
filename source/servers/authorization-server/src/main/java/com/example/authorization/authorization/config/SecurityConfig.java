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

package com.example.authorization.authorization.config;

import com.example.authorization.authorization.oid4vci.AuthorizationDetailsService;
import com.example.authorization.authorization.oid4vci.CustomTokenResponseHandler;
import com.example.authorization.authorization.oid4vci.credentialidentifier.service.CredentialIdentifierFeignService;
import com.example.authorization.authorization.oid4vci.preauthorized.grant.PreAuthorizedCodeGrantAuthenticationConverter;
import com.example.authorization.authorization.oid4vci.preauthorized.grant.PreAuthorizedCodeGrantAuthenticationProvider;
import com.example.authorization.authorization.oid4vci.preauthorized.grant.PreAuthorizedCodeGrantAuthenticationToken;
import com.example.authorization.authorization.oid4vci.preauthorized.service.PreAuthorizationService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Generate and store the key pair only once on application load
    private static final KeyPair rsaKeyPair = generateRsaKey();

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                      PreAuthorizedCodeGrantAuthenticationProvider preAuthorizedCodeGrantAuthenticationProvider,
                                                                      CustomTokenResponseHandler customTokenResponseHandler) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
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

    @Bean
    @Order(2)
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
//    @Bean
//    @Order(1)
//    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
//                                                                      PreAuthorizedCodeGrantAuthenticationProvider preAuthorizedCodeGrantAuthenticationProvider,
//                                                                      CustomTokenResponseHandler customTokenResponseHandler) throws Exception {
//
//        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
//        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
//                .tokenEndpoint(tokenEndpoint ->
//                        tokenEndpoint
//                                .accessTokenRequestConverter(new PreAuthorizedCodeGrantAuthenticationConverter())
//                                .authenticationProvider(preAuthorizedCodeGrantAuthenticationProvider)
//                                // todo : Custom addition for authorization_details
//                                .accessTokenResponseHandler(customTokenResponseHandler)
//                                // ...
//                )
//                .oidc(Customizer.withDefaults());
//
//        http
//                // Redirect to the login page when a user is not authenticated
//                .exceptionHandling((exceptions) -> exceptions
//                        .authenticationEntryPoint(
//                                new LoginUrlAuthenticationEntryPoint("/login"))
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    @Order(2)
//    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
//        http.authorizeHttpRequests(authorize ->
//                        authorize
//                                .requestMatchers("/", "/pre-authorize", "/auth/callback").permitAll()
//                                .requestMatchers("/images/**", "/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
//                                .anyRequest().authenticated()
//                )
//                .csrf(csrf -> csrf.ignoringRequestMatchers("/pre-authorize"))
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .permitAll()
//                )
//                // Enable OAuth2/OIDC login (Google login)
//                .oauth2Login(oauth2 -> oauth2
//                        .loginPage("/login")
//                );
//        return http.build();
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var userDetails = User.builder()
                .username("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
    // Scope parsing - register allowed scopes
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder, @Value("${clients.redirect-url}") String redirectUrl) {
        // Existing web client
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oid4vci-client")
                .clientSecret(passwordEncoder.encode("secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
//                .redirectUri("http://localhost:8081/login/oauth2/code/oid4vci-client-oidc")
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .redirectUri("http://localhost:8081/auth/callback")
//                .redirectUri(redirectUrl)
                .scope(OidcScopes.OPENID)
                .scope("profile")
                .scope("email")
                .scope("UniversityDegree")
                //Disable PKCE
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();

        RegisteredClient androidClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oid4vci-android")
                // Set to NONE as Public Client does not have a Secret
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
                .redirectUri("org.omnione.did.sdk.oid4vc://callback") // Custom scheme for Android app
                .scope(OidcScopes.OPENID)
                .scope("UniversityDegree")
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .build();

        RegisteredClient iosClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oid4vci-ios")
                // Set to NONE as Public Client does not have a Secret
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(PreAuthorizedCodeGrantAuthenticationToken.PRE_AUTHORIZED_CODE)
                .redirectUri("oid4vc-app://callback") // Custom scheme for iPhone app
                .scope(OidcScopes.OPENID)
                .scope("UniversityDegree")
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .build();

        return new InMemoryRegisteredClientRepository(webClient, androidClient, iosClient);
    }

    @Bean
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

    // Generate key pair
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
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    //iss value
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${authorization-server.issuer-url}") String issuerUri) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        return new JwtGenerator(new NimbusJwtEncoder(jwkSource));
    }

    @Bean
    public PreAuthorizedCodeGrantAuthenticationProvider preAuthorizedCodeGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            PreAuthorizationService preAuthorizationService,
            AuthorizationDetailsService authorizationDetailsService) {
        return new PreAuthorizedCodeGrantAuthenticationProvider(authorizationService, tokenGenerator, preAuthorizationService, authorizationDetailsService);
    }

    @Bean
    public CustomTokenResponseHandler customTokenResponseHandler(OAuth2AuthorizationService authorizationService, AuthorizationDetailsService authorizationDetailsService) {
        return new CustomTokenResponseHandler(authorizationService, authorizationDetailsService);
    }

    @Bean
    public AuthorizationDetailsService authorizationDetailsService(CredentialIdentifierFeignService credentialIdentifierFeignService) {
        return new AuthorizationDetailsService(credentialIdentifierFeignService);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            Authentication principal = context.getPrincipal();
            JwtClaimsSet.Builder claims = context.getClaims();

            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                if (context.getAuthorizationGrantType().equals(AuthorizationGrantType.AUTHORIZATION_CODE) ||
                    context.getAuthorizationGrantType().equals(AuthorizationGrantType.REFRESH_TOKEN)) {

                    claims.subject(principal.getName());

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
