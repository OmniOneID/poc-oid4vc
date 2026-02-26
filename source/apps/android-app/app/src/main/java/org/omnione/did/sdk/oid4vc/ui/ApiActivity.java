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
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.omnione.did.sdk.oid4vc.data.dto.AuthorizationDetails;
import org.omnione.did.sdk.oid4vc.network.ApiService;
import org.omnione.did.sdk.oid4vc.data.dto.CredentialOfferRequest;
import org.omnione.did.sdk.oid4vc.data.dto.CredentialRequest;
import org.omnione.did.sdk.oid4vc.data.dto.Proofs;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.omnione.did.sdk.oid4vc.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApiActivity extends AppCompatActivity {

    private EditText issuerUrlEditText, tokenUrlEditText, preAuthCodeEditText, txCodeEditText, accessTokenEditText;
    private Button buttonCredentialOffer, buttonIssuer, buttonCredentials, buttonAuthorize, buttonToken;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        issuerUrlEditText = findViewById(R.id.issuerUrlEditText);
        tokenUrlEditText = findViewById(R.id.tokenUrlEditText);
        buttonCredentialOffer = findViewById(R.id.buttonCredentialOffer);
        buttonIssuer = findViewById(R.id.buttonIssuer);
        buttonToken = findViewById(R.id.buttonToken);
        buttonCredentials = findViewById(R.id.buttonCredentials);
        buttonAuthorize = findViewById(R.id.buttonAuthorize);

        preAuthCodeEditText = findViewById(R.id.preAuthCode);
        txCodeEditText = findViewById(R.id.txCode);
        accessTokenEditText = findViewById(R.id.accessToken);

        gson = new GsonBuilder().setPrettyPrinting().create();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SCANNED_URL")) {
            String scannedUrl = intent.getStringExtra("SCANNED_URL");
            issuerUrlEditText.setText(scannedUrl);
        }

        buttonCredentialOffer.setOnClickListener(v -> {
            CredentialOfferRequest requestBody = new CredentialOfferRequest("test1234");
            Call<ResponseBody> call = getApiService(issuerUrlEditText.getText().toString()).getCredentialOfferForTest();
            executeNetworkRequest(call, gson.toJson(requestBody));
        });

        buttonIssuer.setOnClickListener(v -> {
            Call<ResponseBody> call = getApiService(issuerUrlEditText.getText().toString()).getIssuerInfo();
            executeNetworkRequest(call, null);
        });

        buttonToken.setOnClickListener(v -> {
            String authorizationHeader = "Basic b2lkNHZjaS1jbGllbnQ6c2VjcmV0";
            String grantType = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
            String preAuthorizedCode = preAuthCodeEditText.getText().toString();  //todo : manual input?
            String txCode = txCodeEditText.getText().toString(); //todo : manual input?
            AuthorizationDetails authDetails = new AuthorizationDetails("openid_credential", "TEC", null);
            List<AuthorizationDetails> authDetailsList = new ArrayList<>();
            authDetailsList.add(authDetails);
            String authorizationDetailsJson = gson.toJson(authDetailsList);

            Call<ResponseBody> call = getApiService(tokenUrlEditText.getText().toString()).getTokenByPreAuthCode(authorizationHeader, grantType, preAuthorizedCode, txCode, authorizationDetailsJson);
            String requestBodyString = "grant_type=" + grantType +
                    "&pre-authorized_code=" + preAuthorizedCode +
                    "&tx_code=" + txCode +
                    "&authorization_details=" + authorizationDetailsJson;

            executeNetworkRequest(call, requestBodyString);
        });

        buttonCredentials.setOnClickListener(v -> {
            String authorizationHeader = "Bearer " + accessTokenEditText.getText().toString();
            CredentialRequest requestBody = createAndUseCredentialRequest();
            Call<ResponseBody> call = getApiService(issuerUrlEditText.getText().toString()).getCredential(authorizationHeader, requestBody);
            executeNetworkRequest(call, gson.toJson(requestBody));
        });

        buttonAuthorize.setOnClickListener(v -> {
            Call<ResponseBody> call = getApiService(tokenUrlEditText.getText().toString()).getAuthorize();
            executeNetworkRequest(call, null);
        });

    }

    private ApiService getApiService(String url) {
        String baseUrl = url;
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
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit.create(ApiService.class);
    }

    private void executeNetworkRequest(Call<ResponseBody> call, String requestBodyString) {
        String requestUrl = call.request().url().toString();
        String httpMethod = call.request().method();
        StringBuilder requestInfo = new StringBuilder();
        requestInfo.append("Method: ").append(httpMethod).append("\n");
        requestInfo.append("URL: ").append(requestUrl);

        if (call.request().header("Authorization") != null) {
            requestInfo.append("\n\n--- Headers ---\n");
            requestInfo.append("Authorization: ").append(call.request().header("Authorization"));
        }

        if (requestBodyString != null) {
            requestInfo.append("\n\n--- Request Body ---\n").append(requestBodyString);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String responseData;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        Object jsonObject = gson.fromJson(responseString, Object.class);
                        responseData = gson.toJson(jsonObject);
                    } catch (IOException e) {
                        responseData = "Error reading response body: " + e.getMessage();
                    }
                } else {
                    responseData = "Error Code: " + response.code() + "\nMessage: " + response.message();
                }
                startResultActivity(requestInfo.toString(), responseData);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String errorData = "Error: " + t.getMessage();
                Log.e("sangjun", "Network request failed", t);
                Toast.makeText(ApiActivity.this, "Network error occurred", Toast.LENGTH_SHORT).show();
                startResultActivity(requestInfo.toString(), errorData);
            }
        });
    }

    private void startResultActivity(String request, String response) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("REQUEST_DATA", request);
        intent.putExtra("RESPONSE_DATA", response);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private CredentialRequest createAndUseCredentialRequest() {
        String exampleJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM0MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        Proofs proof = new Proofs();
        proof.setJwt(List.of(exampleJwt));
        String credentialIdentifier = "NationalID";
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialIdentifier(credentialIdentifier);
        credentialRequest.setProofs(proof);
        return credentialRequest;
    }
}