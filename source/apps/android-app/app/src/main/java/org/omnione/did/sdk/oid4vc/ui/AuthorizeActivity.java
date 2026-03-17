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
import org.omnione.did.sdk.oid4vc.util.CryptoUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    private String authorizationEndpoint;
    private String issuerUrl;
    private String issuerState;
    private List<String> credentialConfigurationIds;
    private String clientId;
    private String tokenEndpoint;

    private String codeChallenge;
    private String codeVerifier;

    private Gson gson;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
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
                    finishWithError("Insufficient data to start Authorization Flow.");
                    return;
                }

                startChromeCustomTabs(authorizationEndpoint, issuerState, credentialConfigurationIds, clientId);
            }
        } else {
            finishWithError("Abnormal Authorization Flow access.");
        }
    }

    /**
     * This is called for activities that set launchMode to "singleTop" in their manifest, or if a client used the Intent.FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
     *
     * @param intent The new intent that was started for the activity.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthorizationResponse(intent.getData());
    }

    /**
     * Constructs the authorization URL and launches Chrome Custom Tabs.
     *
     * @param authorizationEndpoint The authorization endpoint.
     * @param issuerState The state provided by the issuer.
     * @param credentialConfigurationIds List of requested credential configuration IDs.
     * @param clientId The client identifier.
     */
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
            finishWithError("URL encoding failed.");
            return;
        }

        generatePkceValues();
        Uri.Builder authUriBuilder = Uri.parse(authorizationEndpoint +"/oauth2/authorize").buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("authorization_details", encodedAuthDetails)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256");

        if (issuerState != null && !issuerState.isEmpty()) {
            authUriBuilder.appendQueryParameter("state", issuerState);
        }

        String finalAuthorizationUrl = authUriBuilder.build().toString();

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(finalAuthorizationUrl));
    }

    /**
     * Processes the authorization response from the deep link URI.
     *
     * @param uri The deep link URI containing authorization response parameters.
     */
    private void handleAuthorizationResponse(Uri uri) {

        if (uri != null && uri.toString().startsWith("org.omnione.did.sdk.oid4vc://callback")) {
            String code = uri.getQueryParameter("code");
            String receivedState = uri.getQueryParameter("state");

            if (this.issuerState == null || !this.issuerState.equals(receivedState)) {
                finishWithError("Security error: State mismatch.");
                return;
            }

            if (code != null) {
                exchangeCodeForToken(code);
            } else {
                String error = uri.getQueryParameter("error");
                String errorDescription = uri.getQueryParameter("error_description");
                finishWithError("Authorization request error: " + (errorDescription != null ? errorDescription : error));
            }
        }
    }

    /**
     * Exchanges the authorization code for an access token.
     *
     * @param code The authorization code.
     */
    private void exchangeCodeForToken(String code) {
        if (issuerUrl == null || tokenEndpoint == null || clientId == null || credentialConfigurationIds == null) {
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
                        TokenResponse tokenResponse = gson.fromJson(tokenResponseJson, TokenResponse.class);
                        String accessToken = tokenResponse.getAccessToken();

                        if (accessToken != null) {
                            finishWithSuccess(accessToken, tokenResponse);
                        } else {
                            finishWithError("Access Token not in response.");
                        }

                    } catch (IOException e) {
                        finishWithError("Failed to parse token response: " + e.getMessage());
                    }
                } else {
                    finishWithError("Token exchange failed (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                finishWithError("Token request failed: " + t.getMessage());
            }
        });
    }

    /**
     * Finishes the activity with a success result.
     *
     * @param accessToken The obtained access token.
     * @param tokenResponse The complete token response.
     */
    private void finishWithSuccess(String accessToken, TokenResponse tokenResponse) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("AUTH_CODE_FLOW_RESULT", "SUCCESS");
        resultIntent.putExtra("ACCESS_TOKEN", accessToken);
        resultIntent.putExtra("TOKEN_RESPONSE_JSON", gson.toJson(tokenResponse));
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Finishes the activity with an error result.
     *
     * @param errorMessage The error message explaining the failure.
     */
    private void finishWithError(String errorMessage) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("AUTH_CODE_FLOW_RESULT", "FAILURE");
        resultIntent.putExtra("ERROR_MESSAGE", errorMessage);
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    /**
     * Creates and configures an ApiService instance for network requests.
     *
     * @param baseUrl The base URL for the API service.
     * @return A configured ApiService instance.
     */
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

    /**
     * Generates PKCE (Proof Key for Code Exchange) values for the authorization flow.
     */
    private void generatePkceValues() {
        CryptoUtil.PkceValues pkceValues = CryptoUtil.generatePkceValues();
        codeVerifier = pkceValues.codeVerifier;
        codeChallenge = pkceValues.codeChallenge;
    }
}
