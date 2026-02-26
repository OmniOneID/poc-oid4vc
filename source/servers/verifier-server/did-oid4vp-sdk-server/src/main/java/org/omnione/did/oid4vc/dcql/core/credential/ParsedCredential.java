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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * A format-agnostic representation of a parsed credential.
 * This class provides a unified view of credentials regardless of their original format.
 */
@Getter
@Builder
public class ParsedCredential {

    /**
     * The credential format identifier (e.g., "dc+sd-jwt", "mso_mdoc", "jwt_vc_json")
     */
    private final String format;

    /**
     * The raw credential string as originally provided
     */
    private final String rawCredential;

    /**
     * Base claims extracted directly from the credential payload.
     * For SD-JWT, this is the JWT payload.
     * For mDoc, this would be the namespaced data elements.
     */
    @Builder.Default
    private final Map<String, Object> baseClaims = new HashMap<>();

    /**
     * All claims including disclosed/selective disclosure claims.
     * This represents the complete set of claims after processing disclosures.
     */
    @Builder.Default
    private final Map<String, Object> allClaims = new HashMap<>();

    /**
     * Format-specific metadata extracted from the credential.
     * For SD-JWT: vct, _sd_alg, etc.
     * For mDoc: docType, validityInfo, etc.
     */
    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * The original parsed credential object in its native format.
     * For SD-JWT: SDJWT instance
     * For mDoc: MDoc instance
     * This allows format-specific operations when needed.
     */
    private final Object nativeCredential;

    /**
     * Returns an unmodifiable view of the base claims.
     */
    public Map<String, Object> getBaseClaims() {
        return Collections.unmodifiableMap(baseClaims);
    }

    /**
     * Returns an unmodifiable view of all claims.
     */
    public Map<String, Object> getAllClaims() {
        return Collections.unmodifiableMap(allClaims);
    }

    /**
     * Returns an unmodifiable view of the metadata.
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Gets a specific claim value by name.
     *
     * @param claimName the claim name
     * @return the claim value, or null if not present
     */
    public Object getClaim(String claimName) {
        return allClaims.get(claimName);
    }

    /**
     * Checks if a specific claim exists.
     *
     * @param claimName the claim name
     * @return true if the claim exists, false otherwise
     */
    public boolean hasClaim(String claimName) {
        return allClaims.containsKey(claimName);
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @return the metadata value, or null if not present
     */
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * Casts and returns the native credential to the specified type.
     *
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the native credential cast to the specified type
     * @throws ClassCastException if the native credential is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T getNativeCredentialAs(Class<T> type) {
        return (T) nativeCredential;
    }
}