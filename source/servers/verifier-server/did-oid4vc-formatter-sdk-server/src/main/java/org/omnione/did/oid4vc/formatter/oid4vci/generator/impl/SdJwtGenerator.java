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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import org.bouncycastle.util.encoders.Base64;
import org.omnione.did.oid4vc.formatter.exception.FormatterErrorCode;
import org.omnione.did.oid4vc.formatter.exception.FormatterException;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.VcGenerator;
import org.omnione.did.oid4vc.formatter.util.KeyUtil;
import org.omnione.did.sdjwt.core.oid4vci.CompactSigner;
import org.omnione.did.sdjwt.core.oid4vci.OID4VCIssuer;

public class SdJwtGenerator implements VcGenerator {
    @Override
    public String generate(Map<String, Object> claims, Map<String, Object> keyInfo) throws FormatterException {
        System.out.println("SdJwtGenerator generate");
        try {
            String privateKey = (String) keyInfo.get("privateKey");
            String publicKey = (String) keyInfo.get("publicKey");
            String issuerKid = (String) keyInfo.getOrDefault("issuerKid", "did:omn:issuer?versionId=1#assert");
            String issuerkeyAlgorithm = (String) keyInfo.getOrDefault("issuerkeyAlgorithm", "Secp256r1");
            String credentialSchemaUrl = (String) keyInfo.getOrDefault("credentialSchemaUrl", "https://credentials.gov.kr/identity_credential");
            CompactSigner compactSigner = (CompactSigner) keyInfo.get("compactSigner");
//            String keyId = (String) keyInfo.get("keyId");

            PrivateKey privateKeyObject = KeyUtil.getPrivateKeyObject(Base64.decode(privateKey));
            PublicKey publicKeyObject = KeyUtil.getPublicKeyObject(KeyUtil.unCompressPublicKey(Base64.decode(publicKey)));

            OID4VCIssuer issuer = new OID4VCIssuer(compactSigner, issuerKid, issuerkeyAlgorithm);
            return issuer.issueCredential(credentialSchemaUrl, claims, publicKeyObject);
        } catch (Exception e) {
            throw new FormatterException(FormatterErrorCode.ERR_CODE_VC_GENERATE_FAILED, "Failed to generate SD-JWT credential", e);
        }
    }
}
