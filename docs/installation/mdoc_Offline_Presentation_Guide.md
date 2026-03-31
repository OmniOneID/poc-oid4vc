# ISO 18013-5 Offline Presentation Installation & Test Guide

This document describes how to install the Wallet (Holder) and mDoc Reader (Verifier) apps and test proximity offline presentation via BLE/NFC. It covers both Android and iOS environments.

> **Prerequisites**: Since the mDoc issuance process via OID4VCI is included, the backend server (Issuer Server) must be running beforehand. Refer to Section 4 of the [OID4VC Installation Guide](oid4vc_Installation_Guide.md) for server setup instructions.

---

## 1. System Requirements

### 1.1. Android

| Category | Details |
| :--- | :--- |
| **OS** | Android 8.0 (API 26) or higher |
| **Language** | Java 21 |
| **IDE** | Android Studio Hedgehog (2023.1.1) or higher |
| **Build System** | Gradle 8.2 / AGP 8.2.1 |
| **Test Environment** | **2 physical devices** (BLE/NFC required) |

| Category | Wallet (Holder) | Reader (Verifier) |
| :--- | :--- | :--- |
| **App Path** | `source/apps/android-app` | `source/apps/android-mdoc-reader` |
| **Required Hardware** | BLE, NFC (HCE) | BLE, NFC, Camera |
| **Optional Hardware** | WiFi Aware | WiFi Aware |

### 1.2. iOS

| Category | Details |
| :--- | :--- |
| **OS** | iOS 15.0 or higher |
| **Language** | Swift 5.0 |
| **IDE** | Xcode 16.2 or higher |
| **Package Manager** | Swift Package Manager (SPM) |
| **Test Environment** | **2 physical iPhone devices** (BLE required) |

| Category | Wallet (Holder) | Reader (Verifier) |
| :--- | :--- | :--- |
| **App Path** | `source/apps/ios-app` | `source/apps/ios-mdoc-reader` |
| **Xcode Project** | `oid4vc/oid4vc.xcodeproj` | `mdocReader-ios/mdocReader.xcodeproj` |
| **Required Hardware** | BLE | BLE, Camera |

> **Important**: BLE/NFC cannot be used on emulators/simulators, so **physical devices** are required for offline presentation testing. However, the OID4VCI issuance step can be performed on emulators/simulators.

---

## 2. Wallet App Installation (Holder)

### 2.1. Android

#### 2.1.1. Open Project

1. Launch Android Studio.
2. Select **File → Open** and open the `source/apps/android-app` folder.
3. Wait for Gradle synchronization to complete.

#### 2.1.2. Build & Install

**Using IDE (Recommended)**:
1. Connect the Android device (Holder role) via USB.
2. Click the green ▶ button in the toolbar to build and install.

**Command Line**:
```bash
cd source/apps/android-app

# Grant Gradle Wrapper execution permission
chmod 755 ./gradlew

# Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

#### 2.1.3. Grant Permissions

The following permissions must be granted on first launch:
- **Camera** — QR code scanning
- **Location** — BLE / WiFi Aware usage
- **Bluetooth** — BLE communication
- **Nearby Devices** — Android 13 and above

### 2.2. iOS

#### 2.2.1. Open Project

1. Launch Xcode.
2. Select **File → Open** and open the `source/apps/ios-app/oid4vc/oid4vc.xcodeproj` file.
3. Wait for Xcode to automatically download SPM dependencies.

#### 2.2.2. Build & Install

1. Connect the iPhone (Holder role) via USB.
2. Select the target device at the top of Xcode.
3. Press **Product → Run** (or `Cmd + R`) to build and install.

> **Signing**: You may need to configure Signing & Capabilities on the first build. Change the Team to your Apple Developer account and modify the Bundle Identifier if needed.

#### 2.2.3. Grant Permissions

The following permissions must be granted on first launch:
- **Camera** — QR code scanning
- **Bluetooth** — BLE offline presentation

#### 2.2.4. SPM Dependencies

| Package | Version | Description |
| :--- | :--- | :--- |
| CodeScanner | 2.5.2 | QR code scanning (SwiftUI) |
| SwiftCBOR | master | CBOR encoding/decoding (ISO 18013-5) |

#### 2.2.5. Server Connection Settings

The API server address for the Wallet app is configured in the `baseURL` of `Network/APIService.swift`.

> **Note**: You can use `localhost` directly on the simulator, but on a physical device, you must use the IP address on the same network.

---

## 3. mDoc Reader App Installation (Verifier)

### 3.1. Android

#### 3.1.1. Open Project

1. Open the `source/apps/android-mdoc-reader` folder in a separate Android Studio window.
2. Wait for Gradle synchronization to complete.

#### 3.1.2. Build & Install

**Using IDE (Recommended)**:
1. Connect the Android device (Verifier role) via USB.
2. Click the green ▶ button in the toolbar to build and install.

**Command Line**:
```bash
cd source/apps/android-mdoc-reader

