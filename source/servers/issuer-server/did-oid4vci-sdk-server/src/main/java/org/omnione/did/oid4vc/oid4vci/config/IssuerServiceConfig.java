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

package org.omnione.did.oid4vc.oid4vci.config;

import org.omnione.did.oid4vc.formatter.oid4vci.generator.impl.OpenDidVcGenerator;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.impl.SdJwtGenerator;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.impl.SdJwtGeneratorWithCert;
import org.omnione.did.oid4vc.oid4vci.adapter.Oid4vcIssuer;
import org.omnione.did.oid4vc.oid4vci.config.IssuerSdkProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@EnableConfigurationProperties(IssuerSdkProperties.class)
public class IssuerServiceConfig {

    @Bean
    public SdJwtGenerator sdJwtGenerator() {
        return new SdJwtGenerator();
    }

    @Bean
    public SdJwtGeneratorWithCert sdJwtGeneratorWithCert() {
        return new SdJwtGeneratorWithCert();
    }

    /**
     * Registers a ProtocolIssuer for SD-JWT based credentials.
     * It uses the generic Oid4vcUniversalIssuer adapter, injected with the specific SdJwtGenerator
     * and the list of format IDs it should handle.
     */
    @Bean
    public Oid4vcIssuer sdJwtProtocolIssuer(SdJwtGenerator sdJwtGenerator, IssuerSdkProperties properties) {
//        Set<String> credentialIdentifiers = properties.getIdentifiersByFormat("dc+sd-jwt");
//        if (credentialIdentifiers.isEmpty()) {
//            // Fallback default
//            credentialIdentifiers = Set.of(
//                    "NationalID",
//                    "mDL"
//            );
//        }
        Set<String> credentialIdentifiers = Set.of("NationalID", "mDL");
        return new Oid4vcIssuer(sdJwtGenerator, credentialIdentifiers);
    }

    @Bean
    public Oid4vcIssuer sdJwtProtocolIssuerWithCert(SdJwtGeneratorWithCert sdJwtGenerator, IssuerSdkProperties properties) {
        Set<String> credentialIdentifiers = Set.of("NationalIDCert", "PID");
//        if (credentialIdentifiers.isEmpty()) {
//            // Fallback default
//            credentialIdentifiers = Set.of(
//                    "NationalIDCert"
//            );
//        }
        return new Oid4vcIssuer(sdJwtGenerator, credentialIdentifiers);
    }

    @Bean
    public OpenDidVcGenerator openDidVcGenerator() {
        return new OpenDidVcGenerator();
    }

    @Bean
    public Oid4vcIssuer openDidVcProtocolIssuer(OpenDidVcGenerator openDidVcGenerator, IssuerSdkProperties properties) {
        Set<String> credentialIdentifiers = properties.getIdentifiersByFormat("open-did-vc");
        if (credentialIdentifiers.isEmpty()) {
            // Fallback default
            credentialIdentifiers = Set.of(
                    "TEC",
                    "UCR"
            );
        }
        return new Oid4vcIssuer(openDidVcGenerator, credentialIdentifiers);
    }

}
