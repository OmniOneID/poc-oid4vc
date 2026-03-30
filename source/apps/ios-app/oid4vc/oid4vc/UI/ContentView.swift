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
import CodeScanner

/// The main entry view of the application, providing access to issuance, verification, and settings.
struct ContentView: View {
    @EnvironmentObject var navigationManager: NavigationManager
    @State private var isShowingScanner = false
    
    @State private var manualUriInput: String = ""
    
    @State private var isIssuanceViewActive = false
    @State private var isVerificationViewActive = false
    @State private var isViewVcActive = false
    @State private var isOfflineActive = false
    @State private var activeIssuanceUri: String? 
    @State private var activeVerificationUri: String?
    @State private var offlineData: (mDoc: String, keys: [String], namespaces: [String])?

    /// The user interface body of the content view.
    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack(path: $navigationManager.path) {
                mainContent
                    .navigationDestination(for: NavigationManager.Destination.self) { destination in
                        destinationView(for: destination)
                    }
            }
        } else {
            NavigationView {
                ZStack {
                    if let uri = activeIssuanceUri {
                        NavigationLink(destination: CredentialIssuanceView(credentialOfferUri: uri), isActive: $isIssuanceViewActive) { EmptyView() }
                    }
                    if let uri = activeVerificationUri {
                        NavigationLink(destination: VerifyView(verificationUri: uri), isActive: $isVerificationViewActive) { EmptyView() }
                    }
                    NavigationLink(destination: ViewVcView(), isActive: $isViewVcActive) { EmptyView() }
                    
                    if let data = offlineData {
                        NavigationLink(destination: QRGeneratorView(mDoc: data.mDoc, selectedKeys: data.keys, namespaces: data.namespaces), isActive: $isOfflineActive) { EmptyView() }
                    }
                    
                    mainContent
                }
            }
            .navigationViewStyle(.stack)
        }
    }
    
    /// The primary content of the view, including action buttons.
    private var mainContent: some View {
        VStack(spacing: 20) {
            
            Spacer()
            
            Text("OID4VC Test")
                .font(.largeTitle)
                .fontWeight(.bold)
            
            Button(action: { self.isShowingScanner = true }) {
                Text("Start Issuance")
                    .font(.title)
                    .frame(width: 250)
                    .padding()
                    .background(Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            
            Button(action: {
                navigationManager.path.append(.viewVc)
            }) {
                Text("View VC")
                    .font(.title)
                    .frame(width: 250)
                    .padding()
                    .background(Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            
            NavigationLink("API Test Page", destination: APITestView())
                .font(.title)
                .frame(width: 250)
                .padding()
                .background(Color.gray)
                .foregroundColor(.white)
                .cornerRadius(10)
            
            Spacer()
            
            #if targetEnvironment(simulator)
            simulatorTools
            #endif
            
            
        }
        .sheet(isPresented: $isShowingScanner) {
            CodeScannerView(codeTypes: [.qr], completion: self.handleScan)
        }
        .onAppear {
            if let firstDestination = navigationManager.path.first {
                handleNavigation(to: firstDestination)
            }
        }
        .onChange(of: navigationManager.path) { newPath in
            if let newDestination = newPath.first {
                handleNavigation(to: newDestination)
            }
        }
    }
    
    /// Returns the view corresponding to a given navigation destination.
    /// - Parameter destination: The navigation destination.
    /// - Returns: A view representing the destination.
    @ViewBuilder
    private func destinationView(for destination: NavigationManager.Destination) -> some View {
        switch destination {
        case .issuance(let uri):
            CredentialIssuanceView(credentialOfferUri: uri)
        case .verification(let uri, let keys, let namespaces):
            VerifyView(verificationUri: uri, selectedClaimsKeys: keys, selectedClaimsNamespaces: namespaces)
        case .mdocOffline(let mDoc, let keys, let namespaces):
            QRGeneratorView(mDoc: mDoc, selectedKeys: keys, namespaces: namespaces)
        case .viewVc:
            ViewVcView()
        }
    }
    
    /// Handles navigation for backward compatibility with iOS 15.
    /// - Parameter destination: The navigation destination.
    private func handleNavigation(to destination: NavigationManager.Destination) {
        if #available(iOS 16.0, *) {
        } else {
            switch destination {
            case .issuance(let uri):
                activeIssuanceUri = uri
                isIssuanceViewActive = true
            case .verification(let uri, _, _):
                activeVerificationUri = uri
                isVerificationViewActive = true
            case .mdocOffline(let mDoc, let keys, let namespaces):
                self.offlineData = (mDoc, keys, namespaces)
                self.isOfflineActive = true
            case .viewVc:
                isViewVcActive = true
            }
            if !navigationManager.path.isEmpty {
                navigationManager.path.removeFirst()
            }
        }
    }
    
    /// A view containing tools specifically for the simulator environment.
    private var simulatorTools: some View {
        VStack {
            Text("Enter QR Code Data")
                .font(.caption)
            TextField("QR Data", text: $manualUriInput)
                .textFieldStyle(.roundedBorder)
                .padding()
            Button("Issue") {
                if let url = URL(string: manualUriInput) {
                    navigationManager.handle(url: url)
                    manualUriInput = ""
                }
            }
            .padding()
        }
    }
    
    /// Handles the result of a QR code scan.
    /// - Parameter result: The result of the scan operation.
    private func handleScan(result: Result<ScanResult, ScanError>) {
        self.isShowingScanner = false
        switch result {
        case .success(let result):
            if let url = URL(string: result.string) {
                navigationManager.handle(url: url)
            }
        case .failure(let error):
            print("Scanning failed: \(error.localizedDescription)")
        }
    }
}

/// A preview provider for the ContentView.
struct ContentView_Previews: PreviewProvider {
    /// Provides previews for the ContentView.
    static var previews: some View {
        ContentView()
            .environmentObject(NavigationManager())
    }
}
