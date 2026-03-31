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

package org.omnione.did.sdk.mdoc.oid4vc.util;

/**
 * COSE signature format conversion utility (DER <-> Raw R||S).
 */
public final class CoseSignatureUtils {

  private CoseSignatureUtils() {
  }

  /**
   * Maps a COSE algorithm identifier to the corresponding JCA algorithm name.
   *
   * @param coseAlg the COSE algorithm identifier (e.g., -7 for ES256)
   * @return the JCA algorithm name
   */
  public static String mapCoseAlgToJca(int coseAlg) {
    switch (coseAlg) {
      case -7: return "SHA256withECDSA";
      case -35: return "SHA384withECDSA";
      case -36: return "SHA512withECDSA";
      default: return "SHA256withECDSA";
    }
  }

  /**
   * Returns the byte length of each R/S component for the given COSE algorithm.
   *
   * @param coseAlg the COSE algorithm identifier
   * @return the component byte length
   */
  public static int getComponentLength(int coseAlg) {
    switch (coseAlg) {
      case -7: return 32;   // P-256
      case -35: return 48;  // P-384
      case -36: return 66;  // P-521
      default: return 32;
    }
  }

  /**
   * Converts a raw ECDSA signature (R||S concatenation) to DER format.
   *
   * @param rawSignature the raw signature bytes (R||S)
   * @return the DER-encoded signature
   */
  public static byte[] convertRawToDer(byte[] rawSignature) {
    int componentLength = rawSignature.length / 2;
    byte[] r = trimLeadingZeros(rawSignature, 0, componentLength);
    byte[] s = trimLeadingZeros(rawSignature, componentLength, componentLength);

    boolean rNeedsLeadingZero = (r[0] & 0x80) != 0;
    boolean sNeedsLeadingZero = (s[0] & 0x80) != 0;

    int rLen = r.length + (rNeedsLeadingZero ? 1 : 0);
    int sLen = s.length + (sNeedsLeadingZero ? 1 : 0);
    int totalLen = 2 + rLen + 2 + sLen;

    byte[] der = new byte[2 + totalLen];
    int pos = 0;
    der[pos++] = 0x30;
    der[pos++] = (byte) totalLen;
    der[pos++] = 0x02;
    der[pos++] = (byte) rLen;
    if (rNeedsLeadingZero) {
      der[pos++] = 0x00;
    }
    System.arraycopy(r, 0, der, pos, r.length);
    pos += r.length;
    der[pos++] = 0x02;
    der[pos++] = (byte) sLen;
    if (sNeedsLeadingZero) {
      der[pos++] = 0x00;
    }
    System.arraycopy(s, 0, der, pos, s.length);

    return der;
  }

  /**
   * Converts a DER-encoded ECDSA signature to raw R||S format.
   *
   * @param derSignature the DER-encoded signature
   * @param coseAlg      the COSE algorithm identifier (determines component length)
   * @return the raw signature bytes (R||S)
   */
  public static byte[] convertDerToRaw(byte[] derSignature, int coseAlg) {
    int componentLength = getComponentLength(coseAlg);
    byte[] raw = new byte[componentLength * 2];

    // Parse DER: 0x30 <len> 0x02 <rLen> <r> 0x02 <sLen> <s>
    int pos = 2; // skip SEQUENCE tag and length

    // R component
    pos++; // skip 0x02 INTEGER tag
    int rLen = derSignature[pos++] & 0xFF;
    int rOffset = pos;
    pos += rLen;

    // S component
    pos++; // skip 0x02 INTEGER tag
    int sLen = derSignature[pos++] & 0xFF;
    int sOffset = pos;

    copyComponent(derSignature, rOffset, rLen, raw, 0, componentLength);
    copyComponent(derSignature, sOffset, sLen, raw, componentLength, componentLength);

    return raw;
  }

  private static byte[] trimLeadingZeros(byte[] data, int offset, int length) {
    int start = offset;
    int end = offset + length;
    while (start < end - 1 && data[start] == 0) {
      start++;
    }
    byte[] result = new byte[end - start];
    System.arraycopy(data, start, result, 0, result.length);
    return result;
  }

  private static void copyComponent(byte[] src, int srcOffset, int srcLen,
      byte[] dst, int dstOffset, int componentLength) {
    if (srcLen > componentLength) {
      int skip = srcLen - componentLength;
      System.arraycopy(src, srcOffset + skip, dst, dstOffset, componentLength);
    } else if (srcLen < componentLength) {
      int pad = componentLength - srcLen;
      System.arraycopy(src, srcOffset, dst, dstOffset + pad, srcLen);
    } else {
      System.arraycopy(src, srcOffset, dst, dstOffset, componentLength);
    }
  }
}
