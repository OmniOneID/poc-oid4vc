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

package org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.dto;

public class PreAuthorizedCodeResponse {

    private final String preAuthorizedCode;
    private final int expiresIn;
    private final String userPin;


    public PreAuthorizedCodeResponse(String preAuthorizedCode, int expiresIn, String userPin) {
        this.preAuthorizedCode = preAuthorizedCode;
        this.expiresIn = expiresIn;
        this.userPin = userPin;
    }

    public String getPreAuthorizedCode() {
        return preAuthorizedCode;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getUserPin() {
        return userPin;
    }
}
