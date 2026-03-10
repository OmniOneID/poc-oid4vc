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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.omnione.did.sdjwt.datamodel.Disclosure;
import org.omnione.did.sdjwt.datamodel.SDJWT;
import org.omnione.did.sdjwt.util.SimpleJWTDecoder;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.data.dto.tec.VerifiableCredential;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.omnione.did.sdk.oid4vc.R;

import COSE.CoseException;
import COSE.Message;
import COSE.MessageTag;
import COSE.OneKey;
import COSE.Sign1Message;

public class ViewVcActivity extends AppCompatActivity {

    private LinearLayout claimsContainer;
    private TextView noFileTextView;
    private Button submitButton;
    private Button deleteVcButton;
    private Button buttonDebugAction;
    private EditText debugInputEditText;
    private Gson gson = new Gson();
    private VerifiableCredential loadedVc;

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR scan canceled.", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedUrl = result.getContents();
                    handleSubmit(scannedUrl);
                }
            });

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
        deleteVcButton = findViewById(R.id.deleteVcButton);

        buttonDebugAction = findViewById(R.id.debugActionButton);
        debugInputEditText = findViewById(R.id.debugInputEditText);

        submitButton.setOnClickListener(v -> launchQrScanner());
        deleteVcButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        buttonDebugAction.setOnClickListener(v -> {
            handleSubmit(debugInputEditText.getText().toString());
        });
        loadVcFile();
    }

    private void loadVcFile() {
        File file = new File(getFilesDir(), "vc.json");

        if (file.exists()) {
            String encodedVcData = readStringFromFile(file);
            if (encodedVcData != null) {
                displayVcClaims(encodedVcData);
            } else {
                showError("Failed to read file.");
            }
        } else {
            showFileNotFoundDialog();
        }
    }

    private String readStringFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e("sangjun", "File read error", e);
            return null;
        }
    }

    private void handleSubmit(String scannedUriString) {
        Uri deepLinkUri = Uri.parse(scannedUriString);
        Intent intent = new Intent(Intent.ACTION_VIEW, deepLinkUri);
        startActivity(intent);
    }


    private void displayVcClaims(String base64EncodedVcArray) {
        Log.d("sangjun", "Encoded VC array JSON: " + base64EncodedVcArray);

            Type listType = new TypeToken<ArrayList<WalletData>>() {}.getType();
            List<WalletData> credentialList = gson.fromJson(base64EncodedVcArray, listType);

            if (credentialList == null || credentialList.isEmpty()) {
                showError("No saved VC found.");
                return;
            }

            WalletData walletData = credentialList.get(0);
            String format = walletData.getFormat();
            Log.d("sangjun",  "vc json format: " + format);
            Object credentialData = walletData.getCredential();

            // Branch based on format value
            if (format.equals("NationalID") || format.equals("NationalIDCert")) {
                if (credentialData instanceof String) {
                    displayJwtVc((String) credentialData);
                } else {
                    showError("Invalid JWT format VC.");
                }

            }
            else if(format.equals("TEC") || format.equals("UCR")) {
                byte[] decodedBytes = Base64.decode((String) credentialData, Base64.DEFAULT);
                String jsonArrayString = new String(decodedBytes, StandardCharsets.UTF_8);
                VerifiableCredential vc = gson.fromJson(jsonArrayString, VerifiableCredential.class);
                tec(vc);
            }
            else if( format.equals("mDL") || format.equals("mDocPID")) {
                if (credentialData instanceof String) {
                    displaymDL((String) credentialData);
                } else {
                    showError("Invalid mDL format VC.");
                }
            }
            else {
                showError("Unsupported VC format: " + format);
            }

    }

    private void displaymDL(String mDlData) {
        try {
            claimsContainer.setVisibility(ViewGroup.VISIBLE);
            claimsContainer.removeAllViews();

            addClaimView("Format", "mDoc", 0);

            try {
                byte[] decodedBytes = Base64.decode(mDlData, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                
                // Use Jackson CBOR decoding
                ObjectMapper mapper = new ObjectMapper(new CBORFactory());
                Map<String, Object> map = mapper.readValue(decodedBytes, Map.class);
                
                if (map != null) {
                    // Try to extract namespaces if they exist in a typical mDL structure
                    if (map.containsKey("nameSpaces")) {
                        Object nameSpacesObj = map.get("nameSpaces");
                        if (nameSpacesObj instanceof Map) {
                            Map<String, Object> nameSpaces = (Map<String, Object>) nameSpacesObj;
                            for (Map.Entry<String, Object> entry : nameSpaces.entrySet()) {
                                Log.d("sangjun", "Namespace: " + entry.getKey());
                                addClaimView("Namespace", entry.getKey(), 0);
                                Object itemsObj = entry.getValue();
                                if (itemsObj instanceof List) {
                                    List<?> items = (List<?>) itemsObj;
                                    for (Object item : items) {
                                        if (item instanceof byte[]) {
                                            try {
                                                Map<String, Object> decodedItem = mapper.readValue((byte[]) item, Map.class);
                                                if (decodedItem.containsKey("elementIdentifier") && decodedItem.containsKey("elementValue")) {
                                                    String identifier = (String) decodedItem.get("elementIdentifier");
                                                    Object value = decodedItem.get("elementValue");
                                                    Log.d("sangjun", "mDL Item - Key: " + identifier + ", Value: " + value);
                                                    addClaimView(identifier, value, 1);
                                                }
                                            } catch (Exception ignored) {
                                                // Fallback: if it's not a map, just show it
                                                Log.d("sangjun", "mDL Item (Raw) - Value: " + item);
                                                addClaimView("Item", item, 1);
                                            }
                                        } else {
                                            Log.d("sangjun", "mDL Item (Non-byte[]) - Value: " + item);
                                            addClaimView("Item", item, 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    addClaimView("mDL Data", mDlData, 0);
                }
            } catch (Exception e) {
                Log.e("sangjun", "CBOR decoding failed", e);
                // If CBOR decoding fails, try to show the base64url data
                addClaimView("mDL Data (Base64URL)", mDlData, 0);
            }

            submitButton.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e("sangjun", "mDL display failed", e);
            showError("Failed to display mDL data.");
        }
    }

    private void tec(VerifiableCredential vc){
        try {
             List<VerifiableCredential.Claim> claims = vc.getCredentialSubject().getClaims();

            if (claims == null || claims.isEmpty()) {
                showError("No Claim information in VC.");
                return;
            }

            claimsContainer.setVisibility(ViewGroup.VISIBLE);
            claimsContainer.removeAllViews();

            for (VerifiableCredential.Claim claim : claims) {
                if ("image".equalsIgnoreCase(claim.getType())) {
                    Log.d("sangjun", "Image data found: " + claim.getCaption());
                } else {
                    TextView captionTextView = new TextView(this);
                    captionTextView.setText(claim.getCaption());
                    captionTextView.setTextSize(14f);

                    TextView valueTextView = new TextView(this);
                    valueTextView.setText(claim.getValue());
                    valueTextView.setTextSize(18f);
                    valueTextView.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 48);
                    valueTextView.setLayoutParams(params);

                    claimsContainer.addView(captionTextView);
                    claimsContainer.addView(valueTextView);
                }
            }
            submitButton.setVisibility(View.VISIBLE);

        } catch (IllegalArgumentException e) {
            Log.e("sangjun", "Base64 decoding failed", e);
            showError("Invalid VC data format. (Base64 error)");
        }
    }

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

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete VC File")
                .setMessage("Are you sure you want to delete the VC file?")
                .setPositiveButton("Yes", (dialog, which) -> deleteVcFile())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteVcFile() {
        File file = new File(getFilesDir(), "vc.json");
        if (file.delete()) {
            if (deleteVcButton != null) { // Add null check
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
                Toast.makeText(this, "Failed to delete VC file. File may be in use or there may be a permission issue.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(true);
        options.setPrompt("Please scan the QR code to submit.");
        options.setBeepEnabled(false);
        options.setCaptureActivity(QrActivity.class);
        qrCodeLauncher.launch(options);
    }

    private void showError(String message) {
        noFileTextView.setText(message);
        noFileTextView.setVisibility(ViewGroup.VISIBLE);
        claimsContainer.setVisibility(ViewGroup.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void displayJwtVc(String sdJwtVc) {
        SDJWT parsedVC = SDJWT.parse(sdJwtVc);

        SimpleJWTDecoder.SimpleJWT credentialJWT = SimpleJWTDecoder.parse(parsedVC.getCredentialJwt());
        if (credentialJWT == null) {
            showError("SD-JWT payload not found.");
            return;
        }

        claimsContainer.setVisibility(ViewGroup.VISIBLE);
        claimsContainer.removeAllViews();
        addClaimView("Issuer", credentialJWT.getPayload().get("iss"), 0);
        addClaimView("Subject", credentialJWT.getPayload().get("sub"), 0);
        addClaimView("vct", credentialJWT.getPayload().get("vct"), 0);

        for (int i = 0; i < parsedVC.getDisclosureCount(); i++) {
            Disclosure disclosure = parsedVC.getDisclosures().get(i);
            addClaimView(disclosure.getClaimName(), disclosure.getClaimValue(), 0);
        }

        submitButton.setVisibility(View.VISIBLE);
    }

    private void addClaimView(String caption, Object value, int indentLevel) {
        if (caption == null || value == null) return;

        int marginLeft = (int) (getResources().getDisplayMetrics().density * 20 * indentLevel);

        if (value instanceof Map) {
            String formattedCaption = (caption.length() > 0) ? caption.substring(0, 1).toUpperCase() + caption.substring(1) : caption;

            TextView parentCaptionTextView = new TextView(this);
            parentCaptionTextView.setText(formattedCaption + ":"); // e.g., "Address:"
            parentCaptionTextView.setTextSize(14f);
            parentCaptionTextView.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            parentCaptionTextView.setPadding(marginLeft, 0, 0, 0); // Apply indentation
            claimsContainer.addView(parentCaptionTextView);

            Map<String, Object> nestedMap = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
                addClaimView(entry.getKey(), entry.getValue(), indentLevel + 1); // Increase indentation level
            }

        } else if (value instanceof List) {
            String formattedCaption = (caption.length() > 0) ? caption.substring(0, 1).toUpperCase() + caption.substring(1) : caption;

            TextView listCaptionTextView = new TextView(this);
            listCaptionTextView.setText(formattedCaption + ":");
            listCaptionTextView.setTextSize(14f);
            listCaptionTextView.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            listCaptionTextView.setPadding(marginLeft, 0, 0, 0);
            claimsContainer.addView(listCaptionTextView);

            List<?> nestedList = (List<?>) value;
            for (int i = 0; i < nestedList.size(); i++) {
                addClaimView("-", nestedList.get(i), indentLevel + 1); // Each item is displayed with "-" caption
            }

        } else {
            TextView captionTextView = new TextView(this);
            String formattedCaption = (caption.length() > 0) ? caption.substring(0, 1).toUpperCase() + caption.substring(1) : caption;
            captionTextView.setText(formattedCaption);
            captionTextView.setTextSize(14f);
            captionTextView.setPadding(marginLeft, 0, 0, 0); // Apply indentation
            claimsContainer.addView(captionTextView);

            if ("portrait".equalsIgnoreCase(caption) && value instanceof byte[]) {
                byte[] imageBytes = (byte[]) value;
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    ImageView imageView = new ImageView(this);
                    imageView.setImageBitmap(bitmap);
                    imageView.setAdjustViewBounds(true);
                    int maxHeightPx = (int) (getResources().getDisplayMetrics().density * 200);
                    imageView.setMaxHeight(maxHeightPx);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(marginLeft, 0, 0, (int)(getResources().getDisplayMetrics().density * 16));
                    imageView.setLayoutParams(params);
                    claimsContainer.addView(imageView);
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
            valueTextView.setPadding(marginLeft, 0, 0, 0); // Apply indentation

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, (int)(getResources().getDisplayMetrics().density * 4)); // Reduce margin slightly for better appearance
            valueTextView.setLayoutParams(params);

            claimsContainer.addView(valueTextView);
        }
    }

    public boolean verifySignatureMdl(byte[] issuerAuthBytes) {
//        try {
//            String publicKey= "Ay/5wNs8D1oX+FDRYgnJUmZ/Ovnff+/73G8LD53+m1tk";
//
//            PublicKey publicKeyObject = KeyUtil.getPublicKeyObject(KeyUtil.unCompressPublicKey(org.bouncycastle.util.encoders.Base64.decode(publicKey)));
//            // 1. 바이트를 COSE Sign1 메시지 객체로 변환
//            Sign1Message msg = (Sign1Message) Message.DecodeFromBytes(issuerAuthBytes, MessageTag.Sign1);
//
//            // 2. 검증에 사용할 공개키를 OneKey 객체로 변환
//            OneKey key = new OneKey(publicKeyObject, null);
//
//            // 3. 라이브러리 내장 함수로 검증 수행
//            // 내부적으로 [Protected Header + Payload(MSO) + External AAD]를 해시하고
//            // 서명값(Signature)과 비교합니다.
//            boolean isValid = msg.validate(key);
//
//            System.out.println(">>> 서명 검증 결과: " + (isValid ? "성공 (Pass)" : "실패 (Fail)"));
//            return isValid;
//
//        } catch (CoseException e) {
//            System.err.println("서명 검증 중 오류: " + e.getMessage());
//            return false;
//        }
        return true;
    }
}
