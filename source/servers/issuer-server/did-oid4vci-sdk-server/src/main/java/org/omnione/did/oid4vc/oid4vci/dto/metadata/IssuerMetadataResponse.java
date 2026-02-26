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

package org.omnione.did.oid4vc.oid4vci.dto.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class IssuerMetadataResponse {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @JsonProperty("authorization_server")
    private List<String> authorizationServer;

    @JsonProperty("credential_offer_endpoint")
    private String credentialOfferEndpoint;

    @JsonProperty("credential_endpoint")
    private String credentialEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("nonce_endpoint")
    private String nonceEndpoint;

    @JsonProperty("deferred_credential_endpoint")
    private String deferredCredentialEndpoint;

    @JsonProperty("notification_endpoint")
    private String notificationEndpoint;

    @JsonProperty("credential_response_encryption_alg_values_supported")
    private String[] credentialResponseEncryptionAlgValuesSupported;

    @JsonProperty("credential_response_encryption_enc_values_supported")
    private String[] credentialResponseEncryptionEncValuesSupported;

    @JsonProperty("require_credential_response_encryption")
    private boolean requireCredentialResponseEncryption;

    @JsonProperty("credential_identifiers_supported")
    private boolean credentialIdentifiersSupported;

    @JsonProperty("credential_configurations_supported")
    private Map<String, Object> credentialConfigurationsSupported;

}
