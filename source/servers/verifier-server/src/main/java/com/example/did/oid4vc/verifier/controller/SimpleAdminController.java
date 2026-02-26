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

package com.example.did.oid4vc.verifier.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.service.ConfigService;
import org.omnione.did.oid4vc.oid4vp.service.ScopeToDCQLMapperService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/oid4vp/config")
@RequiredArgsConstructor
public class SimpleAdminController {

    private final ConfigService configService;
    private final ScopeToDCQLMapperService scopeMapperService;
    private final ObjectMapper objectMapper;

    /**
     * Config management HTML page
     */
    @GetMapping
    public String configPage() {
        return "verifier/config";
    }

    /**
     * Get all configurations
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllConfigs() {
        try {
            List<Map<String, Object>> configs = configService.getAllConfigs();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("Failed to get all configs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get configuration by ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConfigById(@PathVariable Long id) {
        try {
            Map<String, Object> config = configService.getConfigById(id);
            if (config == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Failed to get config by id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new configuration
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody Map<String, Object> request) {
        try {
            String type = (String) request.get("type");
            Object configObj = request.get("config");

            if (type == null || type.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "type is required"
                ));
            }

            if (configObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "config is required"
                ));
            }

            String configJson;
            if (configObj instanceof String) {
                configJson = (String) configObj;
            } else {
                configJson = objectMapper.writeValueAsString(configObj);
            }

            Map<String, Object> created = configService.createConfig(type, configJson);
            return ResponseEntity.ok(created);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse config JSON", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_json",
                "error_description", "Invalid config JSON format"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to create config", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Update configuration by ID
     */
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String type = (String) request.get("type");
            Object configObj = request.get("config");

            String configJson = null;
            if (configObj != null) {
                if (configObj instanceof String) {
                    configJson = (String) configObj;
                } else {
                    configJson = objectMapper.writeValueAsString(configObj);
                }
            }

            Map<String, Object> updated = configService.updateConfig(id, type, configJson);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse config JSON", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_json",
                "error_description", "Invalid config JSON format"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to update config: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Delete configuration by ID
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        try {
            boolean deleted = configService.deleteConfig(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Configuration deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to delete config: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Validate configuration JSON
     */
    @PostMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateConfig(@RequestBody Map<String, Object> request) {
        try {
            Object configObj = request.get("config");

            if (configObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "config is required"
                ));
            }

            String configJson;
            if (configObj instanceof String) {
                configJson = (String) configObj;
            } else {
                configJson = objectMapper.writeValueAsString(configObj);
            }

            Map<String, Object> result = configService.validateConfig(configJson);
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Invalid JSON format: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to validate config", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }

    // ===== DCQL Scope Mapping API =====

    /**
     * DCQL Scope management HTML page
     */
    @GetMapping("/scope")
    public String scopePage() {
        return "verifier/scope";
    }

    /**
     * Get all DCQL scope mappings
     */
    @GetMapping("/api/scope")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllScopes() {
        try {
            List<Map<String, Object>> scopes = scopeMapperService.getAllScopeMappings();
            return ResponseEntity.ok(scopes);
        } catch (Exception e) {
            log.error("Failed to get all DCQL scopes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get DCQL scope mapping by ID
     */
    @GetMapping("/api/scope/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getScopeById(@PathVariable Long id) {
        try {
            Map<String, Object> scope = scopeMapperService.getScopeMappingById(id);
            if (scope == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(scope);
        } catch (Exception e) {
            log.error("Failed to get DCQL scope by id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new DCQL scope mapping
     */
    @PostMapping("/api/scope")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createScope(@RequestBody Map<String, Object> request) {
        try {
            String scope = (String) request.get("scope");
            Object dcqlQueryObj = request.get("dcqlQuery");
            String description = (String) request.get("description");

            if (scope == null || scope.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "scope is required"
                ));
            }

            if (dcqlQueryObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "dcqlQuery is required"
                ));
            }

            String dcqlJson;
            if (dcqlQueryObj instanceof String) {
                dcqlJson = (String) dcqlQueryObj;
            } else {
                dcqlJson = objectMapper.writeValueAsString(dcqlQueryObj);
            }

            Map<String, Object> created = scopeMapperService.createScopeMapping(scope, dcqlJson, description);
            return ResponseEntity.ok(created);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DCQL query JSON", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_json",
                "error_description", "Invalid DCQL query JSON format"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to create DCQL scope", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Update DCQL scope mapping by ID
     */
    @PutMapping("/api/scope/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateScope(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String scope = (String) request.get("scope");
            Object dcqlQueryObj = request.get("dcqlQuery");
            String description = (String) request.get("description");
            Boolean enabled = (Boolean) request.get("enabled");

            String dcqlJson = null;
            if (dcqlQueryObj != null) {
                if (dcqlQueryObj instanceof String) {
                    dcqlJson = (String) dcqlQueryObj;
                } else {
                    dcqlJson = objectMapper.writeValueAsString(dcqlQueryObj);
                }
            }

            Map<String, Object> updated = scopeMapperService.updateScopeMappingById(
                id, scope, dcqlJson, description, enabled);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DCQL query JSON", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_json",
                "error_description", "Invalid DCQL query JSON format"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to update DCQL scope: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Delete DCQL scope mapping by ID
     */
    @DeleteMapping("/api/scope/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteScope(@PathVariable Long id) {
        try {
            boolean deleted = scopeMapperService.deleteScopeMappingById(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "DCQL scope mapping deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to delete DCQL scope: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Toggle DCQL scope mapping enabled status
     */
    @PutMapping("/api/scope/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleScopeEnabled(@PathVariable Long id) {
        try {
            Map<String, Object> updated = scopeMapperService.toggleEnabledById(id);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to toggle DCQL scope enabled: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "server_error",
                "error_description", e.getMessage()
            ));
        }
    }

    /**
     * Validate DCQL query JSON
     */
    @PostMapping("/api/scope/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateDcqlQuery(@RequestBody Map<String, Object> request) {
        try {
            Object dcqlQueryObj = request.get("dcqlQuery");

            if (dcqlQueryObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "dcqlQuery is required"
                ));
            }

            String dcqlJson;
            if (dcqlQueryObj instanceof String) {
                dcqlJson = (String) dcqlQueryObj;
            } else {
                dcqlJson = objectMapper.writeValueAsString(dcqlQueryObj);
            }

            Map<String, Object> result = scopeMapperService.validateDcqlQueryJson(dcqlJson);
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Invalid JSON format: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to validate DCQL query", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }
}