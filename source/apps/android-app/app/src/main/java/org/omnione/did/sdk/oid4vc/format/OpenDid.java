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

package org.omnione.did.sdk.oid4vc.format;

import com.google.gson.Gson;

import org.omnione.did.sdk.oid4vc.data.dto.tec.Header;
import org.omnione.did.sdk.oid4vc.data.dto.tec.Payload;
import org.omnione.did.sdk.oid4vc.data.dto.tec.VerifiableCredential;
import org.omnione.did.sdk.oid4vc.util.LogUtil;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OpenDid {
    private static final Gson gson = new Gson();

    /**
     * Checks if the given format is supported for OpenDID.
     *
     * @param format The format string to check.
     * @return true if the format is supported, false otherwise.
     */
    public static boolean isSupported(String format) {
        return "TEC".equals(format) || "UCR".equals(format);
    }

    /**
     * Creates a VP token for OpenDID.
     *
     * @param nonce The nonce for the VP token.
     * @param aud The audience for the VP token.
     * @param vcCredential The Verifiable Credential data in Base64 format.
     * @return The generated VP token string.
     */
    public static String createVpToken(String nonce, String aud, String vcCredential) {
        Header header = new Header("ES256", "JWT", "did:example:holder#key-1");
        long iat = Instant.now().getEpochSecond();
        long exp = iat + 2592000L;
        String jti = UUID.randomUUID().toString();

        Payload payload = getVpToken(iat, exp, jti, nonce, aud, vcCredential);

        String headerJson = gson.toJson(header);
        String payloadJson = gson.toJson(payload);

        String headerBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String vpToken = headerBase64Url + "." + payloadBase64Url;
        LogUtil.logLongString("sangjun", "vpToken: " + vpToken);
        return vpToken;
    }

    /**
     * Constructs the payload for the VP token.
     *
     * @param iat Issued at time.
     * @param exp Expiration time.
     * @param jti JWT ID.
     * @param nonce Nonce value.
     * @param aud Audience value.
     * @param vcCredential Verifiable Credential data in Base64 format.
     * @return The constructed Payload object.
     */
    private static Payload getVpToken(long iat, long exp, String jti, String nonce, String aud, String vcCredential) {
        Payload container = new Payload();

        container.setIssuer("did:example:holder");
        container.setAudience(aud);
        container.setNonce(nonce);
        container.setIssuedAt(iat);
        container.setExpiration(exp);

        Payload.VerifiablePresentation vp = new Payload.VerifiablePresentation();
        vp.setContext(Collections.singletonList("https://www.w3.org/ns/credentials/v2"));
        vp.setType(Collections.singletonList("VerifiablePresentation"));

        Payload.Proof vpProof = new Payload.Proof();
        vpProof.setType("DataIntegrityProof");
        vpProof.setCryptosuite("ecdsa-rdfc-2019");
        vpProof.setCreated(Instant.now().toString());
        vpProof.setProofPurpose("authentication");
        vpProof.setVerificationMethod("did:example:holder#key-1");
        vpProof.setChallenge(nonce);
        vpProof.setDomain(aud);
        vpProof.setProofValue("zQeVbY4oQowNiQoClz9Qg8X6PpuKy4tP9t8rKHHB3P4...");
        vp.setVpProof(vpProof);

        byte[] decodedBytes = android.util.Base64.decode(vcCredential, android.util.Base64.DEFAULT);
        String vc = new String(decodedBytes, StandardCharsets.UTF_8);
        VerifiableCredential vcItem = new Gson().fromJson(vc, VerifiableCredential.class);

        if (vcItem != null) {
            vp.setVerifiableCredential(Collections.singletonList(vcItem));
        }

        container.setVp(vp);
        return container;
    }

    /**
     * Extracts claims from an OpenDID Verifiable Credential.
     *
     * @param vcCredential The Verifiable Credential data in Base64 format.
     * @return A list of claims extracted from the credential.
     */
    public static List<VerifiableCredential.Claim> getClaims(String vcCredential) {
        byte[] decodedBytes = android.util.Base64.decode(vcCredential, android.util.Base64.DEFAULT);
        String jsonArrayString = new String(decodedBytes, StandardCharsets.UTF_8);
        VerifiableCredential vc = gson.fromJson(jsonArrayString, VerifiableCredential.class);
        if (vc != null && vc.getCredentialSubject() != null) {
            return vc.getCredentialSubject().getClaims();
        }
        return Collections.emptyList();
    }
}
