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

package org.omnione.did.oid4vc.oid4vp.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class OID4VPConfig {

    @JsonProperty("baseUrl")
    private String baseUrl;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("invocationScheme")
    private String invocationScheme = "openid4vp://";

    @JsonProperty("clientId")
    private ClientId clientId = new ClientId();

    @JsonProperty("session")
    private Session session = new Session();

    @JsonProperty("endpoints")
    private Endpoints endpoints = new Endpoints();

    @JsonProperty("clientMetadata")
    private ClientMetadata clientMetadata = new ClientMetadata();

    @JsonProperty("crypto")
    private Crypto crypto = new Crypto();

    @Data
    public static class Session {
        @JsonProperty("sessionTtl")
        private long sessionTtl = 300000;
    }

    @Data
    public static class Endpoints {
        @JsonProperty("response")
        private String response = "/oid4vp/response";

        @JsonProperty("request")
        private String request = "/oid4vp/request";
    }

    @Data
    public static class ClientMetadata {
        @JsonProperty("vpFormatsSupported")
        private Map<String, Object> vpFormatsSupported = Collections.emptyMap();
    }

    @Data
    public static class Crypto {
        @JsonProperty("vpTokenEncryptionKey")
        private String vpTokenEncryptionKey;
    }

    public String getResponseUrl() {
        return baseUrl + endpoints.getResponse();
    }

    public String getRequestUrl() {
        return baseUrl + endpoints.getRequest();
    }

    public String getInvocationScheme() {
        return invocationScheme;
    }

    public String buildClientId() {
      return clientId.getScheme() + ":" + clientId.getValue();
    }

    @Data
    public static class ClientId {
        @JsonProperty("scheme")
        private String scheme;

        @JsonProperty("value")
        private String value;
    }
}