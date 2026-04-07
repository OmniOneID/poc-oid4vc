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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.omnione.did.sdk.mdoc.proximity.holder.ble.BleMode;
import org.omnione.did.sdk.mdoc.proximity.holder.ble.BleModePreferences;
import org.omnione.did.sdk.oid4vc.R;

public class MainActivity extends AppCompatActivity {

    private Button buttonIssue, buttonViewVc, buttonApiTest, buttonDebugAction;

    private EditText debugInputEditText;
    private TextView bleModeValueTextView;
    private SwitchCompat bleModeSwitch;
    private Gson gson;

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Scan canceled.", Toast.LENGTH_LONG).show();
                } else {
                    String scannedUriString = result.getContents();

                    Intent intent = new Intent(MainActivity.this, CredentialIssuanceActivity.class);
                    intent.putExtra("CREDENTIAL_OFFER_URI", scannedUriString);
                    startActivity(intent);
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
        setContentView(R.layout.activity_main);

        buttonIssue = findViewById(R.id.buttonIssue);
        buttonViewVc = findViewById(R.id.buttonViewVc);
        buttonApiTest = findViewById(R.id.buttonApiTest);
        bleModeValueTextView = findViewById(R.id.bleModeValueTextView);
        bleModeSwitch = findViewById(R.id.bleModeSwitch);

        buttonDebugAction = findViewById(R.id.debugActionButton);
        debugInputEditText = findViewById(R.id.debugInputEditText);

        gson = new GsonBuilder().create();

        buttonIssue.setOnClickListener(v -> {
            launchQrScanner();
        });

        buttonViewVc.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewVcActivity.class);
            startActivity(intent);
        });

        buttonApiTest.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApiActivity.class);
            startActivity(intent);
        });

        buttonDebugAction.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CredentialIssuanceActivity.class);
            intent.putExtra("CREDENTIAL_OFFER_URI", debugInputEditText.getText().toString());
            startActivity(intent);
            debugInputEditText.setText("");
        });

        bleModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            BleMode mode = isChecked ? BleMode.CENTRAL_CLIENT : BleMode.PERIPHERAL_SERVER;
            BleModePreferences.setMode(this, mode);
            updateBleModeSummary();
        });

        updateBleModeSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBleModeSummary();
    }

    /**
     * Configures and launches the QR code scanner.
     */
    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(true);
        options.setPrompt("Please scan the QR code.");
        options.setBeepEnabled(false);
        options.setCaptureActivity(QrActivity.class);
        qrCodeLauncher.launch(options);
    }

    private void updateBleModeSummary() {
        BleMode mode = BleModePreferences.getMode(this);
        bleModeSwitch.setChecked(mode == BleMode.CENTRAL_CLIENT);
        bleModeValueTextView.setText(mode == BleMode.CENTRAL_CLIENT ? "Central Client" : "Peripheral Server");
    }
}