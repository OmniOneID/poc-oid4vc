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
package org.omnione.did.mdoc.reader.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.mdoc.reader.ui.ContainerActivity;
import org.omnione.did.mdoc.reader.config.ConfigProvider;
import org.omnione.did.mdoc.reader.di.DependencyProvider;
import org.omnione.did.sdk.mdoc.proximity.reader.core.PlatformController;
import org.omnione.did.mdoc.reader.settings.PreferencesManager;
import org.omnione.did.mdoc.reader.databinding.FragmentHomeBinding;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.DocumentMode;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.RequestedDocument;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.SupportedDocumentUi;

import java.util.*;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeDocumentAdapter adapter;

    private PlatformController platformController;
    private ConfigProvider configProvider;
    private PreferencesManager preferencesManager;

    private DocumentSelectionManager selectionManager;
    private NfcEngagementHandler nfcHandler;

    private boolean nfcNavigating = false;
    private static final String TAG = "HomeFragment";

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Log.d(TAG, "Permission result: " + result);
            if (!hasAllPermissions()) {
                // Permanently denied permissions → Guide user to app settings
                boolean permanentlyDenied = false;
                for (String perm : getRequiredPermissions()) {
                    if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED
                            && !shouldShowRequestPermissionRationale(perm)) {
                        permanentlyDenied = true;
                        break;
                    }
                }
                if (permanentlyDenied) {
                    Toast.makeText(requireContext(), "Please grant the required permissions in Settings", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().getPackageName(), null)));
                }
            }
        });

    private DependencyProvider deps() {
        return (DependencyProvider) requireActivity().getApplication();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        // Dependency injection
        platformController = deps().platformController();
        configProvider = deps().configProvider();
        preferencesManager = deps().preferencesManager();

        // Initialize selection manager
        selectionManager = new DocumentSelectionManager(configProvider, preferencesManager);
        selectionManager.setSelectionChangeListener(this::onSelectionChanged);

        // Initialize NFC handler
        nfcHandler = new NfcEngagementHandler(requireActivity(),
            (bytes, tag) -> handleDeviceEngagement(bytes, tag));

        binding.tvTitle.setText(R.string.home_screen_title);

        // Set up document adapter
        adapter = new HomeDocumentAdapter(new HomeDocumentAdapter.Callback() {
            @Override
            public void onModeSelected(SupportedDocumentUi doc, DocumentMode mode) {
                selectionManager.selectMode(doc, mode);
            }
            @Override
            public boolean onDocDeselected(String docId) {
                return selectionManager.deselectDocument(docId);
            }
            @Override
            public void onClaimToggled(String docId, String claimLabel) {
                selectionManager.toggleClaim(docId, claimLabel);
            }
            @Override
            public void onToggleAllClaims(String docId) {
                for (SupportedDocumentUi d : selectionManager.getSupportedDocs()) {
                    if (d.getId().equals(docId)) {
                        selectionManager.toggleAllClaims(docId, d.getDocumentType());
                        break;
                    }
                }
            }
        });

        binding.rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDocuments.setAdapter(adapter);

        // Load document list and restore selection state
        selectionManager.loadDocuments();
        adapter.submitList(selectionManager.getSupportedDocs(), selectionManager);
        updateScanButton();

        // Menu button
        binding.btnMenu.setOnClickListener(v ->
            navController.navigate(R.id.action_home_to_menu));

        // QR scan button
        binding.btnScanQr.setOnClickListener(v -> {
            List<RequestedDocument> docs = selectionManager.buildRequestedDocuments();
            if (!docs.isEmpty()) {
                Bundle args = new Bundle();
                args.putParcelableArrayList("requestedDocs", new ArrayList<>(docs));
                navController.navigate(R.id.action_home_to_qrScan, args);
            }
        });

        // Handle back press
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    platformController.closeApp();
                }
            });

        // Handle NFC cold start
        nfcHandler.checkIntent(requireActivity().getIntent());

        // Request all required permissions on home entry
        requestAllPermissions();
    }

    private java.util.List<String> getRequiredPermissions() {
        java.util.List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        return perms;
    }

    private boolean hasAllPermissions() {
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestAllPermissions() {
        java.util.List<String> needed = new ArrayList<>();
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }
        Log.d(TAG, "Permissions needed: " + needed);
        if (!needed.isEmpty()) {
            permissionLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private void onSelectionChanged() {
        adapter.refreshState();
        updateScanButton();
    }

    private void updateScanButton() {
        boolean valid = selectionManager.hasValidSelection();
        binding.btnScanQr.setEnabled(valid);
        binding.btnScanQr.setAlpha(valid ? 1.0f : 0.5f);
    }

    private void handleDeviceEngagement(byte[] deviceEngagementBytes, android.nfc.Tag nfcTag) {
        if (nfcNavigating) return;
        nfcNavigating = true;

        List<RequestedDocument> docs = selectionManager.buildRequestedDocuments();
        if (!docs.isEmpty()) {
            Bundle args = new Bundle();
            args.putParcelableArrayList("requestedDocs", new ArrayList<>(docs));
            args.putByteArray("nfcData", deviceEngagementBytes);
            if (nfcTag != null) {
                args.putParcelable("nfcTag", nfcTag);
            }
            NavHostFragment.findNavController(this).navigate(R.id.action_home_to_transferStatus, args);
        } else {
            android.widget.Toast.makeText(requireContext(),
                "Please select a document to request first!", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcNavigating = false;
        nfcHandler.reset();
        // Register delegate with Activity's Reader Mode (no Reader Mode re-invocation)
        if (requireActivity() instanceof ContainerActivity activity) {
            activity.setNfcReaderCallback(nfcHandler);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister delegate -- Reader Mode itself remains active in the Activity
        if (requireActivity() instanceof ContainerActivity activity) {
            activity.setNfcReaderCallback(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
