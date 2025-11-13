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
import android.widget.TextView;

import org.omnione.did.sdk.oid4vc.R;

public class ResultActivity extends AppCompatActivity {

    private TextView requestTextView;
    private TextView responseTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        requestTextView = findViewById(R.id.requestTextView);
        responseTextView = findViewById(R.id.responseTextView);

        Intent intent = getIntent();
        String requestData = intent.getStringExtra("REQUEST_DATA");
        String responseData = intent.getStringExtra("RESPONSE_DATA");

        requestTextView.setText(requestData);
        responseTextView.setText(responseData);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}