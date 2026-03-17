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

import org.omnione.did.sdk.oid4vc.R;

public class IssuerActivity extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issuer);
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
     * Processes the incoming intent and extracts the credential offer URI from the deep link.
     *
     * @param intent The intent to handle.
     */
    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();

        if (uri != null) {
            String requestUri = uri.getQueryParameter("credential_offer_uri");

            if (requestUri != null) {
                Intent intent2 = new Intent(IssuerActivity.this, CredentialIssuanceActivity.class);
                intent2.putExtra("CREDENTIAL_OFFER_URI", uri.toString());
                startActivity(intent2);
            }
        }
        finish();
    }
}