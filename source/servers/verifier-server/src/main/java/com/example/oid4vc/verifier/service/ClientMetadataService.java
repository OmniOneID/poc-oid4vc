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

package com.example.oid4vc.verifier.service;

import com.example.oid4vc.verifier.dto.ClientMetadata;
import com.example.oid4vc.verifier.configuration.OID4VPProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientMetadataService {

  private final OID4VPProperties oid4vpProperties;

  public ClientMetadata createClientMetadata() {
    return ClientMetadata.builder()
        // OpenID4VP 1.0 required fields ===
        .vpFormatsSupported(createMinimalVPFormatsSupported())
        .clientName(this.oid4vpProperties.getClientName())
        .build();
  }

  private Map<String, Object> createMinimalVPFormatsSupported() {
    Map<String, Object> vpFormats = new LinkedHashMap<>();

    Map<String, Object> sdJwtFormat = new LinkedHashMap<>();
    sdJwtFormat.put("sd-jwt_alg_values", List.of("ES256"));
    sdJwtFormat.put("kb-jwt_alg_values", List.of("ES256"));
    vpFormats.put("vc+sd-jwt", sdJwtFormat);

    log.debug("Created minimal VP formats: {}", vpFormats);
    return vpFormats;
  }
}