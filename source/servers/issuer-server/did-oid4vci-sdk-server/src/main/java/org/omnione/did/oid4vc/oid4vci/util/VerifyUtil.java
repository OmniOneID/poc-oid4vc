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

package org.omnione.did.oid4vc.oid4vci.util;

import org.omnione.did.crypto.enums.DigestType;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.exception.CryptoErrorCode;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.DigestUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.crypto.util.SignatureUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class VerifyUtil {
    public static void verifySignature(String origin, String signature, String compressedPubKey) throws CryptoException {
        byte[] hashedSignOrignData = DigestUtils.getDigest(origin.getBytes(StandardCharsets.UTF_8), DigestType.SHA256);
        byte[] signatureByte;
        byte[] compressPublicKeyBytes;
        try {
            signatureByte = Base64.getUrlDecoder().decode(signature);
            compressPublicKeyBytes = MultiBaseUtils.decode(compressedPubKey);
        } catch (CryptoException e) {
            throw new CryptoException(CryptoErrorCode.ERR_CODE_CRYPTOUTIL_ENCDEC_FAIL);
        }

        try {
            SignatureUtils.verifyCompactSignWithCompressedKey(compressPublicKeyBytes, hashedSignOrignData, signatureByte, EccCurveType.Secp256r1);
        } catch (CryptoException e) {
            throw new CryptoException(CryptoErrorCode.ERR_CODE_SIGNATUREUTIL_INVALID_SIGN_VALUE);
        }
    }
}
