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

package org.omnione.did.sdk.oid4vc.data.dto.tec;

public class Header {
    private String alg;
    private String typ;
    private String kid;

    /**
     * Constructs a new Header with the specified algorithm, type, and key ID.
     * @param alg the algorithm used for the header.
     * @param typ the type of the header.
     * @param kid the key ID used for the header.
     */
    public Header(String alg, String typ, String kid) {
        this.alg = alg;
        this.typ = typ;
        this.kid = kid;
    }

    /**
     * Gets the algorithm used in the header.
     * @return the algorithm string.
     */
    public String getAlg() {
        return alg;
    }

    /**
     * Sets the algorithm used in the header.
     * @param alg the algorithm string to set.
     */
    public void setAlg(String alg) {
        this.alg = alg;
    }

    /**
     * Gets the type of the header.
     * @return the type string.
     */
    public String getTyp() {
        return typ;
    }

    /**
     * Sets the type of the header.
     * @param typ the type string to set.
     */
    public void setTyp(String typ) {
        this.typ = typ;
    }

    /**
     * Gets the key ID of the header.
     * @return the key ID string.
     */
    public String getKid() {
        return kid;
    }

    /**
     * Sets the key ID of the header.
     * @param kid the key ID string to set.
     */
    public void setKid(String kid) {
        this.kid = kid;
    }
}
