package org.omnione.did.sdk.oid4vc.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.upokecenter.cbor.CBORObject;

import org.omnione.did.sdk.oid4vc.R;
import org.omnione.did.sdk.oid4vc.data.dto.WalletData;
import org.omnione.did.sdk.oid4vc.format.Mdoc;
import org.omnione.did.sdk.mdoc.proximity.holder.ble.BleConstants;
import org.omnione.did.sdk.mdoc.proximity.holder.core.EphemeralKeyHolder;
import org.omnione.did.sdk.mdoc.proximity.holder.ble.MdocBleServer;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocEngagement;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityListener;
import org.omnione.did.sdk.mdoc.proximity.holder.nfc.MdocHostApduService;
import org.omnione.did.sdk.mdoc.proximity.holder.nfc.MdocNfcServer;
import org.omnione.did.sdk.mdoc.proximity.holder.wifi.MdocWifiServer;
import org.omnione.did.sdk.oid4vc.util.CryptoUtil;
import org.omnione.did.sdk.oid4vc.util.WalletUtil;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.utility.MultibaseUtils;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("NewApi")
public class QRGeneratorActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private Button closeButton;
    private MdocBleServer bleServer;
    private MdocNfcServer nfcServer;
    private MdocWifiServer wifiServer;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private String mDoc;
    private ArrayList<String> selectedKeys;
    private ArrayList<String> namespaces;
    private byte[] deviceEngagementBytes;

    private NfcAdapter nfcAdapter;
    private CardEmulation cardEmulation;
    private ComponentName apduServiceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Offline Presentation");
        }

        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());

        // Load data from Intent
        selectedKeys = getIntent().getStringArrayListExtra("selected_claims_keys");
        namespaces = getIntent().getStringArrayListExtra("selected_claims_namespaces");

        // Load mDoc from storage
        loadMdoc();

        // ✅ NFC Card Emulation 초기화 (포그라운드 우선권용)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            cardEmulation = CardEmulation.getInstance(nfcAdapter);
            apduServiceName = new ComponentName(this, MdocHostApduService.class);
        }

        checkPermissionsAndGenerateQR();
    }

    // 앱이 화면에 뜰 때 NFC 우선권 강제 획득
    @Override
    protected void onResume() {
        super.onResume();
        if (cardEmulation != null && apduServiceName != null) {
            cardEmulation.setPreferredService(this, apduServiceName);
            Log.d("QRGeneratorActivity", "NFC 우선권 획득 완료");
        }
    }

    // 앱이 화면에서 사라질 때 NFC 우선권 반환
    @Override
    protected void onPause() {
        super.onPause();
        if (cardEmulation != null) {
            cardEmulation.unsetPreferredService(this);
            Log.d("QRGeneratorActivity", "NFC 우선권 반환");
        }
    }

    private void loadMdoc() {
        List<WalletData> credentialList = WalletUtil.loadVcFileAsVcItem(this);
        if (credentialList != null && !credentialList.isEmpty()) {
            WalletData walletData = credentialList.get(0);
            if (Mdoc.isSupported(walletData.getFormat())) {
                mDoc = (String) walletData.getCredential();
            }
        }
    }

    private void checkPermissionsAndGenerateQR() {
        List<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissionsList.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        String[] permissions = permissionsList.toArray(new String[0]);

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            generateQR();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                generateQR();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for offline presentation.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleServer != null) {
            bleServer.stop();
        }
        if (nfcServer != null) {
            nfcServer.stop();
        }
        if (wifiServer != null) {
            wifiServer.stop();
        }
    }

    private void generateQR() {
        try {
            // Generate real EDeviceKeyBytes for DeviceEngagement
            EphemeralKeyHolder keyHolder = MdocEngagement.generateEDeviceKeyBytes();
            byte[] eDeviceKeyBytes = keyHolder.eDeviceKeyBytes;
            PrivateKey privateKey = keyHolder.privateKey;

            // Reader가 기대하는 서비스 UUID 바이트 (16바이트)를 임의로 생성합니다.
            byte[] bleUuidBytes = BleConstants.generateRandomBleUuid();

            // Create DeviceEngagement Payload using MdocEngagement utility
            String qrData;
            boolean isEudiWallet = getIntent().getBooleanExtra("is_eudi_wallet", false);
            if (isEudiWallet) {
                qrData = MdocEngagement.createDeviceEngagementPayloadForEudi(eDeviceKeyBytes, bleUuidBytes);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Eudiwallet Offline");
                }
            } else {
                qrData = MdocEngagement.createDeviceEngagementPayload(eDeviceKeyBytes, bleUuidBytes);
            }

            String base64UrlPayload = qrData.replace("mdoc:", "");
            byte[] deviceEngagementBytes = Base64.decode(base64UrlPayload, Base64.URL_SAFE | Base64.NO_PADDING);
            CBORObject deviceEngagementObj = CBORObject.DecodeFromBytes(deviceEngagementBytes);

            // BLE & NFC Server 시작 (개인키와 CBOR 객체를 함께 넘김)
            this.deviceEngagementBytes = deviceEngagementBytes;
            startOfflineServers(bleUuidBytes, privateKey, deviceEngagementBytes);

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 800, 800);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("sangun","Qr generate fail", e);
            Toast.makeText(this, "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startOfflineServers(byte[] bleUuidBytes, PrivateKey privateKey, byte[] deviceEngagementBytes) {
        MdocProximityListener proximityListener = new MdocProximityListener() {
            @Override
            public void onDeviceConnected() {
                runOnUiThread(() -> Toast.makeText(QRGeneratorActivity.this, "Reader connected!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDeviceDisconnected() {
                runOnUiThread(() -> Toast.makeText(QRGeneratorActivity.this, "Reader disconnected.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onRequestReceived(byte[] request) {
                // Here you would process the DeviceRequest and send a Response
                Log.d("ProximityServer", "Received request: " + request.length + " bytes");
                Log.d("sangjun", "Received request: " + MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_16, request));

                try {
                    Log.d("QRGenerator", "전체 요청 수신 완료!");

                    // (중요) 고정된 서명용 개인키 (이 부분은 나중에 보안 저장소로 교체 권장)
                    String PRIVATE_KEY_HEX = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";
                    PrivateKey holderPrivateKey = CryptoUtil.getPrivateKeyObject(Base64.decode(PRIVATE_KEY_HEX, Base64.DEFAULT));

                    // MdocSessionManager를 통해 응답 생성 및 암호화 수행
                    // BLE, NFC, WiFi 중 현재 활성화된 서버의 매니저를 사용 (로직은 동일)
                    byte[] deviceResponse;
                    if (bleServer != null && bleServer.getSessionManager().getEReaderKey() != null) {
                        deviceResponse = bleServer.getSessionManager().generateDeviceResponse(mDoc, selectedKeys, namespaces, holderPrivateKey);
                    } else if (nfcServer != null && nfcServer.getSessionManager().getEReaderKey() != null) {
                        deviceResponse = nfcServer.getSessionManager().generateDeviceResponse(mDoc, selectedKeys, namespaces, holderPrivateKey);
                    } else if (wifiServer != null && wifiServer.getSessionManager().getEReaderKey() != null) {
                        deviceResponse = wifiServer.getSessionManager().generateDeviceResponse(mDoc, selectedKeys, namespaces, holderPrivateKey);
                    } else {
                        Log.e("QRGenerator", "No active session with eReaderKey found.");
                        return;
                    }

                    // 생성된 응답을 다시 서버를 통해 전송
                    if (bleServer != null) bleServer.sendResponse(deviceResponse);
                    if (wifiServer != null) wifiServer.sendResponse(deviceResponse);
                    if (nfcServer != null) nfcServer.sendResponse(deviceResponse);

                } catch (Exception e) {
                    Log.e("QRGenerator", "응답 생성 및 전송 실패", e);
                }
            }
        };

        // BLE 서버 시작
        bleServer = new MdocBleServer(this, bleUuidBytes, privateKey, deviceEngagementBytes);
        bleServer.setListener(proximityListener);
        bleServer.start();

        // NFC 서버 시작
        nfcServer = new MdocNfcServer(privateKey, deviceEngagementBytes, null);
        nfcServer.setListener(proximityListener);
        nfcServer.start();

        //  WiFi Aware 서버 시작
        String wifiPassphrase = org.omnione.did.sdk.mdoc.proximity.holder.core.MdocEngagement.getLastWifiAwarePassphrase();
        wifiServer = new MdocWifiServer(this, privateKey, deviceEngagementBytes, wifiPassphrase);
        wifiServer.setListener(proximityListener);
        wifiServer.start();

        Log.i("QRGenerator", "==== Offline Servers (BLE, NFC & WiFi) Ready ====");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}