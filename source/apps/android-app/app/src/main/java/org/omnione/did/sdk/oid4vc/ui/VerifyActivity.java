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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.omnione.did.sdjwt.core.oid4vp.OID4VPHandler;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.network.ApiService;
import org.omnione.did.sdk.oid4vc.util.LogUtil;
import org.omnione.did.sdk.oid4vc.data.dto.tec.VerifiableCredential;
import org.omnione.did.sdk.oid4vc.data.dto.tec.Header;
import org.omnione.did.sdk.oid4vc.data.dto.tec.Payload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
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
    private String nonce;
    private String aud;

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

    private ApiService getApiService(String baseUrl) {
        if (!baseUrl.startsWith("http")) {
            baseUrl = "http://" + baseUrl;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(loggingInterceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setPrettyPrinting().create()))
                .build();

        return retrofit.create(ApiService.class);
    }

    private String extractJsonField(String jsonResponse, String fieldName) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
            if (jsonObject.has(fieldName) && !jsonObject.get(fieldName).isJsonNull()) {
                if (jsonObject.get(fieldName).isJsonObject()) {
                    return jsonObject.get(fieldName).toString();
                }
                return jsonObject.get(fieldName).getAsString();
            }
        } catch (JsonSyntaxException e) {
            Log.e("sangjun", "JSON parsing error: " + e.getMessage());
        }
        return null;
    }

    private void executeNetworkRequest(String requestUrl) {
        ApiService apiService = getApiService(requestUrl);
        Call<ResponseBody> call = apiService.getRequest(requestUrl);

        Log.d("sangjun", "Method: GET Authorization Request");
        Log.d("sangjun", "URL: " + requestUrl);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        Log.d("sangjun", "Authorization Request Response Data:\n" + responseData);
                        processAuthorizationRequest(responseData);
                    } catch (Exception e) {
                        Log.e("sangjun", "Error processing authorization request", e);
                        infoTextView.setText("Error: A problem occurred while processing the response data.");
                    }
                } else {
                    handleRequestError(response);
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

    private void processAuthorizationRequest(String responseData) throws Exception {
        String responseDataString = parsePayload(responseData);
        Log.d("sangjun", "Request Object: " + responseDataString);

        String responseUri = extractJsonField(responseDataString, "response_uri");
        authorizationRequestState = extractJsonField(responseDataString, "state");
        nonce = extractJsonField(responseDataString, "nonce");
        aud = extractJsonField(responseDataString, "client_id");
        String responseMode = extractJsonField(responseDataString, "response_mode");

        if (responseUri == null || authorizationRequestState == null || nonce == null || aud == null || responseMode == null) {
            Log.e("sangjun", "Missing required fields in authorization request.");
            infoTextView.setText("Error: Required fields not found in the response.");
            return;
        }

        Log.d("sangjun", "Extracted fields: responseUri=" + responseUri + ", state=" + authorizationRequestState + ", nonce=" + nonce + ", aud=" + aud);

        String vpToken = generateVpToken(responseDataString);
        Log.d("sangjun", "Start printing VP Token:");
        LogUtil.logLongString("VerifyActivity", vpToken);
        Log.d("sangjun", "End printing VP Token.");

        postVpTokenToVerifier(responseUri, vpToken, authorizationRequestState, responseMode);
    }

    private String generateVpToken(String requestObject) throws Exception {
        WalletData walletData = loadVcFileAsVcItem()[0];
        String format = walletData.getFormat();

        if ("TEC".equals(format) || "UCR".equals(format)) {
            return createUnsignedVpToken(nonce, aud);
        } else if ("NationalID".equals(format) || "mDL".equals(format) || "NationalIDCert".equals(format)) {
            return createVpTokenSdJwt();
        }
        return "";
    }

    private void handleRequestError(Response<ResponseBody> response) {
        String errorBody = "";
        try {
            if (response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (IOException e) {
            Log.e("sangjun", "Failed to read error body", e);
        }
        Log.e("sangjun", "Request failed (Code: " + response.code() + ", Body: " + errorBody + ")");
        infoTextView.setText("Error: Request failed (Code: " + response.code() + ")");
    }

    private void postVpTokenToVerifier(String responseUri, String vpToken, String state, String responseMode) {
        infoTextView.setText("Submitting VP Token to Verifier...");
        ApiService apiService = getApiService(responseUri);

        apiService.postVpToken(responseUri, vpToken, state).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("sangjun", "VP Token submission successful!");
                    try {
                        String responseBody = response.body().string();
                        infoTextView.setText("VC submission complete! : " + responseBody);
                        Log.d("sangjun", responseBody);
                    } catch (IOException e) {
                        Log.e("sangjun", "Error reading success response", e);
                    }
                    Toast.makeText(VerifyActivity.this, "VC submission successful!", Toast.LENGTH_LONG).show();
                } else {
                    handleRequestError(response);
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
        
    public String createVpTokenSdJwt() throws Exception {
        String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
        String PUBLIC_KEY = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";

        PrivateKey holderPrivateKey = getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));

        WalletData walletData = loadVcFileAsVcItem()[0];
        String sdJwtVc = (String) walletData.getCredential();

        Log.d("sangjun", "Create selectively disclosed VP token based on DCQL");

        List<String> holderX5cChain = readCertFromAssets("holder.crt");
        if (holderX5cChain.isEmpty()) {
            Log.e("sangjun", "Certificate not found or empty");
            return null;
        }

        Log.d("sangjun", "holderX5cChain : " + holderX5cChain.get(0));
        Set<String> requestedClaims = Set.of("family_name", "given_name", "phone_number", "birth_date", "email");

        String vpToken = OID4VPHandler.createVPTokenWithDcqlId(
                sdJwtVc,
                requestedClaims,
                "national_id",
                holderPrivateKey,
                holderX5cChain,
                aud,
                nonce
        );

        Log.d("sangjun", "VP Token (only name and date of birth disclosed):");
        Log.d("sangjun", "   " + vpToken);

        return vpToken;
    }

    public String createUnsignedVpToken(String nonce, String aud) {
        Header header = new Header("ES256", "JWT", "did:example:holder#key-1");
        long iat = Instant.now().getEpochSecond();
        long exp = iat + 2592000L;
        String jti = UUID.randomUUID().toString();

        Payload payload = getVpToken(iat, exp, jti, nonce, aud);

        String headerJson = gson.toJson(header);
        String payloadJson = gson.toJson(payload);

        String headerBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        return headerBase64Url + "." + payloadBase64Url;
    }

    private Payload getVpToken(long iat, long exp, String jti, String nonce, String aud) {
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
        if (!file.exists()) {
            Log.e("sangjun", "vc.json file not found.");
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            String vcData = new String(buffer, StandardCharsets.UTF_8);
            Log.d("sangjun", "vc load :\n" + vcData);
            return gson.fromJson(vcData, WalletData[].class);
        } catch (IOException | JsonSyntaxException e) {
            Log.e("sangjun", "VC file read or JSON parsing error", e);
            return null;
        }
    }

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
            Log.d("sangjun", "pubkey: " + Base64.getEncoder().encodeToString(pubKey.getEncoded()));
            return pubKey.getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static String parsePayload(String token) {
        try {
            String[] chunks = token.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();
            return new String(decoder.decode(chunks[1]));
        } catch (Exception e) {
            throw new RuntimeException("토큰 파싱 실패", e);
        }
    }

    private List<String> readCertFromAssets(String fileName) throws Exception {
        List<String> certs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        try (InputStream is = this.getAssets().open(fileName);
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
}