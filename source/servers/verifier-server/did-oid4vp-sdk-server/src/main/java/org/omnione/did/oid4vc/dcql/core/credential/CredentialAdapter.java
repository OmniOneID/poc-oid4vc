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

import java.util.Map;
import java.util.Set;
import org.omnione.did.oid4vc.dcql.exception.DCQLException;

/**
 * Adapter interface for handling different credential formats.
 * Implementations of this interface provide format-specific logic for parsing,
 * extracting claims, and validating credentials.
 */
public interface CredentialAdapter {

    /**
     * Returns the set of credential formats supported by this adapter.
     *
     * @return set of supported format identifiers (e.g., "dc+sd-jwt", "mso_mdoc")
     */
    Set<String> getSupportedFormats();

    /**
     * Checks if this adapter supports the given credential format.
     *
     * @param format the credential format identifier
     * @return true if the format is supported, false otherwise
     */
    boolean supports(String format);

    /**
     * Parses a raw credential string into a ParsedCredential object.
     *
     * @param rawCredential the raw credential string
     * @return the parsed credential
     * @throws DCQLException if parsing fails
     */
    ParsedCredential parse(String rawCredential) throws DCQLException;

    /**
     * Checks if the credential matches the given metadata requirements.
     *
     * @param credential the parsed credential
     * @param metadata the metadata requirements (e.g., vct_values, doctype)
     * @return true if the credential matches, false otherwise
     */
    boolean matchesMetadata(ParsedCredential credential, Map<String, Object> metadata);

    /**
     * Extracts all claims from the credential, including disclosed claims.
     *
     * @param credential the parsed credential
     * @return map of all claims (claim name -> claim value)
     */
    Map<String, Object> extractAllClaims(ParsedCredential credential);

    /**
     * Returns the set of reserved claim names for this credential format.
     * Reserved claims are typically JWT standard claims or format-specific metadata.
     *
     * @return set of reserved claim names
     */
    Set<String> getReservedClaimNames();
}