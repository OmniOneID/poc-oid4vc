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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import org.omnione.did.sdk.oid4vc.data.dto.AuthorizationDetails;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.network.ApiService;
import org.omnione.did.sdk.oid4vc.data.dto.CredentialOfferResponse;
import org.omnione.did.sdk.oid4vc.data.dto.CredentialRequest;
import org.omnione.did.sdk.oid4vc.data.dto.CredentialResponse;
import org.omnione.did.sdk.oid4vc.data.dto.IssuerMetadataResponse;
import org.omnione.did.sdk.oid4vc.data.dto.Proofs;
import org.omnione.did.sdk.oid4vc.data.dto.TokenResponse;
import org.omnione.did.sdk.oid4vc.util.CryptoUtil;
import org.omnione.did.sdk.oid4vc.util.WalletUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.omnione.did.sdk.oid4vc.R;

public class CredentialIssuanceActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView statusTextView;
    private ImageView resultImageView;
    private Button closeButton;

    private Gson gson = new Gson();
    private String preAuthCode = "";
    private String issuerState;
    private String issuerUrl;
    private String tokenEndpointUrl;
    private List<String> credentialConfigurationIds;
    private Map<String, IssuerMetadataResponse.CredentialConfiguration> issuerSupportedConfigurations;

    private final ActivityResultLauncher<Intent> pinActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String pinCode = result.getData().getStringExtra("PIN_CODE");
                    step2_getToken(pinCode);
                } else {
                    handleFailure("PIN entry canceled.");
                }
            });

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_issuance);

        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        resultImageView = findViewById(R.id.resultImageView);
        closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> finish());

        String offerUriString = getIntent().getStringExtra("CREDENTIAL_OFFER_URI");
        if (offerUriString == null) {
            handleFailure("Credential Offer URI not found.");
            return;
        }

        step0_fetchCredentialOffer(offerUriString);
    }

    /**
     * Fetches and parses the Credential Offer from the provided URI.
     *
     * @param offerUriString The URI string of the Credential Offer.
     */
    private void step0_fetchCredentialOffer(String offerUriString) {
        statusTextView.setText("Verifying Credential Offer...");

        String credentialOfferUrl;
        try {
            Uri uri = Uri.parse(offerUriString);
            String encodedUrl = uri.getQueryParameter("credential_offer_uri");
            if (encodedUrl == null) {
                handleFailure("Could not find credential_offer_uri in URI.");
                return;
            }
            credentialOfferUrl = URLDecoder.decode(encodedUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            handleFailure("Invalid Credential Offer URI format.");
            return;
        }

        ApiService apiService = createApiService(credentialOfferUrl);
        apiService.getRequest(credentialOfferUrl).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String credentialOffer = response.body().string();
                        CredentialOfferResponse offer = gson.fromJson(credentialOffer, CredentialOfferResponse.class);
                        issuerUrl = offer.getCredentialIssuer();
                        credentialConfigurationIds = offer.getCredentialConfigurationIds();
                        if (credentialConfigurationIds == null || credentialConfigurationIds.isEmpty()) {
                            handleFailure("No issuable Credential ID in Offer.");
                            return;
                        }

                        if (offer.getGrants().getPreAuthorizedCodeGrant() != null) {
                            preAuthCode = offer.getGrants().getPreAuthorizedCodeGrant().getPreAuthorizedCode();

                            if (preAuthCode == null || preAuthCode.isEmpty()) {
                                handleFailure("Pre-Authorized Code not in Offer.");
                                return;
                            }

                        } else if (offer.getGrants().getAuthorizationCodeGrant() != null) {
                            issuerState = offer.getGrants().getAuthorizationCodeGrant().getIssuerState();

                        } else {
                            handleFailure("Unsupported Grant type or missing Grants information.");
                        }
                        step1_getIssuerInfo();
                    } catch (IOException e) {
                        handleFailure("Failed to parse Credential Offer response.");
                    }
                } else {
                    handleFailure("Failed to fetch Credential Offer (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleFailure("Credential Offer request failed: " + t.getMessage());
            }
        });
    }

    /**
     * Fetches metadata from the Credential Issuer.
     */
    private void step1_getIssuerInfo() {
        statusTextView.setText("Fetching issuer information...");

        ApiService apiService = createApiService(issuerUrl);
        apiService.getIssuerInfo().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        IssuerMetadataResponse info = gson.fromJson(responseString, IssuerMetadataResponse.class);

                        if (info.getAuthorizationServers() != null && !info.getAuthorizationServers().isEmpty()) {
                            tokenEndpointUrl = info.getAuthorizationServers().get(0);
                        } else {
                            handleFailure("'authorization_servers' information not in response.");
                            tokenEndpointUrl = issuerUrl;
                        }

                        issuerSupportedConfigurations = info.getCredentialConfigurationsSupported();
                        
                        if(preAuthCode.isEmpty()) {
                            startAuthorizationCodeFlow(info.getAuthorizationServers().get(0), issuerState, credentialConfigurationIds);
                        } else {
                            Intent intent = new Intent(CredentialIssuanceActivity.this, PinActivity.class);
                            pinActivityLauncher.launch(intent);
                        }

                    } catch (IOException e) {
                        handleFailure("Failed to parse issuer information: " + e.getMessage());
                    }
                } else {
                    handleFailure("Failed to fetch issuer information (Code: " + response.code() + ")");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleFailure("Issuer information request failed: " + t.getMessage());
            }
        });
    }

    private AlertDialog selectionDialog;

    /**
     * Displays a dialog for the user to select a credential from the available options.
     *
     * @param authorizationDetails List of authorization details representing available credentials.
     * @param accessToken The access token for credential request.
     * @param tokenResponseAuthDetails List of authorization details from the token response.
     */
    private void showCredentialSelectionDialog(
            List<AuthorizationDetails> authorizationDetails,
            String accessToken,
            List<AuthorizationDetails> tokenResponseAuthDetails) {

        if (authorizationDetails == null || authorizationDetails.isEmpty()) {
            handleFailure("No Credential information to select.");
            return;
        }

        if (selectionDialog != null && selectionDialog.isShowing()) {
            selectionDialog.dismiss();
        }

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_credential_selection, null);
        LinearLayout container = dialogView.findViewById(R.id.credentialOptionsContainer);

        List<RadioGroup> radioGroups = new ArrayList<>();

        for (AuthorizationDetails group : authorizationDetails) {
            if(group.getCredentialIdentifiers().size() > 0) {
                TextView groupTitle = new TextView(this);
                groupTitle.setText(group.getCredentialConfigurationId());
                groupTitle.setTextSize(18f);
                groupTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                groupTitle.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                titleParams.setMargins(0, 24, 0, 8);
                groupTitle.setLayoutParams(titleParams);
                container.addView(groupTitle);

                RadioGroup radioGroup = new RadioGroup(this);
                for (String identifier : group.getCredentialIdentifiers()) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setText(identifier);
                    radioButton.setTextSize(16f);
                    radioButton.setId(View.generateViewId());
                    radioGroup.addView(radioButton);
                }
                radioGroups.add(radioGroup);
                container.addView(radioGroup);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", null);

        selectionDialog = builder.create();

        selectionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                final Button okButton = selectionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                final Button cancelButton = selectionDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        handleFailure("Selection canceled.");
                        selectionDialog.dismiss();
                    }
                });

                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String selected = null;
                        for (RadioGroup rg : radioGroups) {
                            int selectedId = rg.getCheckedRadioButtonId();
                            if (selectedId != -1) {
                                RadioButton rb = rg.findViewById(selectedId);
                                selected = String.valueOf(rb.getText());
                                break;
                            }
                        }

                        if (selected == null) {
                            Toast.makeText(CredentialIssuanceActivity.this, "Please select an item.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final String selectedFinal = selected;

                        okButton.setEnabled(false);
                        selectionDialog.dismiss();
                        step3_getCredential(accessToken, tokenResponseAuthDetails, selectedFinal);
                    }
                });
            }
        });
        selectionDialog.show();
    }

    /**
     * Requests an access token using the pre-authorized code and provided PIN.
     *
     * @param pinCode The user-entered PIN.
     */
    private void step2_getToken(String pinCode) {
        statusTextView.setText("Issuing token...");

        ApiService tokenApiService = createApiService(tokenEndpointUrl);
        String authorizationHeader = "Basic b2lkNHZjaS1jbGllbnQ6c2VjcmV0";
        String grantType = "urn:ietf:params:oauth:grant-type:pre-authorized_code";

        List<AuthorizationDetails> authDetailsList = new ArrayList<>();
        for(String credentialConfigurationId : credentialConfigurationIds) {
            AuthorizationDetails authDetails = new AuthorizationDetails("openid_credential", credentialConfigurationId, null);
            authDetailsList.add(authDetails);
        }

        String authorizationDetailsJson = gson.toJson(authDetailsList);

        tokenApiService.getTokenByPreAuthCode(authorizationHeader, grantType, preAuthCode, pinCode, authorizationDetailsJson).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        TokenResponse tokenResponse = gson.fromJson(responseString, TokenResponse.class);
                        String accessToken = tokenResponse.getAccessToken();

                        if (accessToken == null || accessToken.isEmpty()) {
                            handleFailure("Could not find Access Token in response.");
                            return;
                        }

                        String authHeaderValue = "Bearer " + accessToken;

                        List<AuthorizationDetails> responseAuthDetails = tokenResponse.getAuthorizationDetails();
                        if (responseAuthDetails != null && !responseAuthDetails.isEmpty()) {
                            showCredentialSelectionDialog(responseAuthDetails, authHeaderValue, tokenResponse.getAuthorizationDetails());
                        }
                    } catch (IOException e) {
                        handleFailure("Failed to parse token response.");
                    }
                } else {
                    handleFailure("Token issuance failed (Code: " + response.code() + ")");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleFailure("Token request failed: " + t.getMessage());
            }
        });
    }

    /**
     * Requests the actual credential from the issuer using the access token.
     *
     * @param accessToken The access token for the credential request.
     * @param tokenResponseAuthDetails List of authorization details from the token response.
     * @param selectedCredentialIdentifiers The identifier of the selected credential.
     */
    private void step3_getCredential(String accessToken, List<AuthorizationDetails> tokenResponseAuthDetails, String selectedCredentialIdentifiers) {
        statusTextView.setText("Requesting Credential...");
        ApiService issuerApiService = createApiService(issuerUrl);

        CredentialRequest credentialRequest;

        if (tokenResponseAuthDetails != null && !tokenResponseAuthDetails.isEmpty()) {
            credentialRequest = createCredentialRequestWithIdentifier(selectedCredentialIdentifiers);
        } else {
            if (issuerSupportedConfigurations != null && !issuerSupportedConfigurations.isEmpty()) {
                IssuerMetadataResponse.CredentialConfiguration config = issuerSupportedConfigurations.get(credentialConfigurationIds.get(0));
                if (config != null && config.getFormat() != null) {
                    credentialRequest = createCredentialRequestWithIds(credentialConfigurationIds.get(0));
                } else {
                    handleFailure("Could not find format information for '" + credentialConfigurationIds.get(0) + "'.");
                    return;
                }
            } else {
                handleFailure("Insufficient information for Credential request.");
                return;
            }
        }

        issuerApiService.getCredential(accessToken, credentialRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseJson = response.body().string();
                        CredentialResponse credentialResponse = gson.fromJson(responseJson, CredentialResponse.class);
                        WalletData walletData = new WalletData();
                        walletData.setCredential(credentialResponse.getCredentials().get(0).getCredential());
                        walletData.setFormat(selectedCredentialIdentifiers);

                        if (walletData.getCredential() != null) {
                            JsonArray jsonArrayToSave = new JsonArray();
                            jsonArrayToSave.add(gson.toJsonTree(walletData));
                            WalletUtil.saveStringToFile(CredentialIssuanceActivity.this, gson.toJson(jsonArrayToSave), "vc.json");
                            handleSuccess();
                        } else {
                            handleFailure("Invalid credential response format.");
                        }

                    } catch (IOException e) {
                        handleFailure("Failed to save response.");
                    }
                } else {
                    handleFailure("Credential issuance failed (Code: " + response.code() + ")");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleFailure("Credential request failed: " + t.getMessage());
            }
        });
    }

    /**
     * Creates and configures an ApiService instance for network requests.
     *
     * @param baseUrl The base URL for the API service.
     * @return A configured ApiService instance.
     */
    private ApiService createApiService(String baseUrl) {
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

    /**
     * Handles the successful completion of the credential issuance process.
     */
    private void handleSuccess() {
        progressBar.setVisibility(View.GONE);
        resultImageView.setVisibility(View.VISIBLE);
        resultImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_success));
        statusTextView.setText("Credential issued successfully!");
        closeButton.setVisibility(View.VISIBLE);
    }

    /**
     * Handles failures during the credential issuance process.
     *
     * @param message The error message to display.
     */
    private void handleFailure(String message) {
        progressBar.setVisibility(View.GONE);
        resultImageView.setVisibility(View.VISIBLE);
        resultImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_failure));
        statusTextView.setText("Error: " + message);
        closeButton.setVisibility(View.VISIBLE);
    }

    /**
     * Creates a CredentialRequest object with a specific identifier and proof.
     *
     * @param identifier The credential identifier.
     * @return A CredentialRequest object.
     */
    private CredentialRequest createCredentialRequestWithIdentifier(String identifier) {
        String exampleJwtProof = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        Proofs proofs = new Proofs();
        proofs.setJwt(List.of(exampleJwtProof));

        if(identifier.equals("NationalIDCert") || identifier.equals("mDL")) {
            exampleJwtProof = CryptoUtil.generateJws(this, "did:omn:holder", "1234567890", "Raon Kim");
            proofs.setJwt(List.of(exampleJwtProof));
        }
        CredentialRequest request = new CredentialRequest();
        request.setCredentialIdentifier(identifier);
        request.setProofs(proofs);
        return request;
    }

    /**
     * Creates a CredentialRequest object with a configuration ID and proof.
     *
     * @param id The credential configuration ID.
     * @return A CredentialRequest object.
     */
    private CredentialRequest createCredentialRequestWithIds(String id) {
        String exampleJwtProof = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        Proofs proof = new Proofs();
        proof.setJwt(List.of(exampleJwtProof));

        CredentialRequest request = new CredentialRequest();
        request.setCredentialConfigurationId(id);
        request.setProofs(proof);
        return request;
    }

    /**
     * Starts the Authorization Code Flow for credential issuance.
     *
     * @param authorizationEndpoint The authorization server's endpoint.
     * @param issuerState The state provided by the issuer.
     * @param credentialConfigurationIds List of requested credential configuration IDs.
     */
    private void startAuthorizationCodeFlow(String authorizationEndpoint, String issuerState, List<String> credentialConfigurationIds) {
        statusTextView.setText("Redirecting to authorization server...");

        Intent authorizeIntent = new Intent(CredentialIssuanceActivity.this, AuthorizeActivity.class);
        authorizeIntent.putExtra("AUTHORIZATION_ENDPOINT", authorizationEndpoint);
        authorizeIntent.putExtra("ISSUER_URL", issuerUrl);
        authorizeIntent.putExtra("ISSUER_STATE", issuerState);
        authorizeIntent.putStringArrayListExtra("CREDENTIAL_CONFIG_IDS", new ArrayList<>(credentialConfigurationIds));
        authorizeIntent.putExtra("CLIENT_ID", "oid4vci-android");
        authorizeIntent.putExtra("TOKEN_ENDPOINT", authorizationEndpoint);

        authFlowLauncher.launch(authorizeIntent);

    }

    private ActivityResultLauncher<Intent> authFlowLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String flowResult = data.getStringExtra("AUTH_CODE_FLOW_RESULT");
                        if ("SUCCESS".equals(flowResult)) {
                            String accessToken = data.getStringExtra("ACCESS_TOKEN");
                            String tokenResponseJson = data.getStringExtra("TOKEN_RESPONSE_JSON");
                            TokenResponse tokenResponse = gson.fromJson(tokenResponseJson, TokenResponse.class);

                            Toast.makeText(this, "Continuing with Credential issuance.", Toast.LENGTH_SHORT).show();

                            String authHeaderValue = "Bearer " + accessToken;

                            List<AuthorizationDetails> responseAuthDetails = tokenResponse.getAuthorizationDetails();
                            if (responseAuthDetails != null && !responseAuthDetails.isEmpty()) {
                                showCredentialSelectionDialog(responseAuthDetails, authHeaderValue, tokenResponse.getAuthorizationDetails());
                            }
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Intent data = result.getData();
                    String errorMessage = (data != null) ? data.getStringExtra("ERROR_MESSAGE") : "Unknown error";
                    handleFailure("Authorization Code Flow failed: " + errorMessage);
                } else {
                    handleFailure("Unknown result of Authorization Code Flow.");
                }
            });
}
