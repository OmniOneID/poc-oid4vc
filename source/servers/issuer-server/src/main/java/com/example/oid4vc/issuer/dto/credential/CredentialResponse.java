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

package com.example.oid4vc.issuer.dto.credential;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialResponse {

//    @JsonProperty("format")
//    private String format;
    @JsonProperty("credentials")
    private List<Credential> credentials;
//    @JsonProperty("c_nonce")
//    private String cNonce;
//    @JsonProperty("c_nonce_expires_in")
//    private Integer cNonceExpiresIn;
    @JsonProperty("notification_id")
    private String notificationId;

}

