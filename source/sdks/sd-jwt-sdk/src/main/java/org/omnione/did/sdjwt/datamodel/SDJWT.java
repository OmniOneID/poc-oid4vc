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

package org.omnione.did.sdjwt.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.omnione.did.sdjwt.exception.SDJWTException;

//TODO: Structured SD-JWT must be supported.
public class SDJWT {

  private final String credentialJwt;
  private final List<Disclosure> disclosures;
  private final String keyBindingJwt;

  public SDJWT(String credentialJwt, Collection<Disclosure> disclosures) {
    this(credentialJwt, disclosures, null);
  }

  public SDJWT(String credentialJwt, Collection<Disclosure> disclosures, String keyBindingJwt) {
    if (credentialJwt == null || credentialJwt.trim().isEmpty()) {
      throw new IllegalArgumentException("Credential JWT cannot be null or empty");
    }

    this.credentialJwt = credentialJwt.trim();
    this.disclosures = disclosures != null ?
        new ArrayList<>(disclosures) : new ArrayList<>();
    this.keyBindingJwt = keyBindingJwt != null && !keyBindingJwt.trim().isEmpty() ?
        keyBindingJwt.trim() : null;
  }

  public String getCredentialJwt() {
    return credentialJwt;
  }

  public List<Disclosure> getDisclosures() {
    return Collections.unmodifiableList(disclosures);
  }

  public String getKeyBindingJwt() {
    return keyBindingJwt;
  }

  public boolean hasKeyBindingJwt() {
    return keyBindingJwt != null;
  }

  public int getDisclosureCount() {
    return disclosures.size();
  }

  public boolean hasDisclosures() {
    return !disclosures.isEmpty();
  }

  public static SDJWT parse(String sdJwtString) {
    if (sdJwtString == null || sdJwtString.trim().isEmpty()) {
      throw new SDJWTException("SD-JWT string cannot be null or empty");
    }

    String[] parts = sdJwtString.split("~", -1);

    if (parts.length < 1) {
      throw new SDJWTException("SD-JWT must contain at least a credential JWT");
    }

    String credentialJwt = parts[0];
    if (credentialJwt.isEmpty()) {
      throw new SDJWTException("Credential JWT cannot be empty");
    }

    if (!isValidJwtFormat(credentialJwt)) {
      throw new SDJWTException("Invalid credential JWT format");
    }

    List<Disclosure> disclosures = new ArrayList<>();
    String keyBindingJwt = null;

    for (int i = 1; i < parts.length; i++) {
      String part = parts[i];

      if (part.isEmpty()) {

        continue;
      }

      if (isValidJwtFormat(part)) {

        if (i == parts.length - 1 || areAllEmpty(parts, i + 1)) {
          keyBindingJwt = part;
        } else {
          throw new SDJWTException("Key binding JWT can only be the last component");
        }
      } else {

        try {
          Disclosure disclosure = Disclosure.parse(part);
          disclosures.add(disclosure);
        } catch (SDJWTException e) {
          throw e;
        }
      }
    }

    return new SDJWT(credentialJwt, disclosures, keyBindingJwt);
  }

  private static boolean areAllEmpty(String[] parts, int fromIndex) {
    for (int i = fromIndex; i < parts.length; i++) {
      if (!parts[i].isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static boolean isValidJwtFormat(String jwt) {
    if (jwt == null || jwt.trim().isEmpty()) {
      return false;
    }

    String[] parts = jwt.split("\\.");
    return parts.length == 3 &&
        !parts[0].isEmpty() &&
        !parts[1].isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(credentialJwt);

    for (Disclosure disclosure : disclosures) {
      sb.append("~").append(disclosure.getDisclosure());
    }

    if (keyBindingJwt != null) {
      sb.append("~").append(keyBindingJwt);
    } else {

      sb.append("~");
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SDJWT sdjwt = (SDJWT) o;
    return Objects.equals(credentialJwt, sdjwt.credentialJwt) &&
        Objects.equals(disclosures, sdjwt.disclosures) &&
        Objects.equals(keyBindingJwt, sdjwt.keyBindingJwt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentialJwt, disclosures, keyBindingJwt);
  }
}