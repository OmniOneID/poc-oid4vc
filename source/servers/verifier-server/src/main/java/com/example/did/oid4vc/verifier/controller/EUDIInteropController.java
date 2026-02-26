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

package com.example.did.oid4vc.verifier.controller;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.util.jwk.JwkUtils;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint invoked during JAR-based Authorization Request for EUDI Wallet interoperability (temporary for testing).
 */
@Slf4j
@RestController
public class EUDIInteropController {

  /**
   * Non-standard API called by EUDI Wallet to retrieve preregistered client_id public keys (temporary controller for testing)
   */
  @GetMapping("/wallet/public-keys.json")
  public ResponseEntity<Map<String, Object>> getWalletPublicKeys() {
    log.info("=== [/wallet/public-keys.json] Wallet public keys request ===");

    // Verifier DID Doc, Application Layer
    String verificationMethodWithoutKeyId = "did:omn:verifier" + "?versionId=" + "1";

    WalletManagerInterface walletManager;
    try {
      walletManager = WalletManagerFactory.getWalletManager(WalletManagerFactory.WalletManagerType.FILE);
      ClassPathResource walletResource = new ClassPathResource("test.wallet");
      File tempFile = Files.createTempFile("wallet-", ".wallet").toFile();
      tempFile.deleteOnExit();
      Files.copy(walletResource.getInputStream(), tempFile.toPath(), REPLACE_EXISTING);
      walletManager.connect(tempFile.getAbsolutePath(), "123456".toCharArray());
    } catch (WalletException | IOException e) {
      log.error("[wallet/public-keys] Failed to connect wallet", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }

    try {
      List<String> keyIds = walletManager.getKeyIdList();
      List<Map<String, Object>> keys = new ArrayList<>();

      for (String keyId : keyIds) {
        String publicKeyMultibase = walletManager.getPublicKey(keyId);

        try {
          // Standard JWKS format: place JWK objects directly in the keys array
          Map<String, Object> jwk = JwkUtils.buildJwkFromMultibase(publicKeyMultibase, verificationMethodWithoutKeyId + "#" + keyId);
          keys.add(jwk);
        } catch (Exception e) {
          log.warn("[wallet/public-keys] Failed to build JWK for keyId: {}", keyId, e);
        }
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("keys", keys);

      log.info("[wallet/public-keys] Returning {} keys", keys.size());
      return ResponseEntity.ok(result);

    } catch (WalletException e) {
      log.error("[wallet/public-keys] Failed to get keys from wallet", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    } finally {
      walletManager.disConnect();
    }
  }
}
