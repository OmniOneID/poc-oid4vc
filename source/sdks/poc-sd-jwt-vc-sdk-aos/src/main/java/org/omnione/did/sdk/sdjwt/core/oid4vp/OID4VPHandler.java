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

package org.omnione.did.sdk.sdjwt.core.oid4vp;

import org.omnione.did.sdk.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdk.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdk.sdjwt.core.builder.KeyBindingJWTBuilder;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;

import java.security.PrivateKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OID4VPHandler {

  public static String createVPToken(String sdJwtVC,
      Set<String> requestedClaims,
      PrivateKey holderPrivateKey,
      String audience,
      String nonce) throws SDJWTException {

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
      throw new SDJWTException("Failed to create VP token", e);
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

  /**
   * Creates a VP token with holder's x5c certificate chain in the Key Binding JWT header.
   *
   * @param sdJwtVC the SD-JWT VC to present
   * @param requestedClaims the set of claim names to disclose
   * @param holderPrivateKey the holder's private key for signing KB-JWT
   * @param holderX5cChain the holder's X.509 certificate chain for KB-JWT header
   * @param audience the audience claim for KB-JWT
   * @param nonce the nonce claim for KB-JWT
   * @return the VP token string
   * @throws SDJWTException if VP token creation fails
   */
  public static String createVPToken(String sdJwtVC,
      Set<String> requestedClaims,
      PrivateKey holderPrivateKey,
      List<String> holderX5cChain,
      String audience,
      String nonce) throws SDJWTException {

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
          holderPrivateKey, holderX5cChain, audience, nonce, sdJwtForHash);

      SDJWT finalSDJWT = new SDJWT(
          filteredSDJWT.getCredentialJwt(),
          filteredSDJWT.getDisclosures(),
          keyBindingJWT
      );

      return finalSDJWT.toString();
    } catch (SDJWTException e) {
      throw new SDJWTException("Failed to create VP token", e);
    }
  }

  /**
   * Creates a VP token with DCQL ID and holder's x5c certificate chain in the Key Binding JWT header.
   *
   * @param sdJwtVC the SD-JWT VC to present
   * @param requestedClaims the set of claim names to disclose
   * @param dcqlId the DCQL credential ID
   * @param holderPrivateKey the holder's private key for signing KB-JWT
   * @param holderX5cChain the holder's X.509 certificate chain for KB-JWT header
   * @param audience the audience claim for KB-JWT
   * @param nonce the nonce claim for KB-JWT
   * @return the VP token string with DCQL wrapper
   * @throws SDJWTException if VP token creation fails
   */
  public static String createVPTokenWithDcqlId(String sdJwtVC,
      Set<String> requestedClaims,
      String dcqlId,
      PrivateKey holderPrivateKey,
      List<String> holderX5cChain,
      String audience,
      String nonce) throws SDJWTException {

    String vpToken = createVPToken(sdJwtVC, requestedClaims, holderPrivateKey, holderX5cChain, audience, nonce);

    return  "{\"" + dcqlId + "\":[\"" + vpToken + "\"]}";
  }
}