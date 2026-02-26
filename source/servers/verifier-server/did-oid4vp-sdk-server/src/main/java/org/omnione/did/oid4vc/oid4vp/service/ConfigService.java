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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.config.OID4VPConfig;
import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String CONFIG_TYPE_OID4VP = "OID4VP";

    private final OID4VPRepository oid4vpRepository;
    private final ObjectMapper objectMapper;
    private final VerifierConfigService verifierConfigService;

    /**
     * Get all configurations
     */
    public List<Map<String, Object>> getAllConfigs() {
        List<OID4VPConfigDto> dtos = oid4vpRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (OID4VPConfigDto dto : dtos) {
            result.add(dtoToMap(dto));
        }

        return result;
    }

    /**
     * Get configuration by ID
     */
    public Map<String, Object> getConfigById(Long id) {
        Optional<OID4VPConfigDto> dtoOpt = oid4vpRepository.findById(id);
        return dtoOpt.map(this::dtoToMap).orElse(null);
    }

    /**
     * Create new configuration
     */
    public Map<String, Object> createConfig(String type, String configJson) throws OID4VPException {
        // Validate type uniqueness
        if (oid4vpRepository.existsByType(type)) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CONFIG_ALREADY_EXISTS, type);
        }

        // Validate JSON format
        validateConfigJson(configJson);

        OID4VPConfigDto dto = OID4VPConfigDto.builder()
            .type(type)
            .config(configJson)
            .build();

        OID4VPConfigDto saved = oid4vpRepository.save(dto);
        log.info("Created new configuration: type={}, id={}", type, saved.getId());

        // Refresh OID4VPConfig cache if OID4VP type config is created
        refreshOID4VPConfigCacheIfNeeded(type);

        return dtoToMap(saved);
    }

    /**
     * Update configuration by ID
     */
    public Map<String, Object> updateConfig(Long id, String type, String configJson) throws OID4VPException {
        Optional<OID4VPConfigDto> dtoOpt = oid4vpRepository.findById(id);
        if (dtoOpt.isEmpty()) {
            return null;
        }

        OID4VPConfigDto dto = dtoOpt.get();

        // Update type if provided
        if (type != null && !type.trim().isEmpty()) {
            // Check uniqueness if type is changing
            if (!type.equals(dto.getType()) && oid4vpRepository.existsByType(type)) {
                throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CONFIG_ALREADY_EXISTS, type);
            }
            dto.setType(type);
        }

        // Update config if provided
        if (configJson != null && !configJson.trim().isEmpty()) {
            validateConfigJson(configJson);
            dto.setConfig(configJson);
        }

        OID4VPConfigDto saved = oid4vpRepository.save(dto);
        log.info("Updated configuration: id={}, type={}", id, saved.getType());

        // Refresh OID4VPConfig cache if OID4VP type config is updated
        refreshOID4VPConfigCacheIfNeeded(saved.getType());

        return dtoToMap(saved);
    }

    /**
     * Delete configuration by ID
     */
    public boolean deleteConfig(Long id) {
        Optional<OID4VPConfigDto> dtoOpt = oid4vpRepository.findById(id);
        if (dtoOpt.isEmpty()) {
            return false;
        }

        oid4vpRepository.deleteById(id);
        log.info("Deleted configuration: id={}", id);
        return true;
    }

    /**
     * Validate configuration JSON
     */
    public Map<String, Object> validateConfig(String configJson) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Try to parse as OID4VPConfig
            OID4VPConfig config = objectMapper.readValue(configJson, OID4VPConfig.class);

            List<String> warnings = new ArrayList<>();

            // Validate required fields
            if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
                warnings.add("baseUrl is empty");
            }

            if (config.getClientName() == null || config.getClientName().trim().isEmpty()) {
                warnings.add("clientName is empty");
            }

            if (config.getClientId() == null) {
                warnings.add("clientId is missing");
            } else {
                if (config.getClientId().getScheme() == null || config.getClientId().getScheme().trim().isEmpty()) {
                    warnings.add("clientId.scheme is empty");
                }
                if (config.getClientId().getValue() == null || config.getClientId().getValue().trim().isEmpty()) {
                    warnings.add("clientId.value is empty");
                }
            }

            result.put("valid", true);
            result.put("parsedConfig", objectMapper.convertValue(config, Map.class));

            if (!warnings.isEmpty()) {
                result.put("warnings", warnings);
            }

        } catch (JsonProcessingException e) {
            result.put("valid", false);
            result.put("error", "Invalid JSON format: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validate config JSON format (throws exception if invalid)
     */
    public void validateConfigJson(String configJson) throws OID4VPException {
        try {
            objectMapper.readValue(configJson, OID4VPConfig.class);
        } catch (JsonProcessingException e) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CONFIG_LOAD_FAILED, "Invalid config JSON format: " + e.getMessage());
        }
    }

    /**
     * Convert DTO to map
     */
    private Map<String, Object> dtoToMap(OID4VPConfigDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", dto.getId());
        map.put("type", dto.getType());

        // Parse config JSON to object
        try {
            Map<String, Object> configMap = objectMapper.readValue(dto.getConfig(), Map.class);
            map.put("config", configMap);
        } catch (JsonProcessingException e) {
            // If parsing fails, return as string
            map.put("config", dto.getConfig());
            map.put("configParseError", e.getMessage());
        }

        map.put("createdAt", dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : null);
        map.put("updatedAt", dto.getUpdatedAt() != null ? dto.getUpdatedAt().toString() : null);

        return map;
    }

    /**
     * Refresh OID4VPConfig cache if the type is OID4VP
     */
    private void refreshOID4VPConfigCacheIfNeeded(String type) {
        if (CONFIG_TYPE_OID4VP.equals(type)) {
            log.info("Refreshing OID4VP configuration cache");
            verifierConfigService.reloadConfig();
        }
    }
}