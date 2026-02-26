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

package org.omnione.did.oid4vc.oid4vp.util.crypto;

import java.math.BigInteger;
import java.util.Base64;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPErrorCode;
import org.omnione.did.oid4vc.oid4vp.exception.OID4VPException;

/**
 * Utility class for multibase encoding/decoding and EC public key operations.
 */
public class MultibaseUtils {

    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE58_BASE = BigInteger.valueOf(58);

    // P-256 (secp256r1) curve parameters
    private static final BigInteger P256_P = new BigInteger("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16);
    private static final BigInteger P256_A = new BigInteger("ffffffff00000001000000000000000000000000fffffffffffffffffffffffc", 16);
    private static final BigInteger P256_B = new BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16);

    private MultibaseUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Decodes a multibase-encoded string.
     * Currently supports base58btc (prefix 'z').
     *
     * @param multibase the multibase-encoded string
     * @return decoded byte array
     * @throws OID4VPException if the input is invalid or unsupported
     */
    public static byte[] decodeMultibase(String multibase) throws OID4VPException {
        if (multibase == null || multibase.isEmpty()) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER, "multibase");
        }

        char prefix = multibase.charAt(0);
        String encoded = multibase.substring(1);

        if (prefix == 'm') {
            return Base64.getDecoder().decode(encoded);
        } else if (prefix == 'z') {
            return decodeBase58(encoded);
        } else {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_UNSUPPORTED_FORMAT, "Unsupported multibase prefix: " + prefix);
        }
    }

    /**
     * Decodes a Base58 (Bitcoin alphabet) encoded string.
     *
     * @param input the Base58-encoded string
     * @return decoded byte array
     * @throws OID4VPException if the input contains invalid characters
     */
    public static byte[] decodeBase58(String input) throws OID4VPException {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }

        BigInteger num = BigInteger.ZERO;
        for (char c : input.toCharArray()) {
            int digit = BASE58_ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_DECODE_FAILED, "Invalid Base58 character: " + c);
            }
            num = num.multiply(BASE58_BASE).add(BigInteger.valueOf(digit));
        }

        byte[] bytes = num.toByteArray();

        // Remove leading zero if present (BigInteger sign bit)
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }

        // Count leading zeros in input (represented as '1' in Base58)
        int leadingZeros = 0;
        for (char c : input.toCharArray()) {
            if (c == '1') {
                leadingZeros++;
            } else {
                break;
            }
        }

        // Add leading zeros back
        if (leadingZeros > 0) {
            byte[] tmp = new byte[leadingZeros + bytes.length];
            System.arraycopy(bytes, 0, tmp, leadingZeros, bytes.length);
            bytes = tmp;
        }

        return bytes;
    }

    /**
     * Uncompresses a secp256r1 (P-256) compressed public key.
     *
     * @param compressedKey the compressed public key (33 bytes, starting with 0x02 or 0x03)
     * @return uncompressed public key (65 bytes, starting with 0x04)
     * @throws OID4VPException if the input is invalid
     */
    public static byte[] uncompressPublicKey(byte[] compressedKey) throws OID4VPException {
        if (compressedKey == null) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_GENERAL_NULL_PARAMETER, "compressedKey");
        }

        // Already uncompressed
        if (compressedKey.length == 65 && compressedKey[0] == 0x04) {
            return compressedKey;
        }

        if (compressedKey.length != 33) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_CRYPTO_INVALID_KEY_LENGTH, "Invalid compressed public key length: " + compressedKey.length);
        }

        if (compressedKey[0] != 0x02 && compressedKey[0] != 0x03) {
            throw new OID4VPException(OID4VPErrorCode.ERR_CODE_JWS_INVALID_KEY, "Invalid compressed public key prefix: " + compressedKey[0]);
        }

        // Extract X coordinate
        byte[] xBytes = new byte[32];
        System.arraycopy(compressedKey, 1, xBytes, 0, 32);
        BigInteger x = new BigInteger(1, xBytes);

        // Calculate y^2 = x^3 + ax + b (mod p)
        BigInteger ySquared = x.pow(3).add(P256_A.multiply(x)).add(P256_B).mod(P256_P);

        // Calculate y = sqrt(y^2) mod p
        // For p ≡ 3 (mod 4): y = y^((p+1)/4) mod p
        BigInteger y = ySquared.modPow(P256_P.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), P256_P);

        // Check parity (02 = even Y, 03 = odd Y)
        boolean yIsOdd = y.testBit(0);
        boolean shouldBeOdd = (compressedKey[0] == 0x03);
        if (yIsOdd != shouldBeOdd) {
            y = P256_P.subtract(y);
        }

        // Build uncompressed key (0x04 || X || Y)
        byte[] uncompressedKey = new byte[65];
        uncompressedKey[0] = 0x04;

        byte[] xResult = toFixedLengthBytes(x, 32);
        byte[] yResult = toFixedLengthBytes(y, 32);
        System.arraycopy(xResult, 0, uncompressedKey, 1, 32);
        System.arraycopy(yResult, 0, uncompressedKey, 33, 32);

        return uncompressedKey;
    }

    /**
     * Converts a BigInteger to a fixed-length byte array.
     * Pads with leading zeros or truncates leading zeros as needed.
     *
     * @param value  the BigInteger value
     * @param length the desired byte array length
     * @return byte array of the specified length
     */
    public static byte[] toFixedLengthBytes(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();

        if (bytes.length == length) {
            return bytes;
        } else if (bytes.length > length) {
            // Truncate leading bytes (usually leading zero from BigInteger sign bit)
            byte[] result = new byte[length];
            System.arraycopy(bytes, bytes.length - length, result, 0, length);
            return result;
        } else {
            // Pad with leading zeros
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, length - bytes.length, bytes.length);
            return result;
        }
    }
}