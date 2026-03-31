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

package org.omnione.did.sdk.mdoc.oid4vc.constant;

/**
 * Constants for mDoc and MSO (Mobile Security Object).
 * These constants follow the ISO/IEC 18013-5 standard.
 */
public class MdocConstants {

    /**
     * Constants for IssuerSignedItem structure.
     */
    public static class IssuerSignedItem {
        public static final String DIGEST_ID = "digestID";
        public static final String RANDOM = "random";
        public static final String ELEMENT_IDENTIFIER = "elementIdentifier";
        public static final String ELEMENT_VALUE = "elementValue";
    }

    /**
     * Constants for Mobile Security Object (MSO) structure.
     */
    public static class Mso {
        public static final String VERSION = "version";
        public static final String DIGEST_ALGORITHM = "digestAlgorithm";
        public static final String DOC_TYPE = "docType";
        public static final String VALUE_DIGESTS = "valueDigests";
        public static final String DEVICE_KEY_INFO = "deviceKeyInfo";
        public static final String VALIDITY_INFO = "validityInfo";

        public static final String VERSION_1_0 = "1.0";
        public static final String SHA_256 = "SHA-256";
    }

    /**
     * Constants for ValidityInfo structure within MSO.
     */
    public static class ValidityInfo {
        public static final String SIGNED = "signed";
        public static final String VALID_FROM = "validFrom";
        public static final String VALID_UNTIL = "validUntil";
        public static final String EXPECTED_UPDATE = "expectedUpdate";
    }

    /**
     * Constants for DeviceKeyInfo structure.
     */
    public static class DeviceKeyInfo {
        public static final String DEVICE_KEY = "deviceKey";
        public static final String KEY_AUTHORIZATIONS = "keyAuthorizations";
        public static final String KEY_INFO = "keyInfo";
    }

    /**
     * Constants for DeviceResponse structure.
     */
    public static class DeviceResponse {
        public static final String VERSION = "version";
        public static final String DOCUMENTS = "documents";
        public static final String STATUS = "status";

        public static final String VERSION_1_0 = "1.0";
    }

    /**
     * Constants for Document structure.
     */
    public static class Document {
        public static final String DOC_TYPE = "docType";
        public static final String ISSUER_SIGNED = "issuerSigned";
        public static final String DEVICE_SIGNED = "deviceSigned";
    }

    /**
     * Constants for IssuerSigned structure.
     */
    public static class IssuerSigned {
        public static final String NAME_SPACES = "nameSpaces";
        public static final String ISSUER_AUTH = "issuerAuth";
    }

    /**
     * Constants for DeviceSigned structure.
     */
    public static class DeviceSigned {
        public static final String NAME_SPACES = "nameSpaces";
        public static final String DEVICE_AUTH = "deviceAuth";
        public static final String DEVICE_SIGNATURE = "deviceSignature";
    }

    /**
     * Constants for COSE_Sign1 structure.
     */
    public static class Cose {
        public static final String SIGNATURE1 = "Signature1";
    }

    /**
     * Constants for DeviceAuthentication and OID4VP Handover.
     */
    public static class DeviceAuthentication {
        public static final String DEVICE_AUTHENTICATION = "DeviceAuthentication";
        public static final String OPENID4VP_HANDOVER = "OpenID4VPHandover";
    }

    /**
     * Common Namespace constants.
     */
    public static class Namespace {
        public static final String EUDI_PID_1 = "eu.europa.ec.eudi.pid.1";
        public static final String ISO_18013_5_1 = "org.iso.18013.5.1";
    }

    public static class DocType {
        public static final String EUDI_PID_1 = "eu.europa.ec.eudi.pid.1";
        public static final String ISO_18013_5_1_MDL = "org.iso.18013.5.1.mDL";
    }
}
