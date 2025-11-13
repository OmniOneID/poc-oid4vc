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

package com.example.oid4vc.verifier.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oid4vp.verifier")
public class OID4VPProperties {

  private String baseUrl;
  private String clientName = "OID4VP Verifier";
  private Session session = new Session();
  private Endpoints endpoints = new Endpoints();

  @Data
  public static class Session {
    private long requestUriTtl = 300000;  // 5 minutes default (milliseconds)
    private long verificationTtl = 600000; // 10 minutes default (milliseconds)
  }

  @Data
  public static class Endpoints {
    private String response = "/oid4vp/response";
    private String request = "/oid4vp/request";
  }

  private Crypto crypto = new Crypto();

  @Data
  public static class Crypto {

    // Issuer Test key (Base64 encoded)
    private String issuerPrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
    private String issuerPublicKey = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";

    // Holder Test key (using same key for testing)
    private String holderPrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
    private String holderPublicKey = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";

    private String keyAlgorithm = "EC";
    private String curve = "secp256r1";

    private String defaultAudience = "did:omn:issuer";
    private String defaultNonce = "dcql-nonce-456";
  }

  public Crypto getCrypto() {
    return crypto;
  }

  public String getResponseUrl() {
    return baseUrl + endpoints.response;
  }

  public String getRequestUrl() {
    return baseUrl + endpoints.request;
  }

  public String buildClientId(String responseMode) {
    return "redirect_uri:" + getResponseUrl();
  }

  public String buildClientId() {
    return buildClientId("direct_post");
  }
}