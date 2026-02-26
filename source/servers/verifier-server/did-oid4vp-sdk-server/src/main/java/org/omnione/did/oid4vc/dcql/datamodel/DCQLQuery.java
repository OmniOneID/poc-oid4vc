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

package org.omnione.did.oid4vc.dcql.datamodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DCQLQuery {

  @JsonProperty("credentials")
  private List<CredentialQuery> credentials;

  @JsonProperty("credential_sets")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<CredentialSet> credentialSets;

  @JsonProperty("transaction_data")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<Map<String, Object>> transactionData;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CredentialQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("format")
    private String format;

    @JsonProperty("meta")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> meta;

    @JsonProperty("claims")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ClaimQuery> claims;

    @JsonProperty("claim_sets")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ClaimSet> claimSets;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("require_cryptographic_holder_binding")
    private Boolean requireCryptographicHolderBinding;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ClaimQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("path")
    private List<Object> path;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("values")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Object> values;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("max")
    private Object max;

    @JsonProperty("min")
    private Object min;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ClaimSet {

    @JsonProperty("id")
    private String id;

    @JsonProperty("claims")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ClaimQuery> claims;

    @JsonProperty("purpose")
    private String purpose;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CredentialSet {

    @JsonProperty("id")
    private String id;

    @JsonProperty("options")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<List<String>> options;

    @JsonProperty("purpose")
    private String purpose;
  }
}