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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "oid4vc.auth")
public class Oid4vcAuthServerProperties {
    // authorization-server.issuer-url 매핑
    // 기본값
    private String issuerUrl = "http://localhost:8080";

    // clients.* 매핑을 위한 내부 클래스 또는 필드
    private Clients clients = new Clients();

    @Getter
    @Setter
    public static class Clients {
        // clients.redirect-url
        private String redirectUrl = "http://localhost:8080/auth/callback";

        // clients.issuer-server.url
        private IssuerServer issuerServer = new IssuerServer();

        @Getter
        @Setter
        public static class IssuerServer {
            private String url = "http://localhost:8080";
        }
    }
}