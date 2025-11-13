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

package com.example.oid4vc.verifier.dto;

import lombok.Data;

import java.util.Map;

@Data
public class VerificationSession {
    private String state;
    private String nonce;
    private String dcqlQuery;
    private String responseMode;
    private Map<String, Object> verificationResult;
    private String responseCode;
    private String requestId;
    private long createdAt;
}