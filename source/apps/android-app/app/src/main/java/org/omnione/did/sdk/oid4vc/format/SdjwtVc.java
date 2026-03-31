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

import android.content.Context;

import org.omnione.did.sdjwt.core.oid4vp.OID4VPHandler;
import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.util.SimpleJWTDecoder;
import org.omnione.did.sdk.oid4vc.util.CryptoUtil;
import org.omnione.did.sdk.mdoc.util.LogUtil;

import java.security.PrivateKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SdjwtVc {
    private static final String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";

    /**
     * Checks if the given format is supported for SD-JWT.
     *
     * @param format The format string to check.
     * @return true if the format is supported, false otherwise.
     */
    public static boolean isSupported(String format) {
        return "NationalID".equals(format) || "NationalIDCert".equals(format);
    }

    /**
     * Creates a selectively disclosed VP token based on DCQL.
     *
     * @param context The application context.
     * @param sdJwtVc The SD-JWT Verifiable Credential.
     * @param selectedClaimsKeys The list of claim keys selected for disclosure.
     * @param aud The audience for the VP token.
     * @param nonce The nonce for the VP token.
     * @return The generated VP token string.
     * @throws Exception if an error occurs during token creation.
     */
    public static String createVpToken(Context context, String sdJwtVc, List<String> selectedClaimsKeys, String aud, String nonce) throws Exception {
        PrivateKey holderPrivateKey = CryptoUtil.getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));
        List<String> holderX5cChain = CryptoUtil.readCertFromAssets(context, "holder.crt");

        Set<String> requestedClaims;
        if (selectedClaimsKeys != null && !selectedClaimsKeys.isEmpty()) {
            requestedClaims = new java.util.HashSet<>(selectedClaimsKeys);
            requestedClaims.remove("Issuer");
            requestedClaims.remove("Subject");
            requestedClaims.remove("vct");
            requestedClaims.remove("Format");
        } else {
            requestedClaims = Set.of("family_name", "given_name", "phone_number", "birth_date", "email");
        }
        
        String vpToken = OID4VPHandler.createVPTokenWithDcqlId(
                sdJwtVc,
                requestedClaims,
                "national_id",
                holderPrivateKey,
                holderX5cChain,
                aud,
                nonce
        );
        LogUtil.logLongString("sangjun", "vpToken: " + vpToken);

        return vpToken;
    }

    /**
     * Extracts claims from an SD-JWT Verifiable Credential.
     *
     * @param sdJwtVc The SD-JWT Verifiable Credential.
     * @return A map containing the extracted claims.
     */
    public static Map<String, Object> getClaims(String sdJwtVc) {
        Map<String, Object> claims = new LinkedHashMap<>();
        SDJWT parsedVC = SDJWT.parse(sdJwtVc);

        SimpleJWTDecoder.SimpleJWT credentialJWT = SimpleJWTDecoder.parse(parsedVC.getCredentialJwt());
        if (credentialJWT != null && credentialJWT.getPayload() != null) {
            claims.put("Issuer", credentialJWT.getPayload().get("iss"));
            claims.put("Subject", credentialJWT.getPayload().get("sub"));
            claims.put("vct", credentialJWT.getPayload().get("vct"));
        }

        for (int i = 0; i < parsedVC.getDisclosureCount(); i++) {
            Disclosure disclosure = parsedVC.getDisclosures().get(i);
            claims.put(disclosure.getClaimName(), disclosure.getClaimValue());
        }
        return claims;
    }
}
