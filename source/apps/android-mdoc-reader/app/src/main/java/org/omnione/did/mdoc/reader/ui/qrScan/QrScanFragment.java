/*
 * Copyright 2026 OmniOne.
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

package org.omnione.did.mdoc.reader.ui.qrScan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.google.zxing.ResultPoint;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.mdoc.reader.databinding.FragmentQrScanBinding;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.RequestedDocument;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.Constants;

import java.util.ArrayList;
import java.util.List;

public class QrScanFragment extends Fragment {
    private FragmentQrScanBinding binding;
    private boolean scanProcessed = false;
    private boolean scannerInitialized = false;
    private boolean navigated = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                initScanner();
                binding.barcodeScanner.resume();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQrScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        binding.tvTitle.setText(R.string.qr_scan_title);
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initScanner() {
        if (scannerInitialized) return;
        scannerInitialized = true;
        binding.barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!scanProcessed && result != null && result.getText() != null) {
                    scanProcessed = true;
                    onQrScanned(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    private void onQrScanned(String qrCode) {
        if (qrCode != null && qrCode.startsWith(Constants.MDOC_PREFIX)) {
            if (!navigated) {
                navigated = true;
                ArrayList<RequestedDocument> docs = getArguments() != null ?
                    getArguments().getParcelableArrayList("requestedDocs") : null;

                Bundle args = new Bundle();
                args.putString("qrCode", qrCode);
                if (docs != null) args.putParcelableArrayList("requestedDocs", docs);
                NavHostFragment.findNavController(this).navigate(R.id.action_qrScan_to_transferStatus, args);
            }
        } else {
            Toast.makeText(requireContext(), "Invalid QR code. Expected mDOC device engagement.", Toast.LENGTH_SHORT).show();
            scanProcessed = false; // Allow retry
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.barcodeScanner.resume();
        scanProcessed = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.barcodeScanner.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
