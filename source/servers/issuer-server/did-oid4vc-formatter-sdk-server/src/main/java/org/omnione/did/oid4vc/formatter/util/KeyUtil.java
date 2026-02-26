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

package org.omnione.did.oid4vc.formatter.util;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

public class KeyUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static PrivateKey getPrivateKeyObject(byte[] key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to generate PrivateKey object", e);
        }
    }

    public static PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to generate PublicKey object", e);
        }
    }

    public static byte[] unCompressPublicKey(byte[] compressedPublicKey) {
        try {
            ECNamedCurveParameterSpec ecParams = ECNamedCurveTable.getParameterSpec("Secp256r1");
            ECPoint uncompressedPoint = ecParams.getCurve().decodePoint(compressedPublicKey);
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(uncompressedPoint, ecParams);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            return keyFactory.generatePublic(pubKeySpec).getEncoded();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to uncompress public key", e);
        }
    }

    /**
     * Calculates the SHA-256 thumbprint of an X.509 certificate.
     *
     * @param base64Cert Base64-encoded DER certificate (from x5c array)
     * @return Base64url-encoded SHA-256 thumbprint
     */
    public static String calculateX5tS256(String base64Cert) {
        try {
            byte[] certBytes = Base64.getDecoder().decode(base64Cert);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] thumbprint = digest.digest(certBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(thumbprint);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to calculate certificate thumbprint: " + e.getMessage(), e);
        }
    }
}
