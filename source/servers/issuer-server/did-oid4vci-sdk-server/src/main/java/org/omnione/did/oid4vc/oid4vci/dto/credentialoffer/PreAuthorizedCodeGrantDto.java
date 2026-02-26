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

package org.omnione.did.oid4vc.oid4vci.dto.credentialoffer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PreAuthorizedCodeGrantDto {

    @JsonProperty("issuer_state")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String issuerState;

    @JsonProperty("pre-authorized_code")
    private String preAuthorizedCode;

    // tx_code option
    @JsonProperty("tx_code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TxCodeDto txCode;

}