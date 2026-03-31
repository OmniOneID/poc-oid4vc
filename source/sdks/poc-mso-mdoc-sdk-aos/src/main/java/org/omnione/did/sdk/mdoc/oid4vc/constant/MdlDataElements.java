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
 * ISO/IEC 18013-5 (mDL) Data Elements Constants.
 * Namespace: org.iso.18013.5.1
 */
public class MdlDataElements {

    /**
     * mDL namespace
     */
    public static final String MDL_NAMESPACE = "org.iso.18013.5.1";

    /**
     * [Mandatory] (ISO/IEC 18013-5 Table 1)
     */
    public static class Mandatory {
        public static final String FAMILY_NAME = "family_name";             // Family name
        public static final String GIVEN_NAME = "given_name";               // Given name
        public static final String BIRTH_DATE = "birth_date";               // Date of birth (Full date)
        public static final String ISSUE_DATE = "issue_date";               // Issue date
        public static final String EXPIRY_DATE = "expiry_date";             // Expiry date
        public static final String ISSUING_COUNTRY = "issuing_country";     // Issuing country (ISO 3166-1 alpha-2)
        public static final String ISSUING_AUTHORITY = "issuing_authority"; // Issuing authority
        public static final String DOCUMENT_NUMBER = "document_number";     // Document number
        public static final String PORTRAIT = "portrait";                   // Portrait (bstr)
        public static final String DRIVING_PRIVILEGES = "driving_privileges"; // Driving privileges (Array)
        public static final String UN_DISTINGUISHING_SIGN = "un_distinguishing_sign"; // UN distinguishing sign (e.g., ROK)
    }

    /**
     * [Optional] (ISO/IEC 18013-5 Table 2)
     */
    public static class Optional {
        public static final String SEX = "sex";                       // Gender (0:unknown, 1:male, 2:female, 9:not applicable)
        public static final String HEIGHT = "height";                       // Height
        public static final String WEIGHT = "weight";                       // Weight
        public static final String EYE_COLOUR = "eye_colour";               // Eye colour
        public static final String HAIR_COLOUR = "hair_colour";             // Hair colour
        public static final String BIRTH_PLACE = "birth_place";             // Place of birth
        public static final String RESIDENT_ADDRESS = "resident_address";   // Resident address
        public static final String PORTRAIT_CAPTURE_DATE = "portrait_capture_date"; // Portrait capture date
        public static final String AGE_IN_YEARS = "age_in_years";           // Age in years
        public static final String AGE_BIRTH_YEAR = "age_birth_year";       // Birth year
        public static final String ISSUING_JURISDICTION = "issuing_jurisdiction"; // Issuing jurisdiction

        public static final String ADMINISTRATIVE_NUMBER = "administrative_number"; // Administrative number
        public static final String NATIONALITY = "nationality";             // Nationality (ISO 3166-1 alpha-2)
        public static final String RESIDENT_CITY = "resident_city";         // Resident city
        public static final String RESIDENT_STATE = "resident_state";       // Resident state/province/district
        public static final String RESIDENT_POSTAL_CODE = "resident_postal_code"; // Resident postal code
        public static final String RESIDENT_COUNTRY = "resident_country";   // Resident country
        public static final String FAMILY_NAME_NATIONAL_CHARACTER = "family_name_national_character"; // Family name in national characters
        public static final String GIVEN_NAME_NATIONAL_CHARACTER = "given_name_national_character";   // Given name in national characters
        public static final String SIGNATURE_USUAL_MARK = "signature_usual_mark"; // Image of signature/usual mark

        // Biometric templates follow the format "biometric_template_xx" (e.g., biometric_template_face)
        public static final String BIOMETRIC_TEMPLATE_FACE = "biometric_template_face";                       // face
        public static final String BIOMETRIC_TEMPLATE_SIGNATURE_SIGN = "biometric_template_signature_sign";   // signature
        public static final String BIOMETRIC_TEMPLATE_FINGER = "biometric_template_finger";                   // fingerprint
        public static final String BIOMETRIC_TEMPLATE_IRIS = "biometric_template_iris";                       // iris

        public static final String AGE_OVER_18 = "age_over_18";
        public static final String AGE_OVER_19 = "age_over_19";
        public static final String AGE_OVER_21 = "age_over_21";
        // format is age_over_NN (00 to 99)
    }

    /**
     * Driving Privileges internal fields
     */
    public static class Privilege {
        public static final String VEHICLE_CATEGORY_CODE = "vehicle_category_code";
        public static final String ISSUE_DATE = "issue_date";
        public static final String EXPIRY_DATE = "expiry_date";
        public static final String CODES = "codes";
        public static class Code {
            public static final String CODE = "code";
            public static final String SIGN = "sign";
            public static final String VALUE = "value";
        }
    }
}