# Grant Gradle Wrapper execution permission
chmod 755 ./gradlew

# Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

#### 3.1.3. Grant Permissions

The following permissions must be granted on first launch:
- **Camera** — QR code scanning
- **Location** — BLE usage
- **Bluetooth** — BLE communication
- **Nearby Devices** — Android 13 and above

#### 3.1.4. Issuer Certificate Configuration

The Reader app verifies the issuer certificate chain based on Root CA certificates included in the `app/src/main/assets/certs/` directory.

| Certificate File | Purpose |
| :--- | :--- |
| `x509_rootca.crt` | Default Root CA |
| `x509_eudi_age_verification_issuer_ca01_test_rootca.crt` | EUDI Test Root CA |
| `x509_oidf_test_cert.crt` | OIDF Test Certificate |

> If using a custom Issuer, add the corresponding Issuer's Root CA certificate to this directory.

### 3.2. iOS

#### 3.2.1. Open Project

1. Open the `source/apps/ios-mdoc-reader/mdocReader-ios/mdocReader.xcodeproj` file in a separate Xcode window.
2. Wait for SPM dependencies to be automatically downloaded.

#### 3.2.2. Build & Install

1. Connect the iPhone (Verifier role) via USB.
2. Select the target device at the top of Xcode.
3. Press **Product → Run** (or `Cmd + R`) to build and install.

> **Signing**: As with the Wallet app, Team and Bundle Identifier configuration may be required.

#### 3.2.3. Grant Permissions

The following permissions must be granted on first launch:
- **Camera** — Device Engagement QR code scanning
- **Bluetooth** — BLE communication

#### 3.2.4. SPM Dependencies

| Package | Version | Description |
| :--- | :--- | :--- |
| CodeScanner | 2.5.2 | QR code scanning |
| SwiftCBOR | master | CBOR encoding/decoding |

---

## 4. mDoc Issuance (OID4VCI)

To perform an offline presentation, an mDoc Credential must first be issued to the Wallet app.

> **Note**: This step follows the same OID4VCI flow as Section 6.1 of the [OID4VC Installation Guide](oid4vc_Installation_Guide.md).

### 4.1. Generate Credential Offer (Issuer Server)

1. Access the Issuer Server test page in a browser.

```
http://<IP>:8080/oid4vci/test
```

2. Enter a `User ID` and click **Generate QR Code** to generate a Credential Offer QR code.

### 4.2. Perform Issuance in Wallet App

#### Android

1. On the app's main screen, tap **Scan QR** to scan the QR code.
   - If using an emulator or QR scanning is difficult, copy the `Full Offer URI` and paste it into the `Enter QR Code Data` field at the bottom, then tap **Issue**.
2. When the PIN entry screen appears, enter the 4-digit `Tx Code` shown on the Issuer test page.
3. In the `Select Issued Credential` popup, select **mDL** or **PID** and tap **CONFIRM**.
4. Once issuance is complete, you can verify the issued mDoc in **View VC**.

#### iOS

1. On the app's main screen, tap **Start Issuance** to scan the QR code.
   - On the simulator, paste the `Full Offer URI` into the input field at the bottom.
2. When the PIN entry screen appears, enter the 4-digit `Tx Code` shown on the Issuer test page.
3. On the credential selection screen, select **mDL** or **PID**.
4. Once issuance is complete, you can verify the issued mDoc in **View VC**.

---

## 5. Offline Presentation Test

Once issuance is complete, you can test proximity presentation between two devices without an internet connection.

> **Note**: Cross-platform combinations such as Android Wallet ↔ iOS Reader and iOS Wallet ↔ Android Reader also work via BLE.

### 5.1. [Wallet] Generate Device Engagement QR

1. In the Wallet app, tap **View VC** to view the issued mDoc.
2. Tap the **Offline Present** button to generate a Device Engagement QR code.
3. When the QR code is displayed, the app automatically starts the BLE Peripheral server and waits for the Reader to connect.

> On Android, BLE, NFC HCE, and WiFi Aware servers start simultaneously. On iOS, only the BLE server starts.

### 5.2. [Reader] Scan QR & Request Documents

1. In the mDoc Reader app, select the document type (mDL, PID, etc.) and claims to request.
2. Tap **QR Scan** (Android) or **SCAN QR CODE** (iOS) to activate the camera and scan the Device Engagement QR code displayed on the Wallet.

> **Using NFC (Android only)**: Instead of scanning a QR code, you can tap the Reader device against the Wallet device to perform NFC Device Engagement.

### 5.3. Session Establishment & Data Transfer

After QR scanning (or NFC tap), the following process is performed automatically:

1. **BLE Connection Establishment** — A BLE connection is set up between the Reader (Central) and Wallet (Peripheral).
2. **Session Encryption** — P256 ECDH key agreement and AES-256-GCM session encryption are performed per the ISO 18013-5 standard.
3. **DeviceRequest Transmission** — The document/claim request selected by the Reader is encrypted and sent to the Wallet.
4. **DeviceResponse Transmission** — The Wallet encrypts and responds with the requested data along with Device Authentication.

### 5.4. [Reader] Verify Results

Once the transfer is complete, the Reader app verifies and displays the following:

- **Issuer Certificate Chain / Issuer Signature Verification**
- **DeviceAuth Signature Verification** (COSE_Sign1)
- **Data Integrity Verification** (MSO / Data Integrity)
- **Validity Period Check** (validFrom, validUntil)
- **Claim Data Display** (name, date of birth, driver's license information, etc.)

---

## 6. Transport Method Reference

| Transport | Wallet Role | Reader Role | Android | iOS |
| :--- | :--- | :--- | :--- | :--- |
| **BLE** | Peripheral (GATT Server) | Central (GATT Client) | Supported | Supported |
| **NFC** | HCE (Host Card Emulation) | Reader Mode | Supported | Not Supported |
| **WiFi Aware** | Publisher | Subscriber | Supported | Not Supported |

- **BLE**: The default transport method, supported on both Android and iOS.
- **NFC**: Supported on Android only. AID: `A0000002480400`, requires device tap.
- **WiFi Aware**: Supported on Android only, and may not be available on all devices.

---

## 7. Troubleshooting

### 7.1. Common

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| BLE connection not established | Bluetooth disabled or permission not granted | Turn on Bluetooth on both devices and check app permissions (Bluetooth, Location). |
| QR code scan failure | Camera permission not granted or poor QR recognition | Grant camera permission and adjust distance so the QR code is clearly visible. |
| Transfer timeout | Devices too far apart or interference | Place both devices close together (within 1m) and try again. |
| Issuer certificate verification failure | Root CA certificate mismatch | Verify that the correct Root CA certificates are included in the Android Reader's `assets/certs/` directory. |
| mDoc issuance failure | Issuer Server not running or network unreachable | Verify that the Issuer Server is running and accessible from the device. |
| Offline presentation unavailable on emulator/simulator | BLE/NFC not supported | Offline presentation can only be tested on physical devices. |

### 7.2. Android

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| NFC tap not recognized | NFC disabled or HCE not supported | Turn on NFC on both devices and ensure the Wallet app is running in the foreground. |

### 7.3. iOS

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| Build failure due to signing error | Apple Developer account not configured | Set the Team in Xcode → Signing & Capabilities and change the Bundle Identifier to a unique value. |
| SPM dependency download failure | Network issue or cache error | Perform Xcode → File → Packages → Reset Package Caches. |
| Cannot connect to Issuer Server | Server address misconfiguration | Update the `baseURL` in `APIService.swift` to match the current network environment. |

---

## 8. Related Documents

| Document | Description |
| :--- | :--- |
| [OID4VC Installation Guide](oid4vc_Installation_Guide.md) | Backend server setup and OID4VCI/OID4VP test procedures |
| [OID4VC Android App README](../../source/apps/android-app/README.md) | Android Wallet app overview |
| [OID4VC iOS App README](../../source/apps/ios-app/README.md) | iOS Wallet app overview |
| [mDoc Reader Android README](../../source/apps/android-mdoc-reader/README.md) | Android Reader app overview |
| [mDoc Reader iOS README](../../source/apps/ios-mdoc-reader/README.md) | iOS Reader app overview |

---
