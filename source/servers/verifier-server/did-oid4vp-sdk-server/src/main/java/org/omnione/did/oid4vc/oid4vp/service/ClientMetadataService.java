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

package org.omnione.did.oid4vc.oid4vp.service;

import org.omnione.did.oid4vc.oid4vp.config.OID4VPConfig;
import org.omnione.did.oid4vc.oid4vp.dto.ClientMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientMetadataService {

  private final VerifierConfigService configService;

  public ClientMetadata createClientMetadata() {
    OID4VPConfig config = configService.getOID4VPConfig();
    return ClientMetadata.builder()
        // OpenID4VP 1.0 required minimum fields ===
        .vpFormatsSupported(getVpFormatsSupported())
        .clientName(config.getClientName())
        .build();
  }

  /**
   * Gets VP formats supported from configuration
   */
  public Map<String, Object> getVpFormatsSupported() {
    OID4VPConfig config = configService.getOID4VPConfig();
    Map<String, Object> vpFormats = config.getClientMetadata().getVpFormatsSupported();

    log.debug("Using VP formats: {}", vpFormats);
    return vpFormats;
  }
}