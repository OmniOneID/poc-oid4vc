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

package org.omnione.did.mdoc.reader.ui.transferStatus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
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

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.mdoc.reader.config.ConfigProvider;
import org.omnione.did.mdoc.reader.di.DependencyProvider;
import org.omnione.did.sdk.mdoc.proximity.reader.core.DeviceEngagement;
import org.omnione.did.sdk.mdoc.proximity.reader.core.EngagementSource;
import org.omnione.did.mdoc.reader.settings.PreferencesManager;
import org.omnione.did.sdk.mdoc.proximity.reader.core.TransferController;
import org.omnione.did.mdoc.reader.databinding.FragmentTransferStatusBinding;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.ReceivedDocument;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.RequestedDocument;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.TransferStatus;

import org.omnione.did.sdk.mdoc.proximity.reader.communication.TransportConfig;
import org.omnione.did.sdk.mdoc.proximity.reader.communication.WifiAwareTransportManager;
import org.omnione.did.sdk.mdoc.proximity.reader.communication.NfcTransportManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferStatusFragment extends Fragment {
    private FragmentTransferStatusBinding binding;

    private TransferController transferController;
    private PreferencesManager preferencesManager;
    private ConfigProvider configProvider;

    private DependencyProvider deps() {
        return (DependencyProvider) requireActivity().getApplication();
    }

    private String pendingQrCode;
    private byte[] pendingNfcData;
    private android.nfc.Tag pendingNfcTag;
    private ArrayList<RequestedDocument> pendingDocs;

    private boolean navigated = false;

    private final ActivityResultLauncher<String[]> blePermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = !result.containsValue(false);
            if (allGranted) {
                startTransferIfReady();
            } else {
                Toast.makeText(requireContext(), "Bluetooth permissions are required", Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransferStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        // Dependency injection
        transferController = deps().transferController();
        preferencesManager = deps().preferencesManager();
        configProvider = deps().configProvider();

        binding.tvTitle.setText(R.string.transfer_title);
        binding.btnClose.setOnClickListener(v -> {
            transferController.stopConnection();
            navController.navigate(R.id.action_global_pop_to_home);
        });

        // Process received arguments
        Bundle args = getArguments();
        if (args != null) {
            pendingQrCode = args.getString("qrCode");
            pendingNfcData = args.getByteArray("nfcData");
            pendingNfcTag = args.getParcelable("nfcTag");
            pendingDocs = args.getParcelableArrayList("requestedDocs");
            if ((pendingQrCode != null || pendingNfcData != null) && pendingDocs != null) {
                requestPermissionsAndStart();
            }
        }
    }

    private void requestPermissionsAndStart() {
        List<String> needed = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        if (needed.isEmpty()) {
            startTransferIfReady();
        } else {
            blePermissionLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private void startTransferIfReady() {
        if (pendingDocs == null) return;

        EngagementSource source = null;
        if (pendingQrCode != null) {
            source = new EngagementSource.QrCode(pendingQrCode);
        } else if (pendingNfcData != null) {
            source = new EngagementSource.Nfc(pendingNfcData);
        }
        if (source != null) {
            startTransfer(source, pendingDocs);
        }
    }

    private void startTransfer(EngagementSource source, List<RequestedDocument> requestedDocs) {
        showLoading(true);
        setRequestInfo(requestedDocs);

        List<String> certs = configProvider.getCertificates();
        transferController.initializeVerifier(certs, preferencesManager.isSkipIssuerTrust(), true);

        // Parse DeviceEngagement first to determine supported transfer methods
        TransportConfig config;
        try {
            DeviceEngagement de = source.resolve();
            // Priority: 1) Wi-Fi Aware -> 2) BLE -> 3) NFC
            if (de.hasWifiAwareConnectionMethod() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                config = new TransportConfig.WifiAware(
                    de.getWifiAwarePassphrase(),
                    de.getWifiAwareChannelInfo(),
                    de.getWifiAwareBandInfo()
                );
            } else if (de.hasBleConnectionMethod()) {
                config = new TransportConfig.Ble(
                    preferencesManager.isBlePeripheralServer(),
                    UUID.randomUUID(),
                    UUID.randomUUID()
                );
            } else if (de.hasNfcConnectionMethod() && pendingNfcTag != null) {
                android.nfc.tech.IsoDep isoDep = android.nfc.tech.IsoDep.get(pendingNfcTag);
                if (isoDep != null) {
                    config = new TransportConfig.Nfc(
                        isoDep,
                        de.getNfcMaxCommandDataLength(),
                        de.getNfcMaxResponseDataLength()
                    );
                } else {
                    config = new TransportConfig.Ble(
                        preferencesManager.isBlePeripheralServer(),
                        UUID.randomUUID(),
                        UUID.randomUUID()
                    );
                }
            } else {
                config = new TransportConfig.Ble(
                    preferencesManager.isBlePeripheralServer(),
                    UUID.randomUUID(),
                    UUID.randomUUID()
                );
            }
        } catch (Exception e) {
            // Fall back to default BLE on parsing failure
            config = new TransportConfig.Ble(
                preferencesManager.isBlePeripheralServer(),
                UUID.randomUUID(),
                UUID.randomUUID()
            );
        }
        transferController.initializeTransferManager(config);

        transferController.startEngagement(source);

        boolean retainData = preferencesManager.isRetainData();
        transferController.sendRequest(requestedDocs, retainData, this::handleStatus);
    }

    private void setRequestInfo(List<RequestedDocument> requestedDocs) {
        StringBuilder sb = new StringBuilder();
        for (RequestedDocument doc : requestedDocs) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(doc.getDocumentType().getDisplayName())
              .append(" (").append(doc.getMode().getDisplayName()).append(")");
        }
        binding.tvRequestInfo.setText(sb.toString());
    }

    private void handleStatus(TransferStatus status) {
        if (binding == null) return;

        switch (status.getType()) {
            case CONNECTING:
                binding.tvStatus.setText("Connecting...");
                break;
            case CONNECTED:
                binding.tvStatus.setText("Connected");
                break;
            case DEVICE_ENGAGEMENT_COMPLETED:
                binding.tvStatus.setText("Device engagement completed");
                break;
            case REQUEST_SENT:
                binding.tvStatus.setText("Request sent");
                break;
            case RESPONSE_RECEIVED:
                showLoading(false);
                binding.tvStatus.setText("Response received");
                if (!navigated) {
                    navigated = true;
                    List<ReceivedDocument> docs = status.getReceivedDocuments();
                    if (docs != null && !docs.isEmpty()) {
                        Bundle showArgs = new Bundle();
                        showArgs.putSerializable("receivedDocs", (Serializable) new ArrayList<>(docs));
                        NavHostFragment.findNavController(this)
                            .navigate(R.id.action_transferStatus_to_showDocuments, showArgs);
                    }
                }
                break;
            case ERROR:
                showLoading(false);
                binding.tvStatus.setText("Error: " + status.getErrorMessage());
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(status.getErrorMessage());
                break;
            case DISCONNECTED:
                showLoading(false);
                binding.tvStatus.setText("Disconnected");
                break;
        }
    }

    private void showLoading(boolean loading) {
        if (binding != null) {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        transferController.stopConnection();
        binding = null;
    }
}
