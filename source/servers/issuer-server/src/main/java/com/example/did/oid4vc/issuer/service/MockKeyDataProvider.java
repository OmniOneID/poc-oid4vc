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

import lombok.RequiredArgsConstructor;
import org.omnione.did.oid4vc.oid4vci.property.IssuerProperties;
import org.omnione.did.oid4vc.oid4vci.service.KeyDataProvider;
import org.omnione.did.sdjwt.core.oid4vci.CompactSigner;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MockKeyDataProvider implements KeyDataProvider {

    private final IssuerProperties issuerProperties;

    @Override
    public Map<String, Object> getKeyInfo(String userId, String credentialType) {
        // In a real application, fetch from KMS or secure storage
        String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
        String PUBLIC_KEY= "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";

        
        // Add server wallet integration
        WalletManagerInterface walletManager = null;
        try {
            walletManager = WalletManagerFactory.getWalletManager(WalletManagerFactory.WalletManagerType.FILE);
            String walletFileName = issuerProperties.getWalletFileName() != null ? issuerProperties.getWalletFileName() : "forten.wallet";
            String walletPath = Paths.get(issuerProperties.getDataDir(), walletFileName).toString();
            walletManager.connect(walletPath, "123456".toCharArray());

        } catch (WalletException e) {
            throw new RuntimeException(e);
        }

        WalletManagerInterface finalWalletManager = walletManager;
        CompactSigner signer = (keyId, hash) -> {
          try {
            return finalWalletManager.generateCompactSignatureFromHash(keyId, hash);
          } catch (WalletException e) {
            throw new RuntimeException(e);
          }
        };

        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("compactSigner", signer);
//        keyInfo.put("keyId", "assert");

        keyInfo.put("privateKey", PRIVATE_KEY);
        keyInfo.put("publicKey", PUBLIC_KEY);
//        keyInfo.put("issuerDid", "did:omn:issuer");
        keyInfo.put("issuerKid", "did:omn:issuer?versionId=1#assert");
        keyInfo.put("issuerkeyAlgorithm", "Secp256r1");
        keyInfo.put("credentialSchemaUrl", "https://credentials.gov.kr/identity_credential");
//        keyInfo.put("credentialSchemaUrl", "urn:eudi:pid:1");

        return keyInfo;
    }
}
