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
import com.upokecenter.cbor.CBORObject;

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
import java.security.MessageDigest;
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

import COSE.Attribute;
import COSE.OneKey;
import COSE.Sign1Message;
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
    private String responseUri;

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

        responseUri = extractJsonField(responseDataString, "response_uri");
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
        } else if ("NationalID".equals(format) || "NationalIDCert".equals(format)) {
            return createVpTokenSdJwt();
        } else if ("mDL".equals(format) || "mDocPID".equals(format)) {
            return createVpTokenMDoc(); //todo: mDoc Device signed 포함된 객체 생성
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

    public String createVpTokenMDoc() throws Exception {
        String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
        String PUBLIC_KEY = "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";

        PrivateKey holderPrivateKey = getPrivateKeyObject(Base64.getDecoder().decode(PRIVATE_KEY));

        WalletData walletData = loadVcFileAsVcItem()[0];
        String mDoc = (String) walletData.getCredential();

        Log.d("sangjun", "Create selectively disclosed VP token based on DCQL");

        List<String> holderX5cChain = readCertFromAssets("holder.crt");
        if (holderX5cChain.isEmpty()) {
            Log.e("sangjun", "Certificate not found or empty");
            return null;
        }
        Log.d("sangjun", "holderX5cChain : " + holderX5cChain.get(0));

//        String docType = "org.iso.18013.5.1.mDL";
        // test용 발급 mDoc
//        mDoc = "omppc3N1ZXJBdXRohEOhASahGCGCWQGBMIIBfTCCASOgAwIBAgIGAZwhrbQBMAoGCCqGSM49BAMCMDUxFzAVBgNVBAMMDkludGVybWVkaWF0ZUNBMQ0wCwYDVQQKDARSYW9uMQswCQYDVQQGEwJLUjAeFw0yNjAyMDMwNDA1NDhaFw0yNzAyMDMwNDA1NDhaMDQxFjAUBgNVBAMMDVNELUpXVC1Jc3N1ZXIxDTALBgNVBAoMBFJhb24xCzAJBgNVBAYTAktSMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEL_nA2zwPWhf4UNFiCclSZn86-d9_7_vcbwsPnf6bW2R0zoXw649_BqyRkKsbuHdTbMKwI_iZagOxYXp9-G1qE6MgMB4wDAYDVR0TAQH_BAIwADAOBgNVHQ8BAf8EBAMCBaAwCgYIKoZIzj0EAwIDSAAwRQIhALPiPNANYsVvfmsCsjgoQT11J3V2cS5IGaqCeE7EF6cRAiAm6e8tqh4eKtaX8rwhlQJL8oeY11J_C8V0F8SUEQCBJ1kBfjCCAXowggEfoAMCAQICBgGcIa2mMDAKBggqhkjOPQQDAjAtMQ8wDQYDVQQDDAZSb290Q0ExDTALBgNVBAoMBFJhb24xCzAJBgNVBAYTAktSMB4XDTI2MDIwMzA0MDU0OFoXDTI3MDIwMzA0MDU0OFowNTEXMBUGA1UEAwwOSW50ZXJtZWRpYXRlQ0ExDTALBgNVBAoMBFJhb24xCzAJBgNVBAYTAktSMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEL_nA2zwPWhf4UNFiCclSZn86-d9_7_vcbwsPnf6bW2R0zoXw649_BqyRkKsbuHdTbMKwI_iZagOxYXp9-G1qE6MjMCEwDwYDVR0TAQH_BAUwAwEB_zAOBgNVHQ8BAf8EBAMCAQYwCgYIKoZIzj0EAwIDSQAwRgIhAPtuVjeYo-a-MZRCMa2Ph3hrlE-0d-RBdkmZhlvN0duKAiEAwC0PT25bveDppIl3EOAOOju1O0-dli0PQvM6WciopNJZAtHYGFkCzKZnZG9jVHlwZXdldS5ldXJvcGEuZWMuZXVkaS5waWQuMWd2ZXJzaW9uYzEuMGx2YWxpZGl0eUluZm-jZnNpZ25lZMB4GDIwMjYtMDMtMDlUMDE6Mjc6NTYuNDAyWml2YWxpZEZyb23AeBgyMDI2LTAzLTA5VDAxOjI3OjU2LjQwMlpqdmFsaWRVbnRpbMB4GDIwMjctMDMtMDlUMDE6Mjc6NTYuNDAyWmx2YWx1ZURpZ2VzdHOhd2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xqwBYIB9l7lSpxrU6RZaxaIOvRfQMD6AhC1BU1uqy_kkb-8fRAVggK1o1FekmwyemeNTZKvjxgEY1mp4MxQua2bbKk5tvtKcCWCAO7F_p3Pn01osyZnJtf8spYMjeiljqvSPacCW8w3qZ7ANYIGR93isZuB15405zO4pG1lguweyswm_3wvPyNJUfa9TeBFggF6FjORSUoV4WwUkrcVjoX6M9t9I_uD66II5iBFy5mYcFWCDtkReCX_HchPXWiadvMB2kA0xOhead9ONJPRHWzEWKmgZYIHOGUy1_E1pt-m2u4OyhCB0Shes6j-_RboS2AS3JnJImB1ggWvA1N6PJZoijviwRfMr7E5tksoG6YS-isbdOQIQXdXAIWCAkFkQcw0uG4e2hbgMcXuD7FrUz6zybDR9hlwj8F3KhGglYICuXasfH9d7Y1eSTFjpYwwjEjD2QGS8j3hY4HY-wBMrrClgghP_PbqMdIyBSAxkikSIFZACgKbo_wTNCPefUsGPGhGdtZGV2aWNlS2V5SW5mb6FpZGV2aWNlS2V5pAECIAEhWCBr4Y5ZkIp0yxUzBW4F0j84VHUiFgqrB6YxyhgzkWIKCSJYIEeSlEB0mpdcj11_zW-fsXg1BfxpLu_4bDMWYHxrSYjdb2RpZ2VzdEFsZ29yaXRobWdTSEEtMjU2WEDgv2Zq1o3ChA_JoJSG-s2M7dxDvAxlujJziRg00PeEGRJT6k1vxDOO3IYdZdwC4AcmA-WdIqjXMnMHTsJhFfzFam5hbWVTcGFjZXOhd2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xi9gYWF2kZnJhbmRvbVD8w1GV69hBB3KNTLoMsaPfaGRpZ2VzdElEAGxlbGVtZW50VmFsdWVjUk9LcWVsZW1lbnRJZGVudGlmaWVydnVuX2Rpc3Rpbmd1aXNoaW5nX3NpZ27YGFiepGZyYW5kb21QDwdXWzXC2Fc5ciVhEGDtwmhkaWdlc3RJRAFsZWxlbWVudFZhbHVlgaNqaXNzdWVfZGF0ZcBqMjAyNC0wMS0xMGtleHBpcnlfZGF0ZcBqMjAzNC0wMS0xMHV2ZWhpY2xlX2NhdGVnb3J5X2NvZGVhQnFlbGVtZW50SWRlbnRpZmllcnJkcml2aW5nX3ByaXZpbGVnZXPYGFhfpGZyYW5kb21Q5MfEfO9ZPxmsJa2CmgLhl2hkaWdlc3RJRAJsZWxlbWVudFZhbHVlbDExLTEyMzQ1Ni03OHFlbGVtZW50SWRlbnRpZmllcm9kb2N1bWVudF9udW1iZXLYGFhjpGZyYW5kb21QAS3Uc2GcyKz48IXm1y5f4WhkaWdlc3RJRANsZWxlbWVudFZhbHVlwHQyMDI2LTAxLTEwVDA5OjMwOjAwWnFlbGVtZW50SWRlbnRpZmllcmppc3N1ZV9kYXRl2BhYVaRmcmFuZG9tUBcQ4armZGtjWuYs0iBWo8toZGlnZXN0SUQEbGVsZW1lbnRWYWx1ZWJLUnFlbGVtZW50SWRlbnRpZmllcm9pc3N1aW5nX2NvdW50cnnYGFhzpGZyYW5kb21QuDFpwTml9E2DpfYnkWV19mhkaWdlc3RJRAVsZWxlbWVudFZhbHVleB1Lb3JlYW4gTmF0aW9uYWwgUG9saWNlIEFnZW5jeXFlbGVtZW50SWRlbnRpZmllcnFpc3N1aW5nX2F1dGhvcml0edgYWFukZnJhbmRvbVAJ-EOtekv1KCWzLL3CE26oaGRpZ2VzdElEBmxlbGVtZW50VmFsdWXZA-xqMTk5MC0wNS0xNXFlbGVtZW50SWRlbnRpZmllcmpiaXJ0aF9kYXRl2BhYZKRmcmFuZG9tUGKR_4Sm-6nGWOIzAzEud-xoZGlnZXN0SUQHbGVsZW1lbnRWYWx1ZcB0MjAzNi0wMS0xMFQwMDowMDowMFpxZWxlbWVudElkZW50aWZpZXJrZXhwaXJ5X2RhdGXYGFhSpGZyYW5kb21QcyhOtN_Pbiuv36Njr-DScWhkaWdlc3RJRAhsZWxlbWVudFZhbHVlZFJhb25xZWxlbWVudElkZW50aWZpZXJqZ2l2ZW5fbmFtZdgYWRtHpGZyYW5kb21QVJULI0fXGX97ReXebfvr3GhkaWdlc3RJRAlsZWxlbWVudFZhbHVlWRr5_9j_4AAQSkZJRgABAQEASABIAAD_4QCeRXhpZgAATU0AKgAAAAgABQESAAMAAAABAAEAAAEaAAUAAAABAAAASgEbAAUAAAABAAAAUgEoAAMAAAABAAIAAIdpAAQAAAABAAAAWgAAAAAAAABIAAAAAQAAAEgAAAABAAOShgAHAAAAEgAAAISgAgAEAAAAAQAAAGSgAwAEAAAAAQAAAHgAAAAAQVNDSUkAAABTY3JlZW5zaG90_-INJElDQ19QUk9GSUxFAAEBAAANFGFwcGwCEAAAbW50clJHQiBYWVogB-oAAQACAAcAKwAjYWNzcEFQUEwAAAAAQVBQTAAAAAAAAAAAAAAAAAAAAAAAAPbWAAEAAAAA0y1hcHBsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAARZGVzYwAAAVAAAABiZHNjbQAAAbQAAAHwY3BydAAAA6QAAAAjd3RwdAAAA8gAAAAUclhZWgAAA9wAAAAUZ1hZWgAAA_AAAAAUYlhZWgAABAQAAAAUclRSQwAABBgAAAgMYWFyZwAADCQAAAAgdmNndAAADEQAAAAwbmRpbgAADHQAAAA-bW1vZAAADLQAAAAodmNncAAADNwAAAA4YlRSQwAABBgAAAgMZ1RSQwAABBgAAAgMYWFiZwAADCQAAAAgYWFnZwAADCQAAAAgZGVzYwAAAAAAAAAIRGlzcGxheQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG1sdWMAAAAAAAAAJwAAAAxockhSAAAADAAAAeRrb0tSAAAADAAAAeRuYk5PAAAADAAAAeRpZAAAAAAADAAAAeRodUhVAAAADAAAAeRjc0NaAAAADAAAAeRzbFNJAAAADAAAAeRkYURLAAAADAAAAeRubE5MAAAADAAAAeRmaUZJAAAADAAAAeRpdElUAAAADAAAAeRlc0VTAAAADAAAAeRyb1JPAAAADAAAAeRmckNBAAAADAAAAeRhcgAAAAAADAAAAeR1a1VBAAAADAAAAeRoZUlMAAAADAAAAeR6aFRXAAAADAAAAeR2aVZOAAAADAAAAeRza1NLAAAADAAAAeR6aENOAAAADAAAAeRydVJVAAAADAAAAeRlbkdCAAAADAAAAeRmckZSAAAADAAAAeRtcwAAAAAADAAAAeRoaUlOAAAADAAAAeR0aFRIAAAADAAAAeRjYUVTAAAADAAAAeRlbkFVAAAADAAAAeRlc1hMAAAADAAAAeRkZURFAAAADAAAAeRlblVTAAAADAAAAeRwdEJSAAAADAAAAeRwbFBMAAAADAAAAeRlbEdSAAAADAAAAeRzdlNFAAAADAAAAeR0clRSAAAADAAAAeRwdFBUAAAADAAAAeRqYUpQAAAADAAAAeQAQgBLADUANQAwAFl0ZXh0AAAAAENvcHlyaWdodCBBcHBsZSBJbmMuLCAyMDI2AABYWVogAAAAAAAA89gAAQAAAAEWCFhZWiAAAAAAAABxWgAAOkkAAAJtWFlaIAAAAAAAAGBQAAC48AAAEGNYWVogAAAAAAAAJSwAAAzHAADAXWN1cnYAAAAAAAAEAAAAAAUACgAPABQAGQAeACMAKAAtADIANgA7AEAARQBKAE8AVABZAF4AYwBoAG0AcgB3AHwAgQCGAIsAkACVAJoAnwCjAKgArQCyALcAvADBAMYAywDQANUA2wDgAOUA6wDwAPYA-wEBAQcBDQETARkBHwElASsBMgE4AT4BRQFMAVIBWQFgAWcBbgF1AXwBgwGLAZIBmgGhAakBsQG5AcEByQHRAdkB4QHpAfIB-gIDAgwCFAIdAiYCLwI4AkECSwJUAl0CZwJxAnoChAKOApgCogKsArYCwQLLAtUC4ALrAvUDAAMLAxYDIQMtAzgDQwNPA1oDZgNyA34DigOWA6IDrgO6A8cD0wPgA-wD-QQGBBMEIAQtBDsESARVBGMEcQR-BIwEmgSoBLYExATTBOEE8AT-BQ0FHAUrBToFSQVYBWcFdwWGBZYFpgW1BcUF1QXlBfYGBgYWBicGNwZIBlkGagZ7BowGnQavBsAG0QbjBvUHBwcZBysHPQdPB2EHdAeGB5kHrAe_B9IH5Qf4CAsIHwgyCEYIWghuCIIIlgiqCL4I0gjnCPsJEAklCToJTwlkCXkJjwmkCboJzwnlCfsKEQonCj0KVApqCoEKmAquCsUK3ArzCwsLIgs5C1ELaQuAC5gLsAvIC-EL-QwSDCoMQwxcDHUMjgynDMAM2QzzDQ0NJg1ADVoNdA2ODakNww3eDfgOEw4uDkkOZA5_DpsOtg7SDu4PCQ8lD0EPXg96D5YPsw_PD-wQCRAmEEMQYRB-EJsQuRDXEPURExExEU8RbRGMEaoRyRHoEgcSJhJFEmQShBKjEsMS4xMDEyMTQxNjE4MTpBPFE-UUBhQnFEkUahSLFK0UzhTwFRIVNBVWFXgVmxW9FeAWAxYmFkkWbBaPFrIW1hb6Fx0XQRdlF4kXrhfSF_cYGxhAGGUYihivGNUY-hkgGUUZaxmRGbcZ3RoEGioaURp3Gp4axRrsGxQbOxtjG4obshvaHAIcKhxSHHscoxzMHPUdHh1HHXAdmR3DHeweFh5AHmoelB6-HukfEx8-H2kflB-_H-ogFSBBIGwgmCDEIPAhHCFIIXUhoSHOIfsiJyJVIoIiryLdIwojOCNmI5QjwiPwJB8kTSR8JKsk2iUJJTglaCWXJccl9yYnJlcmhya3JugnGCdJJ3onqyfcKA0oPyhxKKIo1CkGKTgpaymdKdAqAio1KmgqmyrPKwIrNitpK50r0SwFLDksbiyiLNctDC1BLXYtqy3hLhYuTC6CLrcu7i8kL1ovkS_HL_4wNTBsMKQw2zESMUoxgjG6MfIyKjJjMpsy1DMNM0YzfzO4M_E0KzRlNJ402DUTNU01hzXCNf02NzZyNq426TckN2A3nDfXOBQ4UDiMOMg5BTlCOX85vDn5OjY6dDqyOu87LTtrO6o76DwnPGU8pDzjPSI9YT2hPeA-ID5gPqA-4D8hP2E_oj_iQCNAZECmQOdBKUFqQaxB7kIwQnJCtUL3QzpDfUPARANER0SKRM5FEkVVRZpF3kYiRmdGq0bwRzVHe0fASAVIS0iRSNdJHUljSalJ8Eo3Sn1KxEsMS1NLmkviTCpMcky6TQJNSk2TTdxOJU5uTrdPAE9JT5NP3VAnUHFQu1EGUVBRm1HmUjFSfFLHUxNTX1OqU_ZUQlSPVNtVKFV1VcJWD1ZcVqlW91dEV5JX4FgvWH1Yy1kaWWlZuFoHWlZaplr1W0VblVvlXDVchlzWXSddeF3JXhpebF69Xw9fYV-zYAVgV2CqYPxhT2GiYfViSWKcYvBjQ2OXY-tkQGSUZOllPWWSZedmPWaSZuhnPWeTZ-loP2iWaOxpQ2maafFqSGqfavdrT2una_9sV2yvbQhtYG25bhJua27Ebx5veG_RcCtwhnDgcTpxlXHwcktypnMBc11zuHQUdHB0zHUodYV14XY-dpt2-HdWd7N4EXhueMx5KnmJeed6RnqlewR7Y3vCfCF8gXzhfUF9oX4BfmJ-wn8jf4R_5YBHgKiBCoFrgc2CMIKSgvSDV4O6hB2EgITjhUeFq4YOhnKG14c7h5-IBIhpiM6JM4mZif6KZIrKizCLlov8jGOMyo0xjZiN_45mjs6PNo-ekAaQbpDWkT-RqJIRknqS45NNk7aUIJSKlPSVX5XJljSWn5cKl3WX4JhMmLiZJJmQmfyaaJrVm0Kbr5wcnImc951kndKeQJ6unx2fi5_6oGmg2KFHobaiJqKWowajdqPmpFakx6U4pammGqaLpv2nbqfgqFKoxKk3qamqHKqPqwKrdavprFys0K1ErbiuLa6hrxavi7AAsHWw6rFgsdayS7LCszizrrQltJy1E7WKtgG2ebbwt2i34LhZuNG5SrnCuju6tbsuu6e8IbybvRW9j74KvoS-_796v_XAcMDswWfB48JfwtvDWMPUxFHEzsVLxcjGRsbDx0HHv8g9yLzJOsm5yjjKt8s2y7bMNcy1zTXNtc42zrbPN8-40DnQutE80b7SP9LB00TTxtRJ1MvVTtXR1lXW2Ndc1-DYZNjo2WzZ8dp22vvbgNwF3IrdEN2W3hzeot8p36_gNuC94UThzOJT4tvjY-Pr5HPk_OWE5g3mlucf56noMui86Ubp0Opb6uXrcOv77IbtEe2c7ijutO9A78zwWPDl8XLx__KM8xnzp_Q09ML1UPXe9m32-_eK-Bn4qPk4-cf6V_rn-3f8B_yY_Sn9uv5L_tz_bf__cGFyYQAAAAAAAwAAAAJmZgAA8qcAAA1ZAAAT0AAAClt2Y2d0AAAAAAAAAAEAAQAAAAAAAAABAAAAAQAAAAAAAAABAAAAAQAAAAAAAAABAABuZGluAAAAAAAAADYAAKUAAABVwAAATgAAAKFAAAAmAAAADMAAAFBAAABUQAACMzMAAjMzAAIzMwAAAAAAAAAAbW1vZAAAAAAAAB5tAABbQQAGmfzgKYR4AAAAAAAAAAAAAAAAAAAAAHZjZ3AAAAAAAAMAAAACZmYAAwAAAAJmZgADAAAAAmZmAAAAAjMzAAAAAAACMzMAAAAAAAIzMwAA_-ECRmh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8APHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNi4wLjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgICAgICAgICB4bWxuczpleGlmPSJodHRwOi8vbnMuYWRvYmUuY29tL2V4aWYvMS4wLyIKICAgICAgICAgICAgeG1sbnM6dGlmZj0iaHR0cDovL25zLmFkb2JlLmNvbS90aWZmLzEuMC8iPgogICAgICAgICA8ZXhpZjpQaXhlbFlEaW1lbnNpb24-MzUxPC9leGlmOlBpeGVsWURpbWVuc2lvbj4KICAgICAgICAgPGV4aWY6VXNlckNvbW1lbnQ-U2NyZWVuc2hvdDwvZXhpZjpVc2VyQ29tbWVudD4KICAgICAgICAgPGV4aWY6UGl4ZWxYRGltZW5zaW9uPjI5MzwvZXhpZjpQaXhlbFhEaW1lbnNpb24-CiAgICAgICAgIDx0aWZmOk9yaWVudGF0aW9uPjE8L3RpZmY6T3JpZW50YXRpb24-CiAgICAgIDwvcmRmOkRlc2NyaXB0aW9uPgogICA8L3JkZjpSREY-CjwveDp4bXBtZXRhPgr_2wBDAAUDBAQEAwUEBAQFBQUGBwwIBwcHBw8LCwkMEQ8SEhEPERETFhwXExQaFRERGCEYGh0dHx8fExciJCIeJBweHx7_2wBDAQUFBQcGBw4ICA4eFBEUHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh7_wAARCAB4AGQDASIAAhEBAxEB_8QAHAABAAIDAQEBAAAAAAAAAAAAAAYHAwQFAggB_8QAPBAAAQMEAAQEAgcDDQAAAAAAAQIDBAAFBhEHEiExEyJBURRhCDJCUnGBoRWRsRYXIzM0NlNygpKUo8L_xAAVAQEBAAAAAAAAAAAAAAAAAAAAAf_EABURAQEAAAAAAAAAAAAAAAAAAAAB_9oADAMBAAIRAxEAPwD7LpSlApSohnGdRrBOj2S22-TfcjmDcW2RNc3L6uuqPlaaHqpXfsAToEJfWnOutsggmbcYkXX-M8lH8TUBawvMsoQX86y6RCYcT1s-POKjMoB15VyP61wg78ySgHfaujB4Q8NYryZP8jLRKlAAfEzGBJeP4uOcyj--gksPIrBMVyxL5bJCvZqWhR_Q10wQRsHpURuvC_h1dUJRcMIx6QE_VKre3tP4EDY_KuQ5wtTanFysGyi-Y1IJ5hHMlUyErQOklh4kJT7-GUH50FjUqurVnd3sNzj2PiVb41tekLSzDvURRNumLJ0Ekq8zDhOtIXsEkAKUasUdRQKUpQKUpQKUpQRXiblLuM2Fv9nRkTb3cX0wrTDUvl8eSsHWz3CEgFaiAdJSacO8PZxiA87IfNwvk9QeulycH9JKd_8AKE9ko7AVwbNrJ-ON4uLikuQcTiotsVIJ6TH0h19RHYkNllIPccyx61ZFAHalKUClKUGjfrRbb5aJVpu8JibBlNlp9h5AUhxJ7gioNw_mXDE8oVw4vcuTMjFhUnHrhIVzLfjpOlx3Fd1ONbT1PVSCD1IUaseoHxxtsh_Czf7Y0pd3xt5N3g8iQVqLWy40N9vEbK0H5KoJ5StWzzmLnaolxiqCmJTKHmlA90qAI_jW1QKUpQKelK8PdGl6-6aCv-AnjScSud4khPj3S_XGSVD7SBJW23_1oQKsOoLwCA_mksZ9VIdUr8S6vdTlSkpSVKIAHck0I_aVrtT4TrnhNTI7jn3UupJ_dutigUpWKRJjxk80h9plPu4sJH60GWscllD8Z1hwAocQUKB9QRqkeQxIRzsPNup90KCh-lZD2oK--js8g8JbVb2-bltLki1Dm76jPrZH6IFWDVbfR1_udeh6DKb0B_z3qsmgUpSgVGM-y6PjDEJhNtnXe53J4sQbfCSkuvqCSpR2ohKEpA2VKIA6DuQDJ6rrNQIfG_Arm-8Ex341ytqEE9C-4ht1P58rDn76DL9Hl8PcKLansth-UwtOweRaJDiVJ6eoII_KtfipjOHyQ5fOIGU3GNaGikNxXLoqJEQda1yt8pcJPXzFXy1XvgI18DYchs6lhS4GTXJJAGtB2Qp9I_2uipFc8LsV0zCNk9zYXNmQ2PBhtvq52YxO-ZxtBGg4QdFXfQ1RFIM2T6N1xlpjQLo7aJJWENPpkvxwVE6GlOeUkntV4cPcdcxewC1Kv1yvbQdU4y_PcC3EIVrSAoDqkehPvVRYJwFm2vO1z7_LtlxsjXjBDHKpSpIWFJSHEqGgAFHfU7IBGquPB8ai4nYEWSDLmyIbLi1RxKd8RTKFKJDSTrfInsN7OvWhHaeSpbS0JWUKUkgKA6g-9fP-T4Nwkx2WlPETMbtfLqtPiFE2cpbyhvqrwmhsD8gPavoFY5kEAkbHcVSufcDY83FW42OSgb18eqbMnXFZU7cFLSpKvEcA2NbHKAOUBIAA9A1MLxbg1fbiRw-yW42a8stlaU2-6PMPIB2AssO7SsdPtIUOlXkwhTbCELcU6pIAK1AbUR6nXTr8qq7DuDtsiYHbLPf1pdu0GaqezcIKi07GdLnNytufW5deU76KBPTR1VoSXEsRXHVnSW0FRJ-Q3QUtwFy-Lbi9jlwgTIyLtkN3XbbisJ-Hlu_FvrWyCDzJcSEq6KACgCQT11d1UBijiJGA8H2mGVKfvF8N0BCeqEFEiQtZ9hpQH-oVf47UIUpSilQzjFY7jeMPMixpCr3aZDdyto3rneaO_D3o6C08yCfZVTOlBUPBG_2y6Z_mLlr8X4a9IhX1AWnlKFra-HdaUD1C0KjjmBGwVD3q3qqjLbTbcJ4r49mkCMiHFvMldpvJQQhsuPgFh5Q-8XG0o6dy4N7q1xRIUpUfzKzXu6tRXLDk0mxS4y1LBTHQ8y_sa5XUKGykd_KpJ-dFSClQfHMf4gi7xpuUZ1ElR4yyoQ7VaRFbf2kjTpcW4ogE70kp6gVOKBUS4yXV-y8LMkuMVIXKbt7qIyCrXO6tJQhO_TalAfnUtqtOMbbWSX_F-HpPO1c5Sp9yQlZSoQow2SCO23VNJ_M67UHI4P25q75WxdYaUnH8StSLBaHEHyyH9J-KdTo6KUlCGweh2lz0q4q1LNbYNotca12yIzDhRWw0wwygJQ2gdAAB2FbdApSlApSlBw89xuNlmIXGwSlqaTLa028n6zLgPMhxPspKglQPyrlcKcok36yOW-9NiNkloX8Hdox9HUjo6n3bcGlpPsdHqCKmNQLiJilzN4j5xhpaayaC34TrDiuVm6RtkmO6fcdShfdKvkSCE8Xzcp5SArXQketVo7beNLY5nM3wVCQdAqx98c3yO5XSpPw8zWzZrYxcbW6UONnw5cRwjxorvqhYB7-xHQjqDXVyGx2jIbWu2Xy2xblCWpKlMSWgtBUk7SdH1BGwaCCuQuMb7-mszwaOrl2Gk2R90n3Vv4gH9KmeIx8ijWcN5RcrfcbjzqKnoURUdrl35QEKWs7HvvrWpjODYhjU52fYccttulut-Et9hgBxSN75ebvy79O1dydLjQYb0yY-1HjsoK3XXFBKUJHcknsKDHeLjBtFqlXS5SmokKK0p5951XKltCRskn8KgnCWLNvVyunEe7x3Yz16Shm2RnUlK41vbJLQUk9luEqcV0BHMlJ-rXJguPcYL0mWQWuH9tknwm1HS7zJbV9ZY-zHQodEnqtQ66SPNbKEhI0AAKI9UpSilKUoFKUoFD2pXBz_ACe34lis29XBzQaRysNJ6rfeV0baQnupalEAAepoKv4aYJGvOAWrIrNcH7BkzLklDdziJBLqEyXSGn0Hyut9-h6jZ5SD1qTs5VxDsPLGybBXL2kHl_aOOvIUhYAHnUw6pK2yTvypK_xrucIbLPx_hpYbTdQ2LizDSqYG_qh9fnc18uZRqV6oivnuIN-lKSxYuGeUSnlnXiTQzDZbPupS182v8qVGtePg99yqZHuPEq4Rn47DiXWMft5UILaxrReWrSpCgdkbCU9jy7ANWToU9KCtvo5DWB3BIAATkd4SAOwAnvAD8AOlWTVacJn2MfyfKMClD4eUi5yLtACz_aospZdK0e_K4pxJA6jQJ6EVZdFKUpQKUpQKUpQKhFk4fNJyUZNlF2k5Jd2XFKgqkICI8AHYAYZHlSrRIKztXU9ddKUoJuBqlKUClKUEczjDLNlsZgXFDzMuIvxYU6K6WpMVf3m1jqN9iOxHQgitzEbfdbXY2YN5vS71LaKgZq2EtLcTs8vMlPl5gNAkAAnZ0O1KUHXpSlApSlB__9lxZWxlbWVudElkZW50aWZpZXJocG9ydHJhaXTYGFhSpGZyYW5kb21QuX6PXNWA5FTdUA6YGCGGtmhkaWdlc3RJRApsZWxlbWVudFZhbHVlY0tpbXFlbGVtZW50SWRlbnRpZmllcmtmYW1pbHlfbmFtZQ";
        byte[] mDocBytes = Base64.getUrlDecoder().decode(mDoc);
        CBORObject issuerSigned = CBORObject.DecodeFromBytes(mDocBytes);

        // mDoc을 base64 decode 해서 byte[]로 변환 후 CBOR Object로 해서 issuerSisgned에 넣어야함
        CBORObject nameSpaces = issuerSigned.get("nameSpaces");
        String namespaceStr = "";
        String docType = "";
        for (CBORObject keyObj : nameSpaces.getKeys()) {
            namespaceStr = keyObj.AsString(); // "eu.europa.ec.eudi.pid.1" 추출
            Log.d("sangjun", "!!!!!!!!namespace : " + namespaceStr);
        }
        if(namespaceStr.equals("eu.europa.ec.eudi.pid.1"))
            docType = "eu.europa.ec.eudi.pid.1";
        else if(namespaceStr.equals("org.iso.18013.5.1"))
            docType = "org.iso.18013.5.1.mDL";

        CBORObject document = CBORObject.NewMap();
        document.Add("docType", docType);
        document.Add("issuerSigned", issuerSigned);

        // deviceSigned를 받아서 넣어야함
        // aud = client ID
        CBORObject deviceSigned = generateDeviceAuth(holderPrivateKey, docType);
        document.Add("deviceSigned", deviceSigned);

        CBORObject deviceResponse = CBORObject.NewMap();
        deviceResponse.Add("status", 0);
        deviceResponse.Add("version", "1.0");
        CBORObject documentsArray = CBORObject.NewArray();
        documentsArray.Add(document);
        deviceResponse.Add("documents", documentsArray);

        byte[] finalBytes = deviceResponse.EncodeToBytes();
        String vpToken = Base64.getUrlEncoder().withoutPadding().encodeToString(finalBytes);
        String dcqlId = "query_0";
        return "{\"" + dcqlId + "\":[\"" + vpToken + "\"]}";

    }

    public CBORObject buildSessionTranscript() throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        // OID4VPHandover 구조체 생성
        CBORObject handoverInfo = CBORObject.NewArray();
        handoverInfo.Add(aud); // clientId
        handoverInfo.Add(nonce);
        handoverInfo.Add(CBORObject.Null);
        handoverInfo.Add(responseUri);

        byte[] handoverInfoByte = handoverInfo.EncodeToBytes();
        byte[] handoverInfoHash = sha256.digest(handoverInfoByte);

        CBORObject handover = CBORObject.NewArray();
        handover.Add("OpenID4VPHandover"); // "OpenID4VPHandover"
        handover.Add(handoverInfoHash);

        // SessionTranscript 배열 생성
        CBORObject sessionTranscript = CBORObject.NewArray();
        sessionTranscript.Add(CBORObject.Null); // DeviceEngagement (null)
        sessionTranscript.Add(CBORObject.Null); // EReaderKey (null)
        sessionTranscript.Add(handover);        // handover -> sha256

        return sessionTranscript;

    }

    public CBORObject generateDeviceAuth(PrivateKey privateKey, String docType) throws Exception {
        CBORObject sessionTranscript = buildSessionTranscript();

        // DeviceNameSpaces 생성
        CBORObject emptyDeviceNameSpaces = CBORObject.NewMap();
        byte[] deviceNameSpacesBytes = emptyDeviceNameSpaces.EncodeToBytes();
        CBORObject deviceNameSpacesTagged = CBORObject.FromObject(deviceNameSpacesBytes).WithTag(24);

        // 서명 대상이 될 DeviceAuthentication 배열 조립
        // [ "DeviceAuthentication", SessionTranscript, docType, NameSpaces ]
        CBORObject deviceAuth = CBORObject.NewArray();
        deviceAuth.Add("DeviceAuthentication"); // 고정 문자열
        deviceAuth.Add(sessionTranscript);
        deviceAuth.Add(docType); // 제출할 docType
        // null?? nameSpacesMap은 발급 받은 것에서 넣어야함?
        deviceAuth.Add(deviceNameSpacesTagged);

        // DeviceAuthentication 바이트화
        byte[] deviceAuthBytes = deviceAuth.EncodeToBytes();

        // 지갑개인키로 COSE_Sign1 서명
        Sign1Message msg = new Sign1Message();
        msg.addAttribute(CBORObject.FromObject(1), CBORObject.FromObject(-7), Attribute.PROTECTED);
        OneKey devicePrivateKey = new OneKey(null, privateKey);
        msg.SetContent(CBORObject.FromObject(deviceAuthBytes).WithTag(24).EncodeToBytes());
        msg.sign(devicePrivateKey);

        // DeviceSigned 객체 조립
        // 구조: { "deviceAuth": { "deviceSignature": [COSE_Sign1] } }
        CBORObject signatureObj = CBORObject.DecodeFromBytes(msg.EncodeToBytes());
        if (signatureObj.isTagged()) {
            signatureObj = signatureObj.Untag(); // Tag 18 제거 (표준 호환성)???
        }
        CBORObject deviceAuthMap = CBORObject.NewMap();
        deviceAuthMap.Add("deviceSignature", signatureObj); // deviceAuth 내부에 서명 삽입

        CBORObject deviceSigned = CBORObject.NewMap();
        deviceSigned.Add("deviceAuth", deviceAuthMap);
        // nameSpaces는 기기 자체 클레임이 없으므로 생략

        byte[] finalBytes = deviceSigned.EncodeToBytes();
        Log.d("sangjun", "deviceSigned : " + Base64.getUrlEncoder().withoutPadding().encodeToString(finalBytes));

        return deviceSigned;
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
