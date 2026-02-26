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

package org.omnione.did.oid4vc.formatter.oid4vci.generator.impl;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bouncycastle.util.encoders.Base64;
import org.omnione.did.oid4vc.formatter.exception.FormatterErrorCode;
import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.VcGenerator;
import org.omnione.did.oid4vc.formatter.util.KeyUtil;
import org.omnione.did.sdjwt.core.oid4vci.CompactSigner;
import org.omnione.did.sdjwt.core.oid4vci.OID4VCIssuer;

public class SdJwtGeneratorWithCert implements VcGenerator {
    @Override
    public String generate(Map<String, Object> claims, Map<String, Object> keyInfo) throws FormatterException {
        System.out.println("SdJwtGeneratorWithCert generate");
        try {
            String privateKey = (String) keyInfo.get("privateKey");
            String publicKey = (String) keyInfo.get("publicKey");
            List<String> x5cChainList = getX5cHeader();
            
            // Extract proof_jwk from keyInfo
            Map<String, Object> proofJwk = (Map<String, Object>) keyInfo.get("proof_jwk");
            
            String issuerkeyAlgorithm = (String) keyInfo.getOrDefault("issuerkeyAlgorithm", "Secp256r1");
            String credentialSchemaUrl = (String) keyInfo.getOrDefault("credentialSchemaUrl", "https://credentials.gov.kr/identity_credential");
            CompactSigner compactSigner = (CompactSigner) keyInfo.get("compactSigner");

            PrivateKey privateKeyObject = KeyUtil.getPrivateKeyObject(Base64.decode(privateKey));
            PublicKey publicKeyObject = KeyUtil.getPublicKeyObject(KeyUtil.unCompressPublicKey(Base64.decode(publicKey)));

            OID4VCIssuer issuer = new OID4VCIssuer(compactSigner, "assert", "Secp256r1", "https://opendid-issuer.dev", x5cChainList);

            if (proofJwk != null) {
                System.out.println("Converting JWK from proof to self-signed x5c for holder binding");
                return issuer.issueCredential(credentialSchemaUrl, claims, proofJwk);
            } else {
                List<String> holderX5cChain = getHolderX5cHeader();
                System.out.println("Using default holder.crt for holder binding (cnf)");
                return issuer.issueCredential(credentialSchemaUrl, claims, holderX5cChain);
            }
        } catch (Exception e) {
            throw new FormatterException(FormatterErrorCode.ERR_CODE_VC_GENERATE_FAILED, "Failed to generate SD-JWT credential", e);
        }
    }

    public List<String> getX5cHeader() throws Exception {
        List<String> x5c = new ArrayList<>();
        x5c.add(loadCertBase64("issuer.crt"));
        x5c.add(loadCertBase64("intermediate.crt"));
        System.out.println("x5c issuer.crt : " + x5c.get(0));
        System.out.println("x5c intermediate.crt : " + x5c.get(1));
        return x5c;
    }

    public List<String> getHolderX5cHeader() throws Exception {
        List<String> x5c = new ArrayList<>();
        x5c.add(loadCertBase64("holder.crt"));
        System.out.println("x5c holder.crt : " + x5c.getFirst());
        return x5c;
    }

    private String loadCertBase64(String path) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(path)));
        return pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
    }
}
