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

package org.omnione.did.oid4vc.oid4vp.util.jwk;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;
import org.omnione.did.oid4vc.oid4vp.util.crypto.MultibaseUtils;

/**
 * Utility class for building JWK (JSON Web Key) objects.
 */
public class JwkUtils {

  private JwkUtils() {
    // Utility class, prevent instantiation
  }

  /**
   * Builds a JWK Map from a multibase-encoded compressed public key (secp256r1/P-256).
   *
   * @param publicKeyMultibase the compressed public key in multibase format (e.g., "z...")
   * @param keyId              the key identifier (kid)
   * @return JWK as a Map containing kty, crv, x, y, and kid
   * @throws OID4VPException if the public key is invalid
   */
  public static Map<String, Object> buildJwkFromMultibase(String publicKeyMultibase, String keyId) throws OID4VPException {
    if (publicKeyMultibase == null || publicKeyMultibase.isEmpty()) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER, "publicKeyMultibase");
    }

    // Decode multibase
    byte[] compressedKey = MultibaseUtils.decodeMultibase(publicKeyMultibase);

    // Uncompress the public key
    byte[] uncompressedKey = MultibaseUtils.uncompressPublicKey(compressedKey);

    // Extract X and Y coordinates (skip 0x04 prefix)
    byte[] xBytes = new byte[32];
    byte[] yBytes = new byte[32];
    System.arraycopy(uncompressedKey, 1, xBytes, 0, 32);
    System.arraycopy(uncompressedKey, 33, yBytes, 0, 32);

    // Base64URL encode without padding
    String x = Base64.getUrlEncoder().withoutPadding().encodeToString(xBytes);
    String y = Base64.getUrlEncoder().withoutPadding().encodeToString(yBytes);

    // Build JWK
    Map<String, Object> jwk = new LinkedHashMap<>();
    jwk.put("kty", "EC");
    jwk.put("crv", "P-256");
    jwk.put("x", x);
    jwk.put("y", y);
    if (keyId != null && !keyId.isEmpty()) {
      jwk.put("kid", keyId);
    }

    return jwk;
  }

  /**
   * Builds a JWK Map from a multibase-encoded compressed public key without key ID.
   *
   * @param publicKeyMultibase the compressed public key in multibase format
   * @return JWK as a Map containing kty, crv, x, and y
   * @throws OID4VPException if the public key is invalid
   */
  public static Map<String, Object> buildJwkFromMultibase(String publicKeyMultibase) throws OID4VPException {
    return buildJwkFromMultibase(publicKeyMultibase, null);
  }
}