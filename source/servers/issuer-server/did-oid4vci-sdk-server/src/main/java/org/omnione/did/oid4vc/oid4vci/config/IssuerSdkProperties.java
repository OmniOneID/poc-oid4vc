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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ConfigurationProperties(prefix = "issuer.sdk")
public class IssuerSdkProperties {

    /**
     * Map of Credential Configuration IDs (e.g. "StudentID") to their configuration details.
     */
    private Map<String, CredentialConfig> credentialConfigurations;

    @Getter
    @Setter
    public static class CredentialConfig {
        /**
         * The data format/protocol (e.g., "sd-jwt", "open-did-vc").
         */
        private String format;

        /**
         * List of specific credential identifiers (e.g., "NationalID", "TEC").
         */
        private Set<String> identifiers;
    }

    /**
     * Helper method to get identifiers by Configuration ID (e.g., "StudentID").
     */
    public Set<String> getIdentifiersByConfigId(String configId) {
        if (credentialConfigurations == null || !credentialConfigurations.containsKey(configId)) {
            return new HashSet<>();
        }
        return credentialConfigurations.get(configId).getIdentifiers();
    }

    /**
     * Helper method to get all identifiers for a specific format (e.g., "sd-jwt").
     * Aggregates identifiers from all configurations that use this format.
     */
    public Set<String> getIdentifiersByFormat(String format) {
        if (credentialConfigurations == null) {
            return new HashSet<>();
        }
        return credentialConfigurations.values().stream()
                .filter(config -> format.equals(config.getFormat()))
                .flatMap(config -> config.getIdentifiers().stream())
                .collect(Collectors.toSet());
    }
}