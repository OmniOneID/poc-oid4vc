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

package com.example.did.oid4vc.issuer.service;

import com.example.did.oid4vc.issuer.property.WalletProperty;
import lombok.RequiredArgsConstructor;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.CompactSigner;
import org.omnione.did.oid4vc.formatter.oid4vci.generator.dto.IssuerKeyInfo;
import org.omnione.did.oid4vc.formatter.util.SignatureUtil;
import org.omnione.did.oid4vc.oid4vci.property.IssuerProperties;
import org.omnione.did.oid4vc.oid4vci.service.KeyDataProvider;
import org.omnione.did.oid4vc.oid4vci.util.VerifyUtil;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockKeyDataProvider implements KeyDataProvider {

    private final IssuerProperties issuerProperties;
    private final WalletProperty walletProperty;

    @Override
    public IssuerKeyInfo getKeyInfo(String userId, String credentialType) {
        // test용 키쌍
        String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
        String PUBLIC_KEY = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";

        // Add server wallet integration
        WalletManagerInterface walletManager = null;
        try {
            walletManager = WalletManagerFactory.getWalletManager(WalletManagerFactory.WalletManagerType.FILE);
            // todo : wallet path 수정해야함
            String walletFileName = issuerProperties.getWalletFileName();
            String walletPath = Paths.get(issuerProperties.getDataDir(), walletFileName).toString();
            walletManager.connect(walletPath, "123456".toCharArray());
//            walletManager.connect(walletProperty.getFilePath(), walletProperty.getPassword().toCharArray());

        } catch (WalletException e) {
            throw new RuntimeException(e);
        }

        WalletManagerInterface finalWalletManager = walletManager;
        CompactSigner signer = (keyId, hash) -> {
            try {
//                return finalWalletManager.generateCompactSignatureFromHash(keyId, hash);
                byte[] signature = finalWalletManager.generateCompactSignatureFromHash(keyId, hash);
                System.out.println("signature length : " + signature.length);
                //65 -> 64 (open did wallet의 개인키 필수)
                byte[] convertSignature = SignatureUtil.convertSignature(signature);
                System.out.println("convert signature length : " + convertSignature.length);
                return convertSignature;
            } catch (WalletException e) {
                throw new RuntimeException(e);
            }
        };

        IssuerKeyInfo keyInfo = new IssuerKeyInfo();
        keyInfo.setCompactSigner(signer);
        keyInfo.setIssuerKid("did:omn:issuer?versionId=1#assert");
        keyInfo.setIssuerKeyAlgorithm("Secp256r1");
        if (credentialType.equals("mDL"))
            keyInfo.setCredentialSchemaUrl("org.iso.18013.5.1.mDL");
        else if (credentialType.equals("mDocPID"))
            keyInfo.setCredentialSchemaUrl("eu.europa.ec.eudi.pid.1");
        else
            keyInfo.setCredentialSchemaUrl("urn:eudi:pid:1"); // "https://credentials.gov.kr/identity_credential"
        keyInfo.setPublicKey(PUBLIC_KEY);
        keyInfo.setPrivateKey(PRIVATE_KEY);
        keyInfo.setX5cList(getX5cHeader());
        keyInfo.setHolderX5c(getHolderX5cHeader());

        return keyInfo;
    }

    private List<String> getX5cHeader() {
        List<String> x5c = new ArrayList<>();
        String issuerCert = issuerProperties.getIssuerCertFileName();
        String intermediateCert = issuerProperties.getIntermediateCertFileName();

        x5c.add(loadCertBase64(issuerCert));
        x5c.add(loadCertBase64(intermediateCert));
        return x5c;
    }

    private List<String> getHolderX5cHeader() {
        List<String> x5c = new ArrayList<>();
        String holderCert = issuerProperties.getHolderCertFileName();
        x5c.add(loadCertBase64(holderCert));
        return x5c;
    }

    private String loadCertBase64(String fileName) {
        String pem = null;
        try {
            String path = Paths.get(issuerProperties.getDataDir(), fileName).toString();
            pem = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
    }
}
