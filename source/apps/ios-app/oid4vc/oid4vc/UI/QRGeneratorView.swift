/*
 * Copyright 2025 - 2026 OmniOne.
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

import SwiftUI
import CoreImage
import CryptoKit

/// SwiftUI view for generating and displaying an ISO 18013-5 engagement QR code.
struct QRGeneratorView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var navigationManager: NavigationManager
    
    @State private var qrImage: UIImage?
    @State private var bleServer: MdocBleServer?
    @State private var deviceEngagementBytes: [UInt8] = []
    
    let mDoc: String
    let selectedKeys: [String]
    let namespaces: [String]
    
    private let fixedPrivateKeyBase64 = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"

    var body: some View {
        VStack(spacing: 20) {
            Text("Offline Presentation")
                .font(.title2)
                .bold()
                .padding(.top)
            
            Text("Reader should scan this QR code")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            if let qrImage = qrImage {
                Image(uiImage: qrImage)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 300, height: 300)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(radius: 5)
            } else {
                ProgressView("Generating QR Code...")
            }
            
            Spacer()
            
            Button(action: {
                handleClose()
            }) {
                Text("Close")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
        .navigationBarHidden(true)
        .onAppear {
            print("DEBUG: [QRGeneratorView] onAppear triggered.")
            generateQRAndStartServer()
        }
        .onDisappear {
            print("DEBUG: [QRGeneratorView] onDisappear triggered. Stopping BLE server.")
            bleServer?.stop()
        }
    }
    
    private func handleClose() {
        print("DEBUG: [QRGeneratorView] Close button tapped.")
        if #available(iOS 16.0, *) {
            if !navigationManager.path.isEmpty {
                print("DEBUG: [QRGeneratorView] Popping from NavigationStack.")
                navigationManager.path.removeLast()
            } else {
                dismiss()
            }
        } else {
            print("DEBUG: [QRGeneratorView] Dismissing via Environment.")
            dismiss()
        }
    }
    
    private func generateQRAndStartServer() {
        print("DEBUG: [QRGeneratorView] Generating Engagement QR...")
        let keyHolder = Mdoc.generateEDeviceKeyBytes()
        let eDeviceKeyBytes = keyHolder.eDeviceKeyBytes
        let privateKey = keyHolder.privateKey
        
        let bleUuidBytes: [UInt8] = [
            0x00, 0x00, 0xff, 0xf0,
            0x00, 0x00, 0x10, 0x00,
            0x80, 0x00, 0x00, 0x80,
            0x5f, 0x9b, 0x34, 0xfb
        ]
        
        let qrData = Mdoc.createDeviceEngagementPayload(eDeviceKeyBytes: eDeviceKeyBytes, bleUuidBytes: bleUuidBytes)
        print("DEBUG: [QRGeneratorView] QR Data: \(qrData)")
        
        let base64UrlPayload = qrData.replacingOccurrences(of: "mdoc:", with: "")
        if let decoded = Base64URL.decode(base64UrlPayload) {
            self.deviceEngagementBytes = [UInt8](decoded)
        }
        
        self.qrImage = generateQRCode(from: qrData)
        
        startOfflineServer(bleUuidBytes: bleUuidBytes, privateKey: privateKey, deviceEngagementBytes: self.deviceEngagementBytes)
    }
    
    private func startOfflineServer(bleUuidBytes: [UInt8], privateKey: P256.KeyAgreement.PrivateKey, deviceEngagementBytes: [UInt8]) {
        let server = MdocBleServer(bleUuidBytes: bleUuidBytes, privateKey: privateKey, deviceEngagementBytes: deviceEngagementBytes)
        
        let listener = MdocInternalProximityListener(
            mDoc: mDoc,
            selectedKeys: selectedKeys,
            namespaces: namespaces,
            fixedPrivateKeyBase64: fixedPrivateKeyBase64,
            bleServer: server
        )
        
        server.setListener(listener)
        server.start()
        self.bleServer = server
    }
    
    private func generateQRCode(from string: String) -> UIImage? {
        let data = string.data(using: .utf8)
        
        if let filter = CIFilter(name: "CIQRCodeGenerator") {
            filter.setValue(data, forKey: "inputMessage")
            filter.setValue("H", forKey: "inputCorrectionLevel")
            
            if let outputImage = filter.outputImage {
                let transform = CGAffineTransform(scaleX: 10, y: 10)
                let scaledImage = outputImage.transformed(by: transform)
                
                let context = CIContext()
                if let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) {
                    return UIImage(cgImage: cgImage)
                }
            }
        }
        return nil
    }
}

/// Internal class to handle proximity events, avoiding capturing self in closures.
class MdocInternalProximityListener: MdocProximityListener {
    let mDoc: String
    let selectedKeys: [String]
    let namespaces: [String]
    let fixedPrivateKeyBase64: String
    weak var bleServer: MdocBleServer?
    
    init(mDoc: String, selectedKeys: [String], namespaces: [String], fixedPrivateKeyBase64: String, bleServer: MdocBleServer) {
        self.mDoc = mDoc
        self.selectedKeys = selectedKeys
        self.namespaces = namespaces
        self.fixedPrivateKeyBase64 = fixedPrivateKeyBase64
        self.bleServer = bleServer
    }
    
    func onDeviceConnected() {
        print("DEBUG: [MdocListener] Reader connected!")
    }
    func onDeviceDisconnected() {
        print("DEBUG: [MdocListener] Reader disconnected.")
    }
    
    func onRequestReceived(request: [UInt8]) {
        guard let server = bleServer else { 
            print("ERROR: [MdocListener] BleServer is nil.")
            return 
        }
        print("DEBUG: [MdocListener] Request received, processing DeviceResponse...")
        
        do {
            guard let holderPrivateKey = CryptoUtil.getPrivateKeyFromBase64(fixedPrivateKeyBase64) else {
                print("ERROR: [MdocListener] Failed to load holder private key.")
                return
            }
            
            let deviceResponse = try server.getSessionManager().generateDeviceResponse(
                mDoc: mDoc,
                selectedClaimsKeys: selectedKeys,
                selectedClaimsNamespaces: namespaces,
                holderPrivateKey: holderPrivateKey
            )
            
            print("DEBUG: [MdocListener] DeviceResponse generated successfully (\(deviceResponse.count) bytes). Sending...")
            server.sendResponse(response: deviceResponse)
        } catch {
            print("ERROR: [MdocListener] DeviceResponse generation/sending failed: \(error)")
        }
    }
}
