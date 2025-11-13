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

package org.omnione.did.oid4vc.oid4vp.core;

import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.core.builder.KeyBindingJWTBuilder;
import org.omnione.did.sdjwt.exception.SDJWTException;
import org.omnione.did.oid4vc.exception.OID4VCException;

import java.security.PrivateKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OID4VPHandler {

  public static String createVPToken(String sdJwtVC,
      Set<String> requestedClaims,
      PrivateKey holderPrivateKey,
      String audience,
      String nonce) throws OID4VCException {

    try {
      SDJWT originalSDJWT = SDJWT.parse(sdJwtVC);

      List<Disclosure> selectedDisclosures = SelectiveDisclosureProcessor.filterDisclosures(
          originalSDJWT.getDisclosures(), requestedClaims);

      SDJWT filteredSDJWT = new SDJWT(
          originalSDJWT.getCredentialJwt(),
          selectedDisclosures
      );

      String sdJwtForHash = filteredSDJWT.getCredentialJwt() + "~" +
          selectedDisclosures.stream()
              .map(Disclosure::getDisclosure)
              .collect(Collectors.joining("~")) + "~";

      String keyBindingJWT = KeyBindingJWTBuilder.createKeyBindingJWT(
          holderPrivateKey, audience, nonce, sdJwtForHash);

      SDJWT finalSDJWT = new SDJWT(
          filteredSDJWT.getCredentialJwt(),
          filteredSDJWT.getDisclosures(),
          keyBindingJWT
      );

      String vpToken = finalSDJWT.toString();

      return vpToken;
    } catch (SDJWTException e) {
      throw new OID4VCException("Failed to create VP token", e);
    }
  }

  public static String createVPTokenWithDcqlId(String sdJwtVC,
      Set<String> requestedClaims,
      String dcqlId,
      PrivateKey holderPrivateKey,
      String audience,
      String nonce) throws SDJWTException {

    String vpToken = createVPToken(sdJwtVC, requestedClaims, holderPrivateKey, audience, nonce);

    return  "{\"" + dcqlId + "\":[\"" + vpToken + "\"]}";
  }
}