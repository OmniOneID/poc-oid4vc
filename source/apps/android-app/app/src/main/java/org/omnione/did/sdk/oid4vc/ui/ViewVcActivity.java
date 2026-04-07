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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.omnione.did.sdk.mdoc.proximity.holder.ble.BleMode;
import org.omnione.did.sdk.mdoc.proximity.holder.ble.BleModePreferences;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.data.dto.tec.VerifiableCredential;
import org.omnione.did.sdk.oid4vc.format.Mdoc;
import org.omnione.did.sdk.oid4vc.format.OpenDid;
import org.omnione.did.sdk.oid4vc.format.SdjwtVc;
import org.omnione.did.sdk.oid4vc.util.WalletUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.omnione.did.sdk.oid4vc.R;

public class ViewVcActivity extends AppCompatActivity {

    private LinearLayout claimsContainer;
    private TextView noFileTextView;
    private Button submitButton;
    private Button offlineSubmitButton;
    private Button eudiOfflineSubmitButton;
    private Button deleteVcButton;
    private Button buttonDebugAction;
    private EditText debugInputEditText;
    private List<CheckBox> claimCheckBoxes = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR scan canceled.", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedUrl = result.getContents();
                    handleSubmit(scannedUrl);
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
        setContentView(R.layout.activity_view_vc);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        claimsContainer = findViewById(R.id.claimsContainer);
        noFileTextView = findViewById(R.id.noFileTextView);
        submitButton = findViewById(R.id.submitButton);
        offlineSubmitButton = findViewById(R.id.offlineSubmitButton);
        eudiOfflineSubmitButton = findViewById(R.id.eudiOfflineSubmitButton);
        deleteVcButton = findViewById(R.id.deleteVcButton);

        LinearLayout emulatorDebugContainer = findViewById(R.id.emulatorDebugContainer);
        if (isEmulator()) {
            emulatorDebugContainer.setVisibility(View.VISIBLE);
        } else {
            emulatorDebugContainer.setVisibility(View.GONE);
        }

        buttonDebugAction = findViewById(R.id.debugActionButton);
        debugInputEditText = findViewById(R.id.debugInputEditText);

