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

package org.omnione.did.sdk.sdjwt.crypto;

import org.omnione.did.sdk.sdjwt.util.Base64UrlUtils;
import org.omnione.did.sdk.sdjwt.exception.SDJWTException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class SignedJWT {

  private String header;
  private String payload;
  private String signature;
  private String originalString;

  private Map<String, Object> headerMap;
  private Map<String, Object> payloadMap;
  private byte[] signatureBytes;

  public SignedJWT(String header, String payload, String signature) {
    this.header = header;
    this.payload = payload;
    this.signature = signature;
    this.originalString = header + "." + payload + "." + signature;
  }

  public SignedJWT(Map<String, Object> headerMap, Map<String, Object> payloadMap)
      throws SDJWTException {
    try {
      this.headerMap = headerMap;
      this.payloadMap = payloadMap;
      ObjectMapper mapper = new ObjectMapper();
      this.header = Base64UrlUtils.encode(mapper.writeValueAsBytes(headerMap));
      this.payload = Base64UrlUtils.encode(mapper.writeValueAsBytes(payloadMap));
    } catch (JsonProcessingException e) {
      throw new SDJWTException("Failed to create SignedJWT: " + e.getMessage(), e);
    }
  }

  public static SignedJWT parse(String jwtString) throws SDJWTException {
    String[] parts = jwtString.split("\\.");
    if (parts.length != 3) {
      throw new SDJWTException("Invalid JWT format");
    }
    return new SignedJWT(parts[0], parts[1], parts[2]);
  }

  public Map<String, Object> getJWTClaimsSet() throws SDJWTException {
    try {
      if (payloadMap == null) {
        byte[] decodedPayload = Base64UrlUtils.decode(payload);
        payloadMap = new ObjectMapper().readValue(decodedPayload,
            new TypeReference<Map<String, Object>>() {
            });
      }
      return payloadMap;
    } catch (IOException e) {
      throw new SDJWTException("Failed to get JWT claims set: " + e.getMessage(), e);
    }
  }

  public Map<String, Object> getHeader() throws SDJWTException {
    try {
      if (headerMap == null) {
        byte[] decodedHeader = Base64UrlUtils.decode(header);
        headerMap = new ObjectMapper().readValue(decodedHeader,
            new TypeReference<Map<String, Object>>() {
            });
      }
      return headerMap;
    } catch (IOException e) {
      throw new SDJWTException("Failed to get JWT header: " + e.getMessage(), e);
    }
  }

  public String getSigningInput() {
    return header + "." + payload;
  }

  public byte[] getSignatureBytes() {
    if (signatureBytes == null && signature != null) {
      signatureBytes = Base64UrlUtils.decode(signature);
    }
    return signatureBytes != null ? signatureBytes.clone() : null;
  }

  public void sign(JWSSigner signer) throws SDJWTException {
    this.signatureBytes = signer.sign(getSigningInput());
    this.signature = Base64UrlUtils.encode(this.signatureBytes);
    this.originalString = getSigningInput() + "." + this.signature;
  }

  public String serialize() {
    return originalString;
  }

  public boolean verify(JWSVerifier verifier) throws SDJWTException {
    return verifier.verify(this);
  }
}
