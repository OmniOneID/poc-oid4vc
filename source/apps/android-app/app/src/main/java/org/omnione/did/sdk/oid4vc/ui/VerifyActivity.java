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
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;

import org.omnione.did.sdk.oid4vc.R;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.format.Mdoc;
import org.omnione.did.sdk.oid4vc.format.OpenDid;
import org.omnione.did.sdk.oid4vc.format.SdjwtVc;
import org.omnione.did.sdk.oid4vc.network.ApiService;
import org.omnione.did.sdk.oid4vc.util.CryptoUtil;
import org.omnione.did.sdk.oid4vc.util.LogUtil;
import org.omnione.did.sdk.oid4vc.util.WalletUtil;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VerifyActivity extends AppCompatActivity {

    private TextView infoTextView;
    private String authorizationRequestState;
    private String nonce;
    private String aud;
    private String responseUri;
    private List<String> selectedClaimsKeys;
    private List<String> selectedClaimsNamespaces;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        infoTextView = findViewById(R.id.info);

        Intent intent = getIntent();
        handleIntent(intent);
    }

    /**
     * This is called for activities that set launchMode to "singleTop" in their manifest, or if a client used the Intent.FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
     *
     * @param intent The new intent that was started for the activity.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Processes the incoming intent and extracts deep link data.
     *
     * @param intent The intent to handle.
     */
    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();

        selectedClaimsKeys = intent.getStringArrayListExtra("selected_claims_keys");
        selectedClaimsNamespaces = intent.getStringArrayListExtra("selected_claims_namespaces");

        if (uri != null) {
            String requestUri = uri.getQueryParameter("request_uri");

            if (requestUri != null) {
                String displayText = "OID4VP Request URI: " + requestUri;
                infoTextView.setText(displayText);
                step1_fetchAuthorizationRequest(requestUri);
            } else {
                infoTextView.setText("Error: request_uri not found.");
            }
        } else {
            infoTextView.setText("Error: Deep link data not found.");
        }
    }

    /**
     * Fetches the Authorization Request Object from the Verifier.
     *
     * @param requestUrl The URL from which to fetch the authorization request.
     */
    private void step1_fetchAuthorizationRequest(String requestUrl) {
        statusUpdate("Fetching Authorization Request...");
        ApiService apiService = getApiService(requestUrl);
        Call<ResponseBody> call = apiService.getRequest(requestUrl);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        step2_processRequestAndGenerateVp(responseData);
                    } catch (Exception e) {
                        infoTextView.setText("Error: A problem occurred while processing the response data.");
                    }
                } else {
                    handleRequestError(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(VerifyActivity.this, "Network error occurred", Toast.LENGTH_SHORT).show();
                infoTextView.setText("Error: Please check your network connection.");
            }
        });
    }

    /**
     * Parses the authorization request, extracts metadata, and initiates VP token generation.
     *
     * @param responseData The raw response data containing the authorization request.
     * @throws Exception If an error occurs during processing.
     */
    private void step2_processRequestAndGenerateVp(String responseData) throws Exception {
        statusUpdate("Processing Request and Generating VP...");
        String responseDataString = CryptoUtil.parsePayload(responseData);

        responseUri = LogUtil.extractJsonField(responseDataString, "response_uri");
        authorizationRequestState = LogUtil.extractJsonField(responseDataString, "state");
        nonce = LogUtil.extractJsonField(responseDataString, "nonce");
        aud = LogUtil.extractJsonField(responseDataString, "client_id");
        String responseMode = LogUtil.extractJsonField(responseDataString, "response_mode");

        if (responseUri == null || authorizationRequestState == null || nonce == null || aud == null || responseMode == null) {
            infoTextView.setText("Error: Required fields not found in the response.");
            return;
        }

        String vpToken = generateVpToken();

        step3_submitVpToken(responseUri, vpToken, authorizationRequestState);
    }

    /**
     * Submits the generated VP token to the verifier's response URI.
     *
     * @param responseUri The URI to which the VP token should be submitted.
     * @param vpToken The generated VP token.
     * @param state The state associated with the authorization request.
     */
    private void step3_submitVpToken(String responseUri, String vpToken, String state) {
        statusUpdate("Submitting VP Token to Verifier...");
        ApiService apiService = getApiService(responseUri);

        apiService.postVpToken(responseUri, vpToken, state).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        infoTextView.setText("VC submission complete! : " + responseBody);
                    } catch (IOException e) {
                    }
                    Toast.makeText(VerifyActivity.this, "VC submission successful!", Toast.LENGTH_LONG).show();
                } else {
                    handleRequestError(response);
                    Toast.makeText(VerifyActivity.this, "VC submission failed!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                infoTextView.setText("Error: VP Token submission network error.");
                Toast.makeText(VerifyActivity.this, "VP Token submission network error!", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Generates a VP token based on the stored VC and requested format.
     *
     * @return The generated VP token as a string.
     * @throws Exception If an error occurs during VP token creation.
     */
    private String generateVpToken() throws Exception {
        List<WalletData> walletDataList = WalletUtil.loadVcFileAsVcItem(this);
        if (walletDataList.isEmpty()) return "";
        WalletData walletData = walletDataList.get(0);
        String format = walletData.getFormat();
        String credential = (String) walletData.getCredential();

        if (OpenDid.isSupported(format)) {
            return OpenDid.createVpToken(nonce, aud, credential);
        } else if (SdjwtVc.isSupported(format)) {
            return SdjwtVc.createVpToken(this, credential, selectedClaimsKeys, aud, nonce);
        } else if (Mdoc.isSupported(format)) {
            return Mdoc.createVpToken(credential, selectedClaimsKeys, selectedClaimsNamespaces, aud, nonce, responseUri);
//            return Mdoc.createVpToken2(this, credential, selectedClaimsKeys, selectedClaimsNamespaces, aud, nonce, responseUri);
        }
        return "";
    }

    /**
     * Creates and configures an ApiService instance for network requests.
     *
     * @param baseUrl The base URL for the API service.
     * @return A configured ApiService instance.
     */
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

    /**
     * Updates the status message displayed in the UI.
     *
     * @param message The status message to display.
     */
    private void statusUpdate(String message) {
        infoTextView.setText(message);
    }

    /**
     * Handles errors that occur during network requests.
     *
     * @param response The response object containing error details.
     */
    private void handleRequestError(Response<ResponseBody> response) {
        String errorBody = "";
        try {
            if (response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (IOException e) {
        }
        infoTextView.setText("Error: Request failed (Code: " + response.code() + ")");
    }
}
