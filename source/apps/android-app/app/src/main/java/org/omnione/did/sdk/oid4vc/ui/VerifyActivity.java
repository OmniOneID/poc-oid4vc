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

package org.omnione.did.sdk.oid4vc.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;


import org.omnione.did.oid4vc.dcql.core.DCQLCredentialMatcher;
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import org.omnione.did.oid4vc.oid4vp.core.OID4VPHandler;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.network.ApiService;
import org.omnione.did.sdk.oid4vc.util.LogUtil;
import org.omnione.did.sdk.oid4vc.data.dto.tec.VerifiableCredential;
import org.omnione.did.sdk.oid4vc.data.dto.tec.Header;
import org.omnione.did.sdk.oid4vc.data.dto.tec.Payload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.omnione.did.sdk.oid4vc.R;

public class VerifyActivity extends AppCompatActivity {

    private TextView infoTextView;
    private Gson gson = new Gson();
    private String authorizationRequestState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        infoTextView = findViewById(R.id.info);

        Intent intent = getIntent();
        handleIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();

        if (uri != null) {
            String requestUri = uri.getQueryParameter("request_uri");
            Log.d("sangjun", "authorization request_uri from deeplink : " + requestUri);

            // TODO: Extract other parameters such as client_id here if necessary
            // String clientId = uri.getQueryParameter("client_id");

            if (requestUri != null) {
                String displayText = "OID4VP Request URI: " + requestUri;
                infoTextView.setText(displayText);
                executeNetworkRequest(requestUri);
            } else {
                infoTextView.setText("Error: request_uri not found.");
            }
        } else {
            infoTextView.setText("Error: Deep link data not found.");
        }
    }

    private void executeNetworkRequest(String requestUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        Gson prettyPrintGson = new GsonBuilder().setPrettyPrinting().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(prettyPrintGson))
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.getRequest(requestUrl);

        Log.d("sangjun", "Method: GET Authorization Request");
        Log.d("sangjun", "URL: " + requestUrl);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String responseDataString;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        responseDataString = response.body().string();
                        Log.d("sangjun", "Authorization Request Response Data:\n" + responseDataString);

                        String responseUri = extractJsonField(responseDataString, "response_uri");
                        if (responseUri == null) {
                            Log.e("sangjun", "Could not find 'response_uri' in the response.");
                            infoTextView.setText("Error: Response URI not found.");
                            return;
                        }
                        Log.d("sangjun", "Extracted response_uri: " + responseUri);

                        authorizationRequestState = extractJsonField(responseDataString, "state");
                        if (authorizationRequestState == null) {
                            Log.e("sangjun", "Could not find 'state' in the response.");
                            infoTextView.setText("Error: state value not found.");
                            return;
                        }
                        Log.d("sangjun", "Extracted Authorization Request state: " + authorizationRequestState);

                        String nonce = extractJsonField(responseDataString, "nonce");
                        if (nonce == null) {
                            Log.e("sangjun", "Could not find 'nonce' in the response.");
                            infoTextView.setText("Error: nonce value not found.");
                            return;
                        }
                        Log.d("sangjun", "Extracted nonce: " + nonce);

                        String aud = extractJsonField(responseDataString, "client_id");
                        if (aud == null) {
                            Log.e("sangjun", "Could not find 'client_id' (aud) in the response.");
                            infoTextView.setText("Error: client_id(aud) value not found.");
                            return;
                        }
                        Log.d("sangjun", "Extracted client_id (aud): " + aud);

                                                // todo : dcql parsing -> format needs to be extracted... hardcoded for now

                                                String vpToken = "";

                                                WalletData walletData = loadVcFileAsVcItem()[0];

                                                if(walletData.getFormat().equals("TEC") || walletData.getFormat().equals("UCR")) {

                                                    // Changed to pass nonce and aud values when creating VP Token

                                                    vpToken = createUnsignedVpToken(nonce, aud); // For TEC

                                                }

                                                else if(walletData.getFormat().equals("NationalID") || walletData.getFormat().equals("mDL")) {

                        

                                                    String dcqlQuery = extractJsonField(responseDataString, "dcql_query");

                                                    vpToken = createVpTokenSdJwt(dcqlQuery);

                                                }

                                                Log.d("sangjun", "Start printing createUnsignedVpToken:");

                                                LogUtil.logLongString("VerifyActivity", vpToken);

                                                Log.d("sangjun", "End printing createUnsignedVpToken.");

                        

                                                // POST request VP Token to response_uri

                                                // Use the state value extracted from the Authorization Request

                                                // Extract response_mode

                                                String responseMode = extractJsonField(responseDataString, "response_mode");

                                                if (responseMode == null) {

                                                    Log.e("sangjun", "Could not find 'responseMode' in the response.");

                                                    infoTextView.setText("Error: responseMode value not found.");

                                                    return;

                                                }

                                                postVpTokenToVerifier(responseUri, vpToken, authorizationRequestState, responseMode);

                        

                                            } catch (IOException | JsonSyntaxException e) {

                                                Log.e("sangjun", "Error reading response body or parsing JSON", e);

                                                infoTextView.setText("Error: A problem occurred while processing the response data.");

                                            }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e("sangjun", "Failed to read error body", e);
                    }
                    Log.e("sangjun", "Authorization Request GET failed (Code: " + response.code() + ", Body: " + errorBody + ")");
                    infoTextView.setText("Error: Authorization Request failed (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("sangjun", "Authorization Request network request failed", t);
                Toast.makeText(VerifyActivity.this, "Network error occurred", Toast.LENGTH_SHORT).show();
                infoTextView.setText("Error: Please check your network connection.");
            }
        });
    }

    private String extractJsonField(String jsonResponse, String fieldName) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
            if (jsonObject.has(fieldName) && !jsonObject.get(fieldName).isJsonNull()) {
                return jsonObject.get(fieldName).getAsString();
            }
                } catch (JsonSyntaxException e) {
                    Log.e("sangjun", "JSON parsing error: " + e.getMessage());
                }
                return null;
            }
        
            private void postVpTokenToVerifier(String responseUri, String vpToken, String state, String responseMode) {
                infoTextView.setText("Submitting VP Token to Verifier...");
        
                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build();
        
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://localhost/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();
        
                ApiService apiService = retrofit.create(ApiService.class);
        
                apiService.postVpToken(responseUri, vpToken, state).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d("sangjun", "VP Token submission successful!");
                            try {
                                infoTextView.setText("VC submission complete! : " + response.body().string());
                                Log.d("sangjun", response.body().string());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Toast.makeText(VerifyActivity.this, "VC submission successful!", Toast.LENGTH_LONG).show();
        
        //                    finish();
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e("sangjun", "Failed to read error body", e);
                            }
                            Log.e("sangjun", "VP Token submission failed (Code: " + response.code() + ", Body: " + errorBody + ")");
                            infoTextView.setText("Error: VP Token submission failed (Code: " + response.code() + ")");
                            Toast.makeText(VerifyActivity.this, "VC submission failed!", Toast.LENGTH_LONG).show();
                        }
                    }
        
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("sangjun", "VP Token submission network request failed", t);
                        infoTextView.setText("Error: VP Token submission network error.");
                        Toast.makeText(VerifyActivity.this, "VP Token submission network error!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        
            //sd-jwt test
            public String createVpTokenSdJwt(String dcqlQuery) throws JsonProcessingException {
        
                //todo : must use real key
                String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
                String PUBLIC_KEY= "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";
        
                PrivateKey issuerPrivateKey = getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));
                PublicKey issuerPublicKey = getPublicKeyObject(unCompressPublicKey(Base64.getDecoder().decode(PUBLIC_KEY)));
                PrivateKey holderPrivateKey = getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));
                PublicKey holderPublicKey = getPublicKeyObject(unCompressPublicKey(Base64.getDecoder().decode(PUBLIC_KEY)));
        
                WalletData walletData = loadVcFileAsVcItem()[0];
                String sdJwtVc = (String) walletData.getCredential();
        
                //todo: parse from dcql
        //        Set<String> onlyRequiredClaims = new HashSet<>(
        //                Arrays.asList("given_name", "family_name", "birth_date")
        //        );
                
                // extract claim from dcql
        
                // DCQL Query JSON conversion
                Log.d("sangjun", "dcqlJsonString : " + dcqlQuery);
        
                // Can be added from here
                // =========== STEP 4: Create selectively disclosed VP token based on DCQL ===========
        
                // Deserialize JSON string to DCQLQuery object
                ObjectMapper objectMapper = new ObjectMapper();
                DCQLQuery dcqlQueryFromStr = objectMapper.readValue(dcqlQuery, DCQLQuery.class);
                Log.d("sangjun", "Create selectively disclosed VP token based on DCQL");
        
                // DCQL Query validation
                DCQLQueryValidator.ValidationResult validationResult = DCQLQueryValidator.validate(dcqlQueryFromStr);
                Log.d("sangjun", "DCQL Query validation result: " + validationResult.isValid());
        
                // SD-JWT and DCQL Query matching test
                SDJWT parsedVC = SDJWT.parse(sdJwtVc);
                boolean isMatching = DCQLCredentialMatcher.matchesMetadata(parsedVC, dcqlQueryFromStr.getCredentials().get(0).getMeta()); // Assuming there is one
                Log.d("sangjun", "SD-JWT and DCQL Meta matching result: " + (isMatching ? "Matching successful" : "Matching failed"));
        
                if(!isMatching) {
                    Log.d("sangjun", "No matching VC");
                }
        
                // Claims extraction test
                Set<String> dcqlRequiredClaims = DCQLCredentialMatcher.extractMatchingClaimNames(dcqlQueryFromStr, parsedVC);
                Log.d("sangjun", "Extracted Claim names: " + dcqlRequiredClaims);
                
        
        //        String vpToken = sdJwtVc + "eyJ0eXAiOiJrYitqd3QiLCJhbGciOiJFUzI1NiJ9.eyJzZF9oYXNoIjoiRUhyYlRuLUQzTDcxRUpnTzFCVFV5RGo0WUxUcUR2Um5aZUVlUXEyOTN6RSIsImF1ZCI6ImRpZDpvbW46aXNzdWVyIiwibm9uY2UiOiJ0ZXN0LW5vbmNlLTEyMyIsImlhdCI6MTc1Nzk5MTAxMH0.leHOsTNbJfQkVOMyToXJH17Qh-OsJcnrwkAP4LPSzn0gUgfaJZcrCTZqan2mjWT0ufpv1gnP7_w4P5VKvawtEw";
        //        String vpToken = OID4VPHandler.createVPToken(
        //                sdJwtVc,
        //                dcqlRequiredClaims,
        //                holderPrivateKey,
        //                "did:omn:issuer",
        //                "test-nonce-123"
        //        );
        
                String vpToken = OID4VPHandler.createVPTokenWithDcqlId(
                        sdJwtVc,
                        dcqlRequiredClaims,
                        dcqlQueryFromStr.getCredentials().get(0).getId(), // Extract id from DCQLQuery object
                        holderPrivateKey,
                        "did:omn:issuer",
                        "dcql-nonce-456"
                );
                Log.d("sangjun", "VP Token (only name and date of birth disclosed):");
                Log.d("sangjun", "   " + vpToken);
        
                return vpToken;
            }
        
            // jwt vp token creation and signing (hardcoded)
            // Changed to receive nonce and aud as parameters
            public String createUnsignedVpToken(String nonce, String aud) {
                Header header = new Header("ES256", "JWT", "did:example:holder#key-1");
                long iat = Instant.now().getEpochSecond();
                long exp = iat + 2592000L;
                String jti = UUID.randomUUID().toString();
        
                // Pass nonce and aud to getVpToken
                Payload payload = getVpToken(iat, exp, jti, nonce, aud);
        
                Gson payLoadGson = new Gson();
                String headerJson = gson.toJson(header);
                String payloadJson = payLoadGson.toJson(payload);
        
                String headerBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
                String payloadBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        
                return headerBase64Url + "." + payloadBase64Url;
            }
        
        
            // Add iat, exp, jti, nonce, aud parameters
            private Payload getVpToken(long iat, long exp, String jti, String nonce, String aud){
                Payload container = new Payload();
        
                container.setIssuer("did:example:holder");
                container.setAudience(aud); // Set with the aud value received from the Authorization Request
                container.setNonce(nonce); // Set with the nonce value received from the Authorization Request
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
                vpProof.setChallenge(nonce); // Use nonce value
                vpProof.setDomain(aud); // Use aud value
                vpProof.setProofValue("zQeVbY4oQowNiQoClz9Qg8X6PpuKy4tP9t8rKHHB3P4...");
                vp.setVpProof(vpProof);
        
                WalletData walletData = loadVcFileAsVcItem()[0];
                byte[] decodedBytes = android.util.Base64.decode((String) walletData.getCredential(), android.util.Base64.DEFAULT);
                String vc = new String(decodedBytes, StandardCharsets.UTF_8);
                VerifiableCredential vcItem = new Gson().fromJson(vc, VerifiableCredential.class);
                if (vcItem != null) {
                    vp.setVerifiableCredential(Collections.singletonList(vcItem));
                } else {
                    Log.e("sangjun", "VC file cannot be loaded and is not included in VP.");
                }
        
                container.setVp(vp);
        
                return container;
            }
        
            private WalletData[] loadVcFileAsVcItem() {
                File file = new File(getFilesDir(), "vc.json");
                String vcData = "";
                if (!file.exists()) {
                    Log.e("sangjun", "vc.json file not found.");
                    return null;
                }
        
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[(int) file.length()];
                    fis.read(buffer);
                    vcData = new String(buffer, StandardCharsets.UTF_8);
        
        //            byte[] decodedBytes = android.util.Base64.decode(encodedVcData, android.util.Base64.DEFAULT);
        //            String decodedVcJson = new String(decodedBytes, StandardCharsets.UTF_8);
                    Log.d("sangjun", "vc load :\n" + vcData);
        
                    return gson.fromJson(vcData, WalletData[].class);
        
                } catch (IOException | JsonSyntaxException e) {
                    Log.e("sangjun", "VC file read or JSON parsing error", e);
                    return null;
                }
            }

    // todo : sample key
    private PrivateKey getPrivateKeyObject(byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    private PublicKey getPublicKeyObject(byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    private byte[] unCompressPublicKey(byte[] compressedPublicKey) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParams = parameters.getParameterSpec(ECParameterSpec.class);

            byte[] xBytes = Arrays.copyOfRange(compressedPublicKey, 1, compressedPublicKey.length);
            BigInteger x = new BigInteger(1, xBytes);

            BigInteger p = ((java.security.spec.ECFieldFp) ecParams.getCurve().getField()).getP();
            BigInteger a = ecParams.getCurve().getA();
            BigInteger b = ecParams.getCurve().getB();
            BigInteger rhs = x.modPow(BigInteger.valueOf(3), p)
                    .add(a.multiply(x)).add(b).mod(p);
            BigInteger y = rhs.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p);

            boolean yOdd = (compressedPublicKey[0] & 1) == 1;
            if (y.testBit(0) != yOdd) {
                y = p.subtract(y);
            }

            ECPoint ecPoint = new ECPoint(x, y);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(ecPoint, ecParams);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey pubKey = kf.generatePublic(pubSpec);
            Log.d("sangjun", "pubkey: " + Base64.getEncoder().encodeToString(pubKey.getEncoded()));
            return pubKey.getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        }
    }
}