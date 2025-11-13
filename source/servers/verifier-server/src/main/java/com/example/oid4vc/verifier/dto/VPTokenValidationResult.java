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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class VPTokenValidationResult {

  private boolean valid = false;
  private String message = "";
  private List<String> errors = new ArrayList<>();
  private List<String> warnings = new ArrayList<>();
  
  private int credentialCount = 0;

  private Map<String, Object> extractedData = new HashMap<>();
  
  private Map<String, Object> metadata = new HashMap<>();

  private Map<String, CredentialValidationDetail> credentialDetails = new HashMap<>();

  @Data
  public static class CredentialValidationDetail {
    private String credentialId;
    private boolean holderBindingValid;
    private boolean structureValid;
    private boolean timestampsValid;
    private String issuer;
    private String holder;
    private List<String> credentialErrors = new ArrayList<>();
  }
}