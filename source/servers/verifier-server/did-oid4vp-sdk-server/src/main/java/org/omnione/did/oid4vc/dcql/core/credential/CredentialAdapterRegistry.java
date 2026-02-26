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

package org.omnione.did.oid4vc.dcql.core.credential;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.dcql.core.credential.impl.OpenDIDVCCredentialAdapter;
import org.omnione.did.oid4vc.dcql.core.credential.impl.SDJWTCredentialAdapter;

/**
 * Registry for managing credential adapters.
 * Provides centralized access to format-specific credential adapters.
 */
@Slf4j
public class CredentialAdapterRegistry {

    private static final CredentialAdapterRegistry INSTANCE = new CredentialAdapterRegistry();

    private final List<CredentialAdapter> adapters = new ArrayList<>();

    private CredentialAdapterRegistry() {
        // Register default adapters
        registerAdapter(new SDJWTCredentialAdapter());
        registerAdapter(new OpenDIDVCCredentialAdapter());
        log.info("CredentialAdapterRegistry initialized with {} adapters", adapters.size());
    }

    /**
     * Returns the singleton instance of the registry.
     */
    public static CredentialAdapterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a new credential adapter.
     *
     * @param adapter the adapter to register
     */
    public void registerAdapter(CredentialAdapter adapter) {
        if (adapter != null) {
            adapters.add(adapter);
            log.info("Registered credential adapter for formats: {}", adapter.getSupportedFormats());
        }
    }

    /**
     * Finds an adapter that supports the given format.
     *
     * @param format the credential format
     * @return Optional containing the adapter if found
     */
    public Optional<CredentialAdapter> findAdapter(String format) {
        return adapters.stream()
            .filter(adapter -> adapter.supports(format))
            .findFirst();
    }

    /**
     * Finds an adapter by analyzing the raw credential content.
     * This is useful when the format is not explicitly known.
     *
     * @param rawCredential the raw credential string
     * @return Optional containing the adapter if detected
     */
    public Optional<CredentialAdapter> detectAdapter(String rawCredential) {
        if (rawCredential == null || rawCredential.trim().isEmpty()) {
            return Optional.empty();
        }

        // SD-JWT detection: contains '~' separator
        if (rawCredential.contains("~")) {
            return findAdapter("dc+sd-jwt");
        }

        // OpenDID VC detection: JSON-LD with @context and VerifiablePresentation type
        if (rawCredential.contains("@context") && rawCredential.contains("VerifiablePresentation")) {
            return findAdapter("opendid_vc");
        }

        // Add more detection logic for other formats as needed

        return Optional.empty();
    }

    /**
     * Returns all supported formats across all registered adapters.
     *
     * @return set of all supported format identifiers
     */
    public Set<String> getAllSupportedFormats() {
        Set<String> formats = new HashSet<>();
        for (CredentialAdapter adapter : adapters) {
            formats.addAll(adapter.getSupportedFormats());
        }
        return Collections.unmodifiableSet(formats);
    }

    /**
     * Checks if a format is supported by any registered adapter.
     *
     * @param format the format to check
     * @return true if supported, false otherwise
     */
    public boolean isFormatSupported(String format) {
        return adapters.stream().anyMatch(adapter -> adapter.supports(format));
    }

    /**
     * Returns all registered adapters.
     *
     * @return unmodifiable list of adapters
     */
    public List<CredentialAdapter> getAllAdapters() {
        return Collections.unmodifiableList(adapters);
    }
}