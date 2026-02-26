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
import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifierConfigService {

    private static final String CONFIG_TYPE_OID4VP = "OID4VP";

    private final OID4VPRepository oid4vpRepository;
    private final ObjectMapper objectMapper;

    private final AtomicReference<OID4VPConfig> cachedConfig = new AtomicReference<>();

    @PostConstruct
    public void init() {
        loadConfig();
    }

    /**
     * Get OID4VP configuration (cached).
     */
    public OID4VPConfig getOID4VPConfig() {
        OID4VPConfig config = cachedConfig.get();
        if (config == null) {
            config = loadConfig();
        }
        return config;
    }

    /**
     * Reload configuration from database.
     */
    public OID4VPConfig reloadConfig() {
        log.info("Reloading OID4VP configuration from database");
        return loadConfig();
    }

    /**
     * Load configuration from database and cache it.
     */
    protected OID4VPConfig loadConfig() {
        try {
            OID4VPConfigDto dto = oid4vpRepository.findByType(CONFIG_TYPE_OID4VP)
                    .orElseThrow(() -> new OID4VPException(OID4VPErrorCode.ERR_CODE_CONFIG_NOT_FOUND,
                            "type=" + CONFIG_TYPE_OID4VP));

            OID4VPConfig config = objectMapper.readValue(dto.getConfig(), OID4VPConfig.class);
            cachedConfig.set(config);

            log.info("Loaded OID4VP configuration: baseUrl={}, clientName={}",
                    config.getBaseUrl(), config.getClientName());
            log.debug("VP formats supported: {}",
                    config.getClientMetadata() != null ?
                            config.getClientMetadata().getVpFormatsSupported().keySet() : "none");

            return config;
        } catch (OID4VPException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse OID4VP configuration JSON", e);
            throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CONFIG_LOAD_FAILED, e));
        }
    }

    /**
     * Clear cached configuration.
     */
    public void clearCache() {
        cachedConfig.set(null);
        log.info("OID4VP configuration cache cleared");
    }

    /**
     * Save OID4VP configuration and update cache.
     */
    public void saveOID4VPConfig(OID4VPConfig config) {
      try {
        String configJson = objectMapper.writeValueAsString(config);

        OID4VPConfigDto dto = oid4vpRepository.findByType(CONFIG_TYPE_OID4VP)
            .map(existing -> {
              existing.setConfig(configJson);
              return existing;
            })
            .orElse(OID4VPConfigDto.builder()
                .type(CONFIG_TYPE_OID4VP)
                .config(configJson)
                .build());

        oid4vpRepository.save(dto);
        cachedConfig.set(config);  // Update Cache

        log.info("OID4VP configuration saved and cache updated: baseUrl={}", config.getBaseUrl());
      } catch (JsonProcessingException e) {
        log.error("Failed to serialize OID4VP configuration", e);
        throw new RuntimeException(new OID4VPException(OID4VPErrorCode.ERR_CODE_CONFIG_SAVE_FAILED, e));
      }
    }
}