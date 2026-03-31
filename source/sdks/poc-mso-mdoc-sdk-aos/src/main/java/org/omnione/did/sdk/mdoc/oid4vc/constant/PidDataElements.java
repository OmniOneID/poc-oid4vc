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
 * EUDI Wallet PID (Person Identification Data) Constants.
 * Namespace: eu.europa.ec.eudi.pid.1
 */
public class PidDataElements {

    /**
     * Standard PID namespace
     */
    public static final String PID_NAMESPACE = "eu.europa.ec.eudi.pid.1";

    /**
     * [Mandatory] Required elements (per EUDI ARF specification)
     */
    public static class Mandatory {
        public static final String FAMILY_NAME = "family_name";             // Family name
        public static final String GIVEN_NAME = "given_name";               // Given name
        public static final String BIRTH_DATE = "birth_date";               // Date of birth
        public static final String ISSUE_DATE = "issue_date";               // Issue date
        public static final String EXPIRY_DATE = "expiry_date";             // Expiry date
        public static final String ISSUING_COUNTRY = "issuing_country";     // Issuing country (ISO 3166-1 alpha-2)
        public static final String ISSUING_AUTHORITY = "issuing_authority"; // Issuing authority
    }

    /**
     * [Optional] Optional elements
     */
    public static class Optional {
        public static final String GENDER = "gender";                       // Gender (ISO/IEC 5218)
        public static final String NATIONALITY = "nationality";             // Nationality (ISO 3166-1 alpha-2)
        public static final String RESIDENT_ADDRESS = "resident_address";   // Resident address
        public static final String BIRTH_PLACE = "birth_place";             // Place of birth
        public static final String PORTRAIT = "portrait";                   // Portrait (bstr)
        public static final String PERSONAL_NUMBER = "personal_number";     // Personal identification number
        public static final String EMAIL_ADDRESS = "email_address";         // Email address
        public static final String MOBILE_PHONE_NUMBER = "mobile_phone_number"; // Mobile phone number
        public static final String RESIDENT_CITY = "resident_city";         // Resident city
        public static final String RESIDENT_POSTAL_CODE = "resident_postal_code"; // Postal code
        public static final String RESIDENT_STREET = "resident_street";     // Resident street
    }
}
