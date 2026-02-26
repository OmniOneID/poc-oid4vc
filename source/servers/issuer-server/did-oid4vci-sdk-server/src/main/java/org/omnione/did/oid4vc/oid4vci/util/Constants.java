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

package org.omnione.did.oid4vc.oid4vci.util;

public final class Constants {

    private Constants() {}

    public static final String CREDENTIAL_ISSUER = "http://192.168.3.130:8096";
    public static final String AUTHORIZATION_SERVER = "https://example.com/auth/realms/test";
    public static final String CREDENTIAL_ENDPOINT = "https://example.com/issuer/credential";
    public static final String[] CREDENTIAL_RESPONSE_ENCRYPTION_ALG_VALUES_SUPPORTED = {"RSA-OAEP", "RSA-OAEP-256"};
    public static final String[] CREDENTIAL_RESPONSE_ENCRYPTION_ENC_VALUES_SUPPORTED = {"A128CBC-HS256", "A128GCM"};
    public static final boolean REQUIRE_CREDENTIAL_RESPONSE_ENCRYPTION = false;
    public static final boolean CREDENTIAL_IDENTIFIERS_SUPPORTED = false;

    public static final String TOKEN_TYPE_BEARER = "bearer";
    public static final int EXPIRES_IN = 60;
    public static final int C_NONCE_EXPIRES_IN = 86400;

    public static final String VC_FORMAT_JWT_VC = "jwt_vc";
    public static final String VC_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    public static final String VC_TYPE_VERIFIABLE_CREDENTIAL = "VerifiableCredential";
    public static final String VC_TYPE_UNIVERSITY_DEGREE = "UniversityDegree";
    public static final String VC_AUDIENCE = "https://example.com/verifier";

    public static final String PRE_AUTHORIZED_CODE_TYPE = "P";
    public static final String AUTHORIZATION_CODE_TYPE = "A";

}