        submitButton.setOnClickListener(v -> launchQrScanner());
        offlineSubmitButton.setOnClickListener(v -> handleOfflineSubmit());
        eudiOfflineSubmitButton.setOnClickListener(v -> handleEudiwalletOfflineSubmit());
        deleteVcButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        buttonDebugAction.setOnClickListener(v -> {
            handleSubmit(debugInputEditText.getText().toString());
        });
        loadVcFile();
    }

    /**
     * Loads the VC file from internal storage and displays its claims.
     */
    private void loadVcFile() {
        List<WalletData> credentialList = WalletUtil.loadVcFileAsVcItem(this);

        if (credentialList != null && !credentialList.isEmpty()) {
            displayVcClaims(credentialList);
        } else {
            showFileNotFoundDialog();
        }
    }

    /**
     * Processes the submission of selected claims to the verifier's URI.
     *
     * @param scannedUriString The verifier's URI obtained from the QR scan.
     */
    private void handleSubmit(String scannedUriString) {
        Bundle selectedClaims = getSelectedClaims();
        ArrayList<String> selectedClaimsKeys = selectedClaims.getStringArrayList("selected_claims_keys");
        ArrayList<String> selectedClaimsNamespaces = selectedClaims.getStringArrayList("selected_claims_namespaces");

        Uri deepLinkUri = Uri.parse(scannedUriString);
        Intent intent = new Intent(Intent.ACTION_VIEW, deepLinkUri);
        intent.putStringArrayListExtra("selected_claims_keys", selectedClaimsKeys);
        intent.putStringArrayListExtra("selected_claims_namespaces", selectedClaimsNamespaces);
        
        startActivity(intent);
    }

    /**
     * Handles the offline submission by showing the QR generator activity.
     */
    private void handleOfflineSubmit() {
        Bundle selectedClaims = getSelectedClaims();
        ArrayList<String> selectedClaimsKeys = selectedClaims.getStringArrayList("selected_claims_keys");
        ArrayList<String> selectedClaimsNamespaces = selectedClaims.getStringArrayList("selected_claims_namespaces");

        if (selectedClaimsKeys == null || selectedClaimsKeys.isEmpty()) {
            Toast.makeText(this, "No claims selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, QRGeneratorActivity.class);
        intent.putExtra(BleMode.EXTRA_KEY, BleModePreferences.getMode(this).getValue());
        intent.putStringArrayListExtra("selected_claims_keys", selectedClaimsKeys);
        intent.putStringArrayListExtra("selected_claims_namespaces", selectedClaimsNamespaces);
        startActivity(intent);
    }

    /**
     * Handles the Eudiwallet offline submission by showing the QR generator activity 2.
     */
    private void handleEudiwalletOfflineSubmit() {
        Bundle selectedClaims = getSelectedClaims();
        ArrayList<String> selectedClaimsKeys = selectedClaims.getStringArrayList("selected_claims_keys");
        ArrayList<String> selectedClaimsNamespaces = selectedClaims.getStringArrayList("selected_claims_namespaces");

        if (selectedClaimsKeys == null || selectedClaimsKeys.isEmpty()) {
            Toast.makeText(this, "No claims selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, QRGeneratorActivity.class);
        intent.putExtra("is_eudi_wallet", true);
        intent.putExtra(BleMode.EXTRA_KEY, BleModePreferences.getMode(this).getValue());
        intent.putStringArrayListExtra("selected_claims_keys", selectedClaimsKeys);
        intent.putStringArrayListExtra("selected_claims_namespaces", selectedClaimsNamespaces);
        startActivity(intent);
    }

    /**
     * Gathers the selected claims from the UI.
     *
     * @return A Bundle containing the selected claims keys and namespaces.
     */
    private Bundle getSelectedClaims() {
        ArrayList<String> selectedClaimsKeys = new ArrayList<>();
        ArrayList<String> selectedClaimsNamespaces = new ArrayList<>();

        for (CheckBox cb : claimCheckBoxes) {
            if (cb.isChecked()) {
                Object tag = cb.getTag();
                if (tag instanceof Bundle) {
                    Bundle bundle = (Bundle) tag;
                    String key = bundle.getString("key");
                    String namespace = bundle.getString("namespace");

                    if (key != null && !key.isEmpty() && !"-".equals(key)) {
                        selectedClaimsKeys.add(key);
                        selectedClaimsNamespaces.add(namespace != null ? namespace : "");
                    }
                }
            }
        }

        Bundle result = new Bundle();
        result.putStringArrayList("selected_claims_keys", selectedClaimsKeys);
        result.putStringArrayList("selected_claims_namespaces", selectedClaimsNamespaces);
        return result;
    }

    /**
     * Displays the claims of the provided credential based on its format.
     *
     * @param credentialList The list of wallet data containing the credential.
     */
    private void displayVcClaims(List<WalletData> credentialList) {
        WalletData walletData = credentialList.get(0);
        String format = walletData.getFormat();
        String credentialData = (String) walletData.getCredential();

        claimsContainer.setVisibility(ViewGroup.VISIBLE);
        claimsContainer.removeAllViews();
        claimCheckBoxes.clear();

        if (SdjwtVc.isSupported(format)) {
            Map<String, Object> claims = SdjwtVc.getClaims(credentialData);
            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                addClaimView(entry.getKey(), entry.getValue(), null, 0, false, null);
            }
            submitButton.setVisibility(View.VISIBLE);
            offlineSubmitButton.setVisibility(View.VISIBLE);
            eudiOfflineSubmitButton.setVisibility(View.VISIBLE);
        } else if (OpenDid.isSupported(format)) {
            List<VerifiableCredential.Claim> claims = OpenDid.getClaims(credentialData);
            for (VerifiableCredential.Claim claim : claims) {
                renderOpenDidClaim(claim);
            }
            submitButton.setVisibility(View.VISIBLE);
            offlineSubmitButton.setVisibility(View.VISIBLE);
            eudiOfflineSubmitButton.setVisibility(View.VISIBLE);
        } else if (Mdoc.isSupported(format)) {
            addClaimView("Format", "mDoc", null, 0, false, null);
            Map<String, Object> nsMap = Mdoc.getClaims(credentialData);
            for (Map.Entry<String, Object> nsEntry : nsMap.entrySet()) {
                String namespace = nsEntry.getKey();
                addClaimView("Namespace", namespace, null, 0, false, null);
                Map<String, Object> claims = (Map<String, Object>) nsEntry.getValue();
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    boolean isSelectableParent = "driving_privileges".equals(entry.getKey());
                    addClaimView(entry.getKey(), entry.getValue(), namespace, 1, isSelectableParent, null);
                }
            }
            submitButton.setVisibility(View.VISIBLE);
            offlineSubmitButton.setVisibility(View.VISIBLE);
            eudiOfflineSubmitButton.setVisibility(View.VISIBLE);
        } else {
            showError("Unsupported VC format: " + format);
        }
    }

    /**
     * Renders a claim specifically for the OpenDID format.
     *
     * @param claim The claim to render.
     */
    private void renderOpenDidClaim(VerifiableCredential.Claim claim) {
        if (!"image".equalsIgnoreCase(claim.getType())) {
            addClaimView(claim.getCaption(), claim.getValue(), null, 0, false, null);
        }
    }

    /**
     * Displays a dialog when the VC file is not found.
     */
    private void showFileNotFoundDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification")
                .setMessage("No saved VC file. Please issue a credential first.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Displays a confirmation dialog before deleting the VC file.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete VC File")
                .setMessage("Are you sure you want to delete the VC file?")
                .setPositiveButton("Yes", (dialog, which) -> deleteVcFile())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the stored VC file.
     */
    private void deleteVcFile() {
        File file = new File(getFilesDir(), "vc.json");
        if (file.delete()) {
            if (deleteVcButton != null) {
                deleteVcButton.setVisibility(View.GONE);
            }
            finish();
        } else {
            if (!file.exists()) {
                Toast.makeText(this, "No VC file to delete.", Toast.LENGTH_SHORT).show();
                if (deleteVcButton != null) {
                    deleteVcButton.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(this, "Failed to delete VC file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Configures and launches the QR code scanner for submission.
     */
    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(true);
        options.setPrompt("Please scan the QR code to submit.");
        options.setBeepEnabled(false);
        options.setCaptureActivity(QrActivity.class);
        qrCodeLauncher.launch(options);
    }

    /**
     * Displays an error message when something goes wrong with VC processing.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        noFileTextView.setText(message);
        noFileTextView.setVisibility(ViewGroup.VISIBLE);
        claimsContainer.setVisibility(ViewGroup.GONE);
    }

    /**
     * Handles the up navigation action.
     *
     * @return True if the action was handled, false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Checks if the app is running on an emulator.
     *
     * @return true if running on an emulator, false otherwise.
     */
    private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    /**
     * Dynamically adds a claim view to the container, handling nested maps and lists recursively.
     *
     * @param caption The label for the claim.
     * @param value The value of the claim.
     * @param namespace The namespace for the claim (relevant for mDoc).
     * @param indentLevel The level of indentation for nested claims.
     * @param isSelectableParent Whether this claim is a parent that can be selected to toggle its children.
     * @param parentCheckBox The checkbox of the parent claim, if any.
     */
    private void addClaimView(String caption, Object value, String namespace, int indentLevel, boolean isSelectableParent, CheckBox parentCheckBox) {
        if (caption == null || value == null) return;

        int marginLeft = (int) (getResources().getDisplayMetrics().density * 20 * indentLevel);

        if (value instanceof Map || value instanceof List) {
            String formattedCaption = (caption.length() > 0) ? caption.substring(0, 1).toUpperCase() + caption.substring(1) : caption;

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            headerRow.setPadding(marginLeft, 0, 0, 0);

            CheckBox currentCheckBox = null;
            List<CheckBox> childrenCheckBoxes = new ArrayList<>();
            if (isSelectableParent) {
                currentCheckBox = new CheckBox(this);
                currentCheckBox.setChecked(true);
                Bundle tag = new Bundle();
                tag.putBoolean("isParent", true);
                tag.putString("key", caption);
                tag.putString("namespace", namespace);
                currentCheckBox.setTag(tag);
                headerRow.addView(currentCheckBox);
                claimCheckBoxes.add(currentCheckBox);

                CheckBox finalCurrentCheckBox = currentCheckBox;
                currentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    for (CheckBox child : childrenCheckBoxes) {
                        child.setChecked(isChecked);
                    }
                });
            } else {
                currentCheckBox = parentCheckBox;
            }

            TextView captionTextView = new TextView(this);
            captionTextView.setText(formattedCaption + ":");
            captionTextView.setTextSize(14f);
            captionTextView.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            headerRow.addView(captionTextView);
            claimsContainer.addView(headerRow);

            int beforeCount = claimCheckBoxes.size();

            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
                    addClaimView(entry.getKey(), entry.getValue(), namespace, indentLevel + 1, false, currentCheckBox);
                }
            } else {
                List<?> nestedList = (List<?>) value;
                for (int i = 0; i < nestedList.size(); i++) {
                    addClaimView("-", nestedList.get(i), namespace, indentLevel + 1, false, currentCheckBox);
                }
            }

            if (isSelectableParent && currentCheckBox != null) {
                int afterCount = claimCheckBoxes.size();
                for (int i = beforeCount; i < afterCount; i++) {
                    childrenCheckBoxes.add(claimCheckBoxes.get(i));
                }
            }

        } else {
            LinearLayout claimRow = new LinearLayout(this);
            claimRow.setOrientation(LinearLayout.HORIZONTAL);
            claimRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            claimRow.setPadding(marginLeft, 0, 0, (int) (getResources().getDisplayMetrics().density * 16));

            List<String> metadataCaptions = Arrays.asList("Format", "Namespace", "Issuer", "Subject", "vct");
            boolean isMetadata = metadataCaptions.contains(caption);

            if (!isMetadata) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setChecked(true);
                
                Bundle tag = new Bundle();
                if (parentCheckBox == null) {
                    tag.putString("namespace", namespace);
                    tag.putString("key", caption);
                } else {
                    tag.putString("key", "-");
                }
                tag.putString("value", String.valueOf(value));
                checkBox.setTag(tag);
                
                if (parentCheckBox != null) {
                    checkBox.setClickable(false);
                    checkBox.setFocusable(false);
                }
                
                claimCheckBoxes.add(checkBox);
                claimRow.addView(checkBox);
            }

            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setPadding((int) (getResources().getDisplayMetrics().density * (isMetadata ? 0 : 8)), 0, 0, 0);
            
            LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textContainer.setLayoutParams(textContainerParams);

            TextView captionTextView = new TextView(this);
            String formattedCaption = (caption.length() > 0) ? caption.substring(0, 1).toUpperCase() + caption.substring(1) : caption;
            captionTextView.setText(formattedCaption);
            captionTextView.setTextSize(14f);
            textContainer.addView(captionTextView);

            if ("portrait".equalsIgnoreCase(caption) && value instanceof byte[]) {
                byte[] imageBytes = (byte[]) value;
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    ImageView imageView = new ImageView(this);
                    imageView.setImageBitmap(bitmap);
                    imageView.setAdjustViewBounds(true);
                    
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, (int)(getResources().getDisplayMetrics().density * 8), 0, (int)(getResources().getDisplayMetrics().density * 16));
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.FIT_START);
                    
                    textContainer.addView(imageView);
                    claimRow.addView(textContainer);
                    claimsContainer.addView(claimRow);
                    return;
                }
            }

            TextView valueTextView = new TextView(this);
            String displayValue;
            if (value instanceof byte[]) {
                displayValue = Base64.encodeToString((byte[]) value, Base64.NO_WRAP);
            } else {
                displayValue = String.valueOf(value);
            }
            valueTextView.setText(displayValue);
            valueTextView.setTextSize(18f);
            valueTextView.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

            textContainer.addView(valueTextView);
            claimRow.addView(textContainer);
            claimsContainer.addView(claimRow);
        }
    }

}
