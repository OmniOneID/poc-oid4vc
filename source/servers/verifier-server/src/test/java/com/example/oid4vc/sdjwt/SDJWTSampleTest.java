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

package com.example.oid4vc.sdjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.omnione.did.oid4vc.dcql.core.DCQLCredentialMatcher;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.oid4vci.core.OID4VCIssuer;
import org.omnione.did.oid4vc.oid4vp.core.OID4VPHandler;
import org.omnione.did.oid4vc.oid4vp.core.SDJWTVerifier;
import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.DisclosureFrame;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.util.SimpleJWTDecoder;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerFactory.WalletManagerType;
import org.omnione.did.wallet.key.WalletManagerInterface;

/**
 * Complete SD-JWT VC sample generation and VP token processing test (DCQL functionality included)
 */
public class SDJWTSampleTest {

  // Register BouncyCastle Provider during class initialization
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Test
  @DisplayName("Nation ID SD-JWT VC/VP flow test (DCQL included)")
  void SDJWTSampleTest() throws Exception {
    // =========== STEP 1: Key Generation ===========
    System.out.println("Generate key pair");
    // Use pure Java KeyPairGenerator
    /*
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1"); // P-256 curve
    keyGen.initialize(ecSpec);

    KeyPair issuerKeyPair = keyGen.generateKeyPair();
    KeyPair holderKeyPair = keyGen.generateKeyPair();

    PrivateKey issuerPrivateKey = issuerKeyPair.getPrivate();
    PublicKey issuerPublicKey = issuerKeyPair.getPublic();
    PrivateKey holderPrivateKey = holderKeyPair.getPrivate();
    PublicKey holderPublicKey = holderKeyPair.getPublic();
    */

    String PUBLIC_KEY_WALLET = "AkEFAYZR7Ys4XIx4ILHFb5IaUlgpnpyOhpfS+daMuZH+";

    String PUBLIC_KEY = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";
    String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";

    //PrivateKey issuerPrivateKey = getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));
    PublicKey issuerPublicKey = getPublicKeyObject(unCompressPublicKey(Base64.getDecoder().decode(PUBLIC_KEY_WALLET)));
    PrivateKey holderPrivateKey = getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));
    PublicKey holderPublicKey = getPublicKeyObject(unCompressPublicKey(Base64.getDecoder().decode(PUBLIC_KEY)));

    System.out.println("Issuer key generation completed");
    System.out.println("Holder key generation completed");

    // =========== STEP 2: Issue SD-JWT VC ===========
    System.out.println("Issue SD-JWT VC");

    // Add server wallet integration
    WalletManagerInterface walletManager = WalletManagerFactory.getWalletManager(WalletManagerType.FILE);
    walletManager.connect("/Users/shlee1223/20250805/tec_op_server/op.wallet", "123456".toCharArray());

    // Create issuer
    OID4VCIssuer issuer = new OID4VCIssuer(
        holderPrivateKey,
        //walletManager,
        //"assert",
        "did:omn:issuer"
    );

    // Identity information (all fields can be selectively disclosed)
    Map<String, Object> identityInfo = Map.of(
        "given_name", "Raon",
        "family_name", "Kim",
        "birth_date", "1990-01-01",
        "gender", "male",
        "nationality", "KR",
        "id_number", "900101-1234567",
        "address", Map.of(
            "country", "대한민국",
            "region", "서울특별시",
            "locality", "강남구",
            "street_address", "테헤란로 123"
        ),
        "phone_number", "+82-10-1234-5678",
        "email", "raonkim@raoncorp.com"
    );

    // ===== Create DisclosureFrame for Structured SD-JWT =====
    // Method 1: Using DisclosureFrame API (Programmatic approach)
    System.out.println("Create DisclosureFrame using API");
    DisclosureFrame disclosureFrame = new DisclosureFrame();

    // Top-level selective disclosure fields
    // These fields will have their digests in the top-level _sd array
    disclosureFrame.addSdFields(Arrays.asList(
        "given_name",
        "family_name",
        "birth_date",
        "gender",
        "nationality",
        "id_number",
        "phone_number"
    ));

    // Nested frame for address object
    // Only "locality" and "street_address" will be selectively disclosed
    // "country" and "region" will be included as plain values
    DisclosureFrame addressFrame = new DisclosureFrame();
    addressFrame.addSdFields(Arrays.asList(
        "locality",
        "street_address",
        "region"
    ));
    disclosureFrame.addNestedFrame("address", addressFrame);

    System.out.println("DisclosureFrame structure:");
    System.out.println("  Top-level SD fields: given_name, family_name, birth_date, id_number, phone_number, email");
    System.out.println("  Nested frame for 'address': locality, street_address will be disclosed");
    System.out.println("  Non-disclosed nested fields: country, region (included as plain values)");
    System.out.println();

    /* ===== Alternative Method: Using JSON string =====
    System.out.println("Create DisclosureFrame from JSON");
    String disclosureFrameJson = """
    {
      "_sd": [
        "given_name",
        "family_name",
        "birth_date",
        "id_number",
        "phone_number",
        "email"
      ],
      "address": {
        "_sd": [
          "locality",
          "street_address"
        ]
      }
    }
    """;
    DisclosureFrame disclosureFrame = DisclosureFrame.fromJson(disclosureFrameJson);
    */

    /* ===== Alternative Method: Using Map =====
    System.out.println("Create DisclosureFrame from Map");
    Map<String, Object> disclosureFrameMap = new HashMap<>();
    disclosureFrameMap.put("_sd", Arrays.asList(
        "given_name",
        "family_name",
        "birth_date",
        "id_number",
        "phone_number",
        "email"
    ));

    Map<String, Object> addressFrameMap = new HashMap<>();
    addressFrameMap.put("_sd", Arrays.asList(
        "locality",
        "street_address"
    ));
    disclosureFrameMap.put("address", addressFrameMap);

    DisclosureFrame disclosureFrame = DisclosureFrame.fromMap(disclosureFrameMap);
    */

    // Issue SD-JWT VC with DisclosureFrame
    String identityVC = issuer.issueCredential(
        "https://credentials.gov.kr/identity_credential",
        identityInfo,
        disclosureFrame,
        holderPublicKey
    );

    System.out.println("Issued SD-JWT VC:");
    System.out.println("   " + identityVC);
    System.out.println();

    // =========== STEP 3: Analyze SD-JWT structure ===========
    System.out.println("Analyze SD-JWT structure");

    SDJWT parsedVC = SDJWT.parse(identityVC);

    // Analyze JWT header and payload (using SimpleJWTDecoder)
    SimpleJWTDecoder.SimpleJWT credentialJWT = SimpleJWTDecoder.parse(parsedVC.getCredentialJwt());

    System.out.println("JWT Header: " + credentialJWT.getHeader());
    System.out.println("JWT Payload: " + credentialJWT.getPayload());
    System.out.println("Total number of Disclosures: " + parsedVC.getDisclosureCount());

    // Detailed information for each Disclosure
    System.out.println("Disclosure detailed information:");
    for (int i = 0; i < parsedVC.getDisclosures().size(); i++) {
      Disclosure disclosure = parsedVC.getDisclosures().get(i);
      System.out.println("     " + (i+1) + ". " + disclosure.getClaimName() + ": " + disclosure.getClaimValue());
      System.out.println("        Salt: " + disclosure.getSalt());
      System.out.println("        Digest: " + disclosure.digest());
      System.out.println("        Raw: " + disclosure.getDisclosure());
    }
    System.out.println();

    // =========== Newly added: Create and validate DCQL Query ===========
    System.out.println("=========== Create and validate DCQL Query ===========");

    // Create DCQL Query (including Meta information)
    Map<String, Object> dcqlMeta = new HashMap<>();
    dcqlMeta.put("vct_values", Arrays.asList("https://credentials.gov.kr/identity_credential"));
    dcqlMeta.put("issuer_did", "did:omn:issuer");

    // DCQL Query for adult authentication (18 years or older, Korean nationality)
    DCQLQuery.ClaimQuery ageClaimQuery = DCQLQuery.ClaimQuery.builder()
        .id("age_verification")
        .path(Arrays.asList("birth_date"))
        .purpose("Adult verification for age-restricted services")
        .build();

    DCQLQuery.ClaimQuery nationalityClaimQuery = DCQLQuery.ClaimQuery.builder()
        .id("nationality_verification")
        .path(Arrays.asList("nationality"))
        .values(Arrays.asList("US", "KR"))
        .purpose("Korean nationality verification")
        .build();

    DCQLQuery.ClaimQuery nameClaimQuery = DCQLQuery.ClaimQuery.builder()
        .id("name_verification")
        .path(Arrays.asList("given_name"))
        .purpose("Identity verification")
        .build();

    DCQLQuery.ClaimQuery familyNameClaimQuery = DCQLQuery.ClaimQuery.builder()
        .id("family_name_verification")
        .path(Arrays.asList("family_name"))
        .purpose("Family name verification")
        .build();

    DCQLQuery.ClaimQuery addressClaimQuery = DCQLQuery.ClaimQuery.builder()
        .id("address_verification")
        .path(Arrays.asList("address", "locality"))
        .purpose("Address verification")
        .build();

    // Create Credential Query
    DCQLQuery.CredentialQuery credentialQuery = DCQLQuery.CredentialQuery.builder()
        .id("identity_credential")
        .format("vc+sd-jwt")
        .meta(dcqlMeta)
        .claims(Arrays.asList(ageClaimQuery, nationalityClaimQuery, nameClaimQuery, familyNameClaimQuery))
        .purpose("Identity and nationality verification for Korean adults")
        .requireCryptographicHolderBinding(true)
        .build();

    // Complete DCQL Query
    DCQLQuery dcqlQuery = DCQLQuery.builder()
        .credentials(Arrays.asList(credentialQuery))
        .build();

    System.out.println("DCQL Query creation completed");
    System.out.println("   - Credential ID: " + credentialQuery.getId());
    System.out.println("   - Required Claims: " + credentialQuery.getClaims().size() + "개");
    System.out.println("   - Meta Fields: " + dcqlMeta.size() + "개");

    // Convert DCQL Query to JSON
    String dcqlJsonString = convertDCQLQueryToJsonString(dcqlQuery);
    System.out.println("dcqlJsonString : " + dcqlJsonString);

    // Additional code can be added from here
    // =========== STEP 4: Create selective disclosure VP token based on DCQL ===========

    // Deserialize JSON string to DCQLQuery object
    ObjectMapper objectMapper = new ObjectMapper();
    DCQLQuery dcqlQueryFromStr = objectMapper.readValue(dcqlJsonString, DCQLQuery.class);
    System.out.println("Create selective disclosure VP token based on DCQL");

    // Validate DCQL Query
    DCQLQueryValidator.ValidationResult validationResult = DCQLQueryValidator.validate(dcqlQueryFromStr);
    System.out.println("DCQL Query validation result: " + validationResult.isValid());

    // Test SD-JWT and DCQL Query matching
    boolean isMatching = DCQLCredentialMatcher.matchesMetadata(parsedVC, dcqlQueryFromStr.getCredentials().get(0).getMeta()); // Assuming 1 credential
    System.out.println("SD-JWT and DCQL Meta matching result: " + (isMatching ? "Matching successful" : "Matching failed"));

    if(!isMatching) {
      return;
    }

    // Test Claims extraction
    Set<String> dcqlRequiredClaims = DCQLCredentialMatcher.extractMatchingClaimNames(dcqlQueryFromStr, parsedVC);
    System.out.println("Extracted Claim names: " + dcqlRequiredClaims);

    String dcqlVpToken = OID4VPHandler.createVPTokenWithDcqlId(
        identityVC,
        dcqlRequiredClaims,
            "test-dcql",
        holderPrivateKey,
        "did:omn:issuer",
        "dcql-nonce-456"
    );

    System.out.println("DCQL-based VP Token:");
    System.out.println("   " + dcqlVpToken);
    System.out.println();

    dcqlVpToken = OID4VPHandler.createVPToken(
        identityVC,
        dcqlRequiredClaims,
        holderPrivateKey,
        "did:omn:issuer",
        "dcql-nonce-456"
    );

    // =========== STEP 5: Verify VP token (Check DCQL conditions) ===========
    System.out.println("Verify VP token (Check DCQL conditions)");

    SDJWTVerifier verifier = new SDJWTVerifier(holderPublicKey, holderPublicKey);

    // DCQL-based VP verification
    SDJWTVerifier.SDJWTClaimsSet dcqlClaims = verifier.verify(
        dcqlVpToken,
        "did:omn:issuer",
        "dcql-nonce-456"
    );

    System.out.println("DCQL-based VP verification result:");
    dcqlClaims.getClaims().forEach((key, value) -> {
      if (!key.startsWith("_") && !key.equals("iss") && !key.equals("iat") &&
          !key.equals("exp") && !key.equals("vct") && !key.equals("cnf")) {
        System.out.println("     " + key + ": " + value);
      }
    });
  }

  /**
   * Convert DCQL Query to JSON string
   */
  private String convertDCQLQueryToJsonString(DCQLQuery dcqlQuery) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dcqlQuery);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      System.out.println("Jackson conversion failed, using default toString: " + e.getMessage());
      return dcqlQuery.toString();
    }
  }

  public static PrivateKey getPrivateKeyObject(byte[] privateKeyBytes) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
      return keyFactory.generatePrivate(privateKeySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
      return keyFactory.generatePublic(publicKeySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] unCompressPublicKey(byte[] compressedPublicKey) {
    byte[] uncompressPublicKey = null;
    ECNamedCurveParameterSpec ecParams = ECNamedCurveTable.getParameterSpec("Secp256r1");
    ECPoint uncompressedPoint = ecParams.getCurve().decodePoint(compressedPublicKey);
    ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(uncompressedPoint, ecParams);

    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      uncompressPublicKey = keyFactory.generatePublic(pubKeySpec).getEncoded();
      return uncompressPublicKey;
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}