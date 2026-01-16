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

import SwiftUI
import CodeScanner

struct ContentView: View {
    @EnvironmentObject var navigationManager: NavigationManager
    @State private var isShowingScanner = false
    
    // For simulator
    @State private var manualUriInput: String = ""
    
    // For iOS 15 navigation
    @State private var isIssuanceViewActive = false
    @State private var isVerificationViewActive = false
    @State private var activeIssuanceUri: String? 
    @State private var activeVerificationUri: String?

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
                    
                    mainContent
                }
            }
            .navigationViewStyle(.stack)
        }
    }
    
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
            
            NavigationLink("View VC", destination: ViewVcView())
                .font(.title)
                .frame(width: 250)
                .padding()
                .background(Color.gray)
                .foregroundColor(.white)
                .cornerRadius(10)
            
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
    
    @ViewBuilder
    private func destinationView(for destination: NavigationManager.Destination) -> some View {
        switch destination {
        case .issuance(let uri):
            CredentialIssuanceView(credentialOfferUri: uri)
        case .verification(let uri):
            VerifyView(verificationUri: uri)
        }
    }
    
    // Handles navigation for iOS 15 by setting state variables
    private func handleNavigation(to destination: NavigationManager.Destination) {
        if #available(iOS 16.0, *) {
            // iOS 16 uses path-based navigation, no extra action needed
        } else {
            switch destination {
            case .issuance(let uri):
                activeIssuanceUri = uri
                isIssuanceViewActive = true
            case .verification(let uri):
                activeVerificationUri = uri
                isVerificationViewActive = true
            }
            navigationManager.path.removeFirst()
        }
    }
    
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
                }
            }
            .padding()
        }
    }
    
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

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(NavigationManager())
    }
}
