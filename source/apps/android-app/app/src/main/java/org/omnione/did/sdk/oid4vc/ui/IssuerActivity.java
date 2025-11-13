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

import org.omnione.did.sdk.oid4vc.R;

public class IssuerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issuer);
        Intent intent = getIntent();
        // Receiver for credential offer deeplink
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

            String requestUri = uri.getQueryParameter("credential_offer_uri");
            Log.d("sangjun", "credential offer_uri from deeplink : " + requestUri);

            if (requestUri != null) {
                Log.d("sangjun","OID4VCI Request URI: " + uri);
                Intent intent2 = new Intent(IssuerActivity.this, CredentialIssuanceActivity.class);
                intent2.putExtra("CREDENTIAL_OFFER_URI", uri.toString());
                startActivity(intent2);
            } else {
                Log.d("sangjun","Error: request_uri not found.");
            }
        } else {
            Log.d("sangjun","Error: Deep link data not found.");
        }
        finish();
    }
}