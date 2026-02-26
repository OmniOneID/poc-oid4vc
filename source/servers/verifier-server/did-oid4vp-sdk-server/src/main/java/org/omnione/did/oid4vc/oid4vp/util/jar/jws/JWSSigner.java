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

package org.omnione.did.oid4vc.oid4vp.util.jar.jws;

import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;

/**
 * JWS Signer interface for signing JWT tokens.
 */
public interface JWSSigner {

  /**
   * Signs the given signing input.
   *
   * @param signingInput the input string to sign (header.payload)
   * @return the signature bytes
   * @throws OID4VPException if signing fails
   */
  byte[] sign(String signingInput) throws OID4VPException;
}
