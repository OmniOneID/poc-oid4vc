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

package org.omnione.did.oid4vc.formatter.oid4vp.verifier.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for VP Token format verification.
 * Contains parameters needed to verify individual credential format (e.g., SD-JWT, mDoc).
 * <p>
 * Note: Protocol-level validation (e.g., DCQL) is handled separately at a higher layer.
 */
@Getter
@Builder
public class VerificationConfig {

  /**
   * Single issuer public key for individual credential verification.
   * Base64-encoded compressed public key.
   */
  private final String issuerPublicKey;

  /**
   * Single holder public key for individual credential verification.
   * Used for Key Binding JWT signature verification.
   * If null, holder public key will be extracted from cnf.jwk in the credential.
   */
  private final String holderPublicKey;

  /**
   * Verifier's client_id per OID4VP specification.
   * Used as the 'aud' claim value in Key Binding JWT verification.
   */
  private final String clientId;

  /**
   * Expected nonce value for verification.
   */
  private final String nonce;
}