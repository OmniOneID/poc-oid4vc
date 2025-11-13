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

package com.example.oid4vc.issuer.config;

import com.example.oid4vc.issuer.issuer.CredentialIssuer;
import com.example.oid4vc.issuer.issuer.SdJwtIssuer;
import com.example.oid4vc.issuer.issuer.VcIssuer;
import com.example.oid4vc.issuer.service.SharedStateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class IssuerSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/credential/**").authenticated()
                        .anyRequest().permitAll()
                )
                // verify access token
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    // todo: Figure out how to handle this

    @Bean
    public SdJwtIssuer devSdJwtIssuer(SharedStateService sharedStateService) {
        return new SdJwtIssuer(sharedStateService);
    }

    @Bean
    public VcIssuer vcIssuer(SharedStateService sharedStateService) {
        return new VcIssuer(sharedStateService);
    }

    @Bean(name = "NationalID")
    public CredentialIssuer nationalIdIssuer(SdJwtIssuer sdJwtIssuer) {
        return sdJwtIssuer;
    }

    @Bean(name = "mDL")
    public CredentialIssuer mdlIssuer(SdJwtIssuer sdJwtIssuer) {
        return sdJwtIssuer;
    }

    @Bean(name = "TEC")
    public CredentialIssuer tecIssuer(VcIssuer vcIssuer) {
        return vcIssuer;
    }

    @Bean(name = "UCR")
    public CredentialIssuer ucrIssuer(VcIssuer vcIssuer) {
        return vcIssuer;
    }
}