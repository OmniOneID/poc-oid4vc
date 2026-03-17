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

package org.omnione.did.sdk.oid4vc.util;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.omnione.did.sdk.core.keymanager.supportalgorithm.Secp256R1Manager;
import org.omnione.did.sdk.utility.DataModels.DigestEnum;
import org.omnione.did.sdk.utility.DigestUtils;
import org.omnione.did.sdk.utility.MultibaseUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CryptoUtil {

    /**
     * Converts a byte array representing a private key into a PrivateKey object.
     *
     * @param privateKeyBytes The byte array of the private key.
     * @return The PrivateKey object.
     * @throws RuntimeException if the key specification or algorithm is invalid.
     */
    public static PrivateKey getPrivateKeyObject(byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a byte array representing a public key into a PublicKey object.
     *
     * @param publicKeyBytes The byte array of the public key.
     * @return The PublicKey object.
     * @throws RuntimeException if the key specification or algorithm is invalid.
     */
    public static PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uncompresses a compressed elliptic curve public key.
     *
     * @param compressedPublicKey The byte array of the compressed public key.
     * @return The byte array of the uncompressed public key.
     * @throws RuntimeException if an error occurs during uncompression.
     */
    public static byte[] unCompressPublicKey(byte[] compressedPublicKey) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParams = parameters.getParameterSpec(ECParameterSpec.class);

            byte[] xBytes = Arrays.copyOfRange(compressedPublicKey, 1, compressedPublicKey.length);
            BigInteger x = new BigInteger(1, xBytes);

            BigInteger p = ((java.security.spec.ECFieldFp) ecParams.getCurve().getField()).getP();
            BigInteger a = ecParams.getCurve().getA();
            BigInteger b = ecParams.getCurve().getB();
            BigInteger rhs = x.modPow(BigInteger.valueOf(3), p).add(a.multiply(x)).add(b).mod(p);
            BigInteger y = rhs.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p);

            boolean yOdd = (compressedPublicKey[0] & 1) == 1;
            if (y.testBit(0) != yOdd) {
                y = p.subtract(y);
            }

            ECPoint ecPoint = new ECPoint(x, y);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(ecPoint, ecParams);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey pubKey = kf.generatePublic(pubSpec);
            return pubKey.getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the payload from a JWT token.
     *
     * @param token The JWT token.
     * @return The decoded payload as a string.
     * @throws RuntimeException if parsing fails.
     */
    public static String parsePayload(String token) {
        try {
            String[] chunks = token.split("\\.");
            return new String(Base64.decode(chunks[1], Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token", e);
        }
    }

    /**
     * Reads a certificate from the assets folder.
     *
     * @param context The application context.
     * @param fileName The name of the certificate file.
     * @return A list containing the certificate string.
     * @throws Exception if an error occurs while reading the file.
     */
    public static List<String> readCertFromAssets(Context context, String fileName) throws Exception {
        List<String> certs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("BEGIN CERTIFICATE") || line.contains("END CERTIFICATE")) {
                    continue;
                }
                sb.append(line.trim());
            }
        }
        certs.add(sb.toString());
        return certs;
    }

    /**
     * Encodes a byte array into a Base64 URL-safe string.
     *
     * @param data The byte array to encode.
     * @return The Base64 URL-safe encoded string.
     */
    public static String base64UrlEncode(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    /**
     * Generates PKCE (Proof Key for Code Exchange) values.
     *
     * @return A PkceValues object containing the code verifier and code challenge.
     * @throws RuntimeException if the SHA-256 algorithm is not available.
     */
    public static PkceValues generatePkceValues() {
        byte[] verifierBytes = new byte[32];
        new SecureRandom().nextBytes(verifierBytes);
        String codeVerifier = base64UrlEncode(verifierBytes);

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            String codeChallenge = base64UrlEncode(digest);
            return new PkceValues(codeVerifier, codeChallenge);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static class PkceValues {
        public final String codeVerifier;
        public final String codeChallenge;

        /**
         * Constructs a PkceValues object.
         *
         * @param codeVerifier The code verifier string.
         * @param codeChallenge The code challenge string.
         */
        public PkceValues(String codeVerifier, String codeChallenge) {
            this.codeVerifier = codeVerifier;
            this.codeChallenge = codeChallenge;
        }
    }

    /**
     * Generates a JWS (JSON Web Signature) for the given parameters.
     *
     * @param context The application context.
     * @param holderDid The DID of the holder.
     * @param pii Personally Identifiable Information.
     * @param name The name of the holder.
     * @return The generated JWS string, or null if an error occurs.
     */
    public static String generateJws(Context context, String holderDid, String pii, String name) {
        try {
            JSONObject payloadJson = new JSONObject();
            payloadJson.put("iss", holderDid);
            payloadJson.put("sub", pii);
            payloadJson.put("name", name);
            payloadJson.put("iat", System.currentTimeMillis() / 1000);

            String serializedPayload = payloadJson.toString();
            List<String> x5cList = readCertFromAssets(context, "holder.crt");

            JSONObject headerJson = new JSONObject();
            headerJson.put("alg", "ES256");
            headerJson.put("typ", "JWT");

            JSONArray x5cArray = new JSONArray();
            for (String cert : x5cList) {
                x5cArray.put(cert);
            }
            headerJson.put("x5c", x5cArray);

            String encodedHeader = base64UrlEncode(headerJson.toString().getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(serializedPayload.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;
            byte[] signingInputBytes = signingInput.getBytes(StandardCharsets.UTF_8);
            
            Secp256R1Manager secp256R1Manager = new Secp256R1Manager();
            byte[] aosPrivateKey = MultibaseUtils.decode("f98c395f0b9a2b4838a402ca749b0b1b16d3199532e42374fb6225d8e1c1fc746");
            byte[] hashedData = DigestUtils.getDigest(signingInputBytes, DigestEnum.DIGEST_ENUM.SHA_256);
            byte[] signatureBytes = secp256R1Manager.sign(aosPrivateKey, hashedData);
            
            String encodedSignature = base64UrlEncode(signatureBytes);
            return signingInput + "." + encodedSignature;
        } catch (Exception e) {
            return null;
        }
    }
}