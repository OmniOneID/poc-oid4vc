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
import androidx.browser.customtabs.CustomTabsIntent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.omnione.did.sdk.oid4vc.data.dto.AuthorizationDetails;
import org.omnione.did.sdk.oid4vc.data.dto.IssuerMetadataResponse;
import org.omnione.did.sdk.oid4vc.data.dto.TokenResponse;
import org.omnione.did.sdk.oid4vc.network.ApiService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthorizeActivity extends AppCompatActivity {

    private static final String TAG = "AuthorizeActivity";

    // Data to be received from CredentialIssuanceActivity
    private String authorizationEndpoint; // Login screen URL of the authorization server
    private String issuerUrl; // Base URL of the Issuer
    private String issuerState; // State for CSRF prevention
    private List<String> credentialConfigurationIds; // List of Credential Configuration IDs to be issued
    private String clientId; // Client ID of the Wallet app
    private String tokenEndpoint; // Token endpoint received from IssuerMetadataResponse

    private String codeChallenge;
    private String codeVerifier;

    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gson = new GsonBuilder().setLenient().create();

        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && data.toString().startsWith("org.omnione.did.sdk.oid4vc://callback")) {
                handleAuthorizationResponse(data);
            } else {
                authorizationEndpoint = intent.getStringExtra("AUTHORIZATION_ENDPOINT");
                issuerUrl = intent.getStringExtra("ISSUER_URL");
                issuerState = intent.getStringExtra("ISSUER_STATE");
                credentialConfigurationIds = intent.getStringArrayListExtra("CREDENTIAL_CONFIG_IDS");
                clientId = intent.getStringExtra("CLIENT_ID");
                tokenEndpoint = intent.getStringExtra("TOKEN_ENDPOINT");

                if (authorizationEndpoint == null || issuerUrl == null || clientId == null || credentialConfigurationIds == null || tokenEndpoint == null) {
                    Log.e(TAG, "Missing required data: authorizationEndpoint, issuerUrl, clientId, credentialConfigurationIds, tokenEndpoint");
                    finishWithError("Insufficient data to start Authorization Flow.");
                    return;
                }

                startChromeCustomTabs(authorizationEndpoint, issuerState, credentialConfigurationIds, clientId);
            }
        } else {
            Log.e(TAG, "No intent data. Abnormal access.");
            finishWithError("Abnormal Authorization Flow access.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthorizationResponse(intent.getData());
    }

    private void startChromeCustomTabs(String authorizationEndpoint, String issuerState, List<String> credentialConfigurationIds, String clientId) {
        String redirectUri = "org.omnione.did.sdk.oid4vc://callback";

        List<AuthorizationDetails> authDetails = new ArrayList<>();
        if (credentialConfigurationIds != null) {
            for (String configId : credentialConfigurationIds) {
                authDetails.add(new AuthorizationDetails("openid_credential", configId, null));
            }
        }
        String authDetailsJson = gson.toJson(authDetails);
        String encodedAuthDetails = null;
        try {
            encodedAuthDetails = URLEncoder.encode(authDetailsJson, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(TAG, "authorization_details URL encoding failed", e);
            finishWithError("URL encoding failed.");
            return;
        }

        // generatePkceValues
        generatePkceValues();
        Uri.Builder authUriBuilder = Uri.parse(authorizationEndpoint +"/oauth2/authorize").buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("authorization_details", encodedAuthDetails)
                // pkce
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256"); //sha256

        if (issuerState != null && !issuerState.isEmpty()) {
            authUriBuilder.appendQueryParameter("state", issuerState);
        }

        String finalAuthorizationUrl = authUriBuilder.build().toString();
        Log.d(TAG, "Final Authorization URL: " + finalAuthorizationUrl);

        // Execute Chrome Custom Tabs
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(finalAuthorizationUrl));
    }


    private void handleAuthorizationResponse(Uri uri) {

        if (uri != null && uri.toString().startsWith("org.omnione.did.sdk.oid4vc://callback")) {
            Log.d(TAG, "Authorization code response: " + uri.toString());
            String code = uri.getQueryParameter("code");
            String receivedState = uri.getQueryParameter("state");

            if (this.issuerState == null || !this.issuerState.equals(receivedState)) {
                Log.e(TAG, "State mismatch: Possible CSRF attack. Expected: " + this.issuerState + ", Received: " + receivedState);
                finishWithError("Security error: State mismatch.");
                return;
            }

            if (code != null) {
                Log.d(TAG, "Authorization code received: " + code);
                exchangeCodeForToken(code);
            } else {
                String error = uri.getQueryParameter("error");
                String errorDescription = uri.getQueryParameter("error_description");
                Log.e(TAG, "Authorization request error: " + error + ", " + errorDescription);
                finishWithError("Authorization request error: " + (errorDescription != null ? errorDescription : error));
            }
        }
    }

    private void exchangeCodeForToken(String code) {
        Log.d(TAG, "Exchanging authorization code for token...");
        Log.d(TAG, "issuerUrl : " + issuerUrl);
        Log.d(TAG, "tokenEndpoint : " + tokenEndpoint);
        Log.d(TAG, "clientId : " + clientId);
        Log.d(TAG, "credentialConfigurationIds : " + credentialConfigurationIds);
        if (issuerUrl == null || tokenEndpoint == null || clientId == null || credentialConfigurationIds == null) {
            Log.e(TAG, "Missing data required for token exchange.");
            finishWithError("Insufficient data for token exchange.");
            return;
        }

        ApiService tokenApiService = createApiService(tokenEndpoint);

        tokenApiService.getTokenByAuthCode(
                null,
                "authorization_code",
                code,
                codeVerifier,
                "org.omnione.did.sdk.oid4vc://callback",
                clientId
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String tokenResponseJson = response.body().string();
                        Log.d(TAG, "Token response: " + tokenResponseJson);
                        TokenResponse tokenResponse = gson.fromJson(tokenResponseJson, TokenResponse.class);
                        String accessToken = tokenResponse.getAccessToken();

                        if (accessToken != null) {
                            Log.d(TAG, "Access Token obtained successfully.");
                            finishWithSuccess(accessToken, tokenResponse);
                        } else {
                            finishWithError("Access Token not in response.");
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Failed to parse token response", e);
                        finishWithError("Failed to parse token response: " + e.getMessage());
                    }
                } else {
                    String errorBody = "N/A";
                    try {
                        if (response.errorBody() != null)
                            errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e(TAG, "Token exchange failed (Code: " + response.code() + ", Error: " + errorBody + ")");
                    finishWithError("Token exchange failed (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Token request failed", t);
                finishWithError("Token request failed: " + t.getMessage());
            }
        });
    }

    // Pass results to CredentialIssuanceActivity
    private void finishWithSuccess(String accessToken, TokenResponse tokenResponse) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("AUTH_CODE_FLOW_RESULT", "SUCCESS");
        resultIntent.putExtra("ACCESS_TOKEN", accessToken);
        resultIntent.putExtra("TOKEN_RESPONSE_JSON", gson.toJson(tokenResponse));
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void finishWithError(String errorMessage) {
        Log.e(TAG, "Authorization Flow error: " + errorMessage);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("AUTH_CODE_FLOW_RESULT", "FAILURE");
        resultIntent.putExtra("ERROR_MESSAGE", errorMessage);
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    private ApiService createApiService(String baseUrl) {
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
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
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit.create(ApiService.class);
    }

    private void generatePkceValues() {

        byte[] verifierBytes = new byte[32];
        new SecureRandom().nextBytes(verifierBytes);

        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = messageDigest.digest(codeVerifier.getBytes());
        codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

    }
}