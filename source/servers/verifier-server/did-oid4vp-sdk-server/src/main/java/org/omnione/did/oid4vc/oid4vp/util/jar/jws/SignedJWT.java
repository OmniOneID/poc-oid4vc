/*
 * Copyright 2026 OmniOne.
 */

package org.omnione.did.oid4vc.oid4vp.util.jar.jws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;

public class SignedJWT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Map<String, Object> header;
  private final Map<String, Object> payload;
  private byte[] signature;

  public SignedJWT(Map<String, Object> header, Map<String, Object> payload) {
    this.header = header;
    this.payload = payload;
  }

  public void sign(JWSSigner signer) throws OID4VPException {
    try {
      String signingInput = getSigningInput();
      this.signature = signer.sign(signingInput);
    } catch (OID4VPException e) {
      throw e;
    } catch (JsonProcessingException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_SERIALIZE_FAILED, e.getMessage(), e);
    }
  }

  public String serialize() throws OID4VPException {
    if (signature == null) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_NOT_SIGNED);
    }
    try {
      String headerBase64 = base64UrlEncode(OBJECT_MAPPER.writeValueAsString(header));
      String payloadBase64 = base64UrlEncode(OBJECT_MAPPER.writeValueAsString(payload));
      String signatureBase64 = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(signature);
      return headerBase64 + "." + payloadBase64 + "." + signatureBase64;
    } catch (JsonProcessingException e) {
      throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_SERIALIZE_FAILED, e.getMessage(), e);
    }
  }

  private String getSigningInput() throws JsonProcessingException {
    String headerBase64 = base64UrlEncode(OBJECT_MAPPER.writeValueAsString(header));
    String payloadBase64 = base64UrlEncode(OBJECT_MAPPER.writeValueAsString(payload));
    return headerBase64 + "." + payloadBase64;
  }

  private String base64UrlEncode(String input) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(input.getBytes(StandardCharsets.UTF_8));
  }
}