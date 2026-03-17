/*
 * Copyright 2025 OmniOne.
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

package org.omnione.did.sdk.oid4vc.data.dto;

import com.google.gson.annotations.SerializedName;

public class WalletData {
    @SerializedName("format")
    private String format;

    @SerializedName("credential")
    private Object credential;

    /**
     * Gets the format of the wallet data.
     * @return the format string.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the format of the wallet data.
     * @param format the format string to set.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Gets the credential object.
     * @return the credential object.
     */
    public Object getCredential() {
        return credential;
    }

    /**
     * Sets the credential object.
     * @param credential the credential object to set.
     */
    public void setCredential(Object credential) {
        this.credential = credential;
    }
}
