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

package org.omnione.did.oid4vc.oid4vci.adapter;

import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.VcGenerator;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIErrorCode;
import org.omnione.did.oid4vc.oid4vci.exception.OID4VCIException;
import org.omnione.did.oid4vc.oid4vci.spi.ProtocolIssuer;

import java.util.Map;
import java.util.Set;

/**
 * A universal adapter that bridges the OID4VCI protocol flow to any specific VcGenerator.
 * It is configured with a list of supported formats (or credential configuration IDs)
 * and delegates the actual creation logic to the injected VcGenerator.
 */
public class Oid4vcIssuer implements ProtocolIssuer {

    private final VcGenerator generator;
    private final Set<String> supportedFormats;

    public Oid4vcIssuer(VcGenerator generator, Set<String> supportedFormats) {
        this.generator = generator;
        this.supportedFormats = supportedFormats;
    }

    @Override
    public boolean supports(String format) {
        return supportedFormats.contains(format);
    }

    @Override
    public Object issueCredential(Map<String, Object> claims, Map<String, Object> keyInfo) throws OID4VCIException {
        // This generic adapter assumes that the generator returns the correct object
        // expected by the OID4VCI response 'credential' field (usually a String).
        try {
            return generator.generate(claims, keyInfo);
        } catch (FormatterException e) {
            throw new OID4VCIException(OID4VCIErrorCode.ERR_CODE_ISSUE_GENERATE_FAILED, e.getMessage(), e);
        }
    }
}
