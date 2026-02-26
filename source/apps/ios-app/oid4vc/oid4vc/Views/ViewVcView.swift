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

enum SdJwtParsingError: Error, LocalizedError {
    case invalidFormat(description: String)
    case base64DecodingFailed
    case jsonParsingFailed(description: String)
    
    var errorDescription: String? {
        switch self {
        case .invalidFormat(let desc):
            return "Invalid SD-JWT format: \(desc)"
        case .base64DecodingFailed:
            return "Base64URL decoding failed."
        case .jsonParsingFailed(let desc):
            return "JSON parsing failed: \(desc)"
        }
    }
}

private func decodeBase64URL(_ base64UrlString: String) throws -> Data {
    var base64 = base64UrlString
        .replacingOccurrences(of: "-", with: "+")
        .replacingOccurrences(of: "_", with: "/")
    
    let padding = base64.count % 4
    if padding != 0 {
        base64.append(String(repeating: "=", count: 4 - padding))
    }
    
    guard let data = Data(base64Encoded: base64) else {
        throw SdJwtParsingError.base64DecodingFailed
    }
    return data
}

struct ViewVcView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var navigationManager: NavigationManager
    
    enum VcState {
        case loading
        case loaded(claims: [ClaimViewModel])
        case error(message: String)
    }
    
    @State private var vcState: VcState = .loading
    @State private var isShowingDeleteAlert = false
    @State private var isShowingScanner = false
    
    // For simulator
    @State private var manualUriInput: String = ""

    var body: some View {
        VStack {
            switch vcState {
            case .loading:
                ProgressView("Loading VC...")
                Spacer()
            case .loaded(let claims):
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        ForEach(claims) { claim in
                            ClaimView(claim: claim)
                        }
                    }
                    .padding()
                }
                Spacer()
                
                #if targetEnvironment(simulator)
                simulatorTools
                #endif
                
                bottomButtons
            case .error(let message):
                Text(message)
                    .foregroundColor(.red)
                    .padding()
                Spacer()
            }
        }
        .navigationTitle("Verifiable Credential")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { isShowingDeleteAlert = true }) {
                    Image(systemName: "trash")
                }
            }
        }
        .onAppear(perform: loadVcFile)
        .alert("Delete VC File", isPresented: $isShowingDeleteAlert) {
            Button("Delete", role: .destructive, action: deleteVcFile)
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("Are you sure you want to delete the VC file?")
        }
        .sheet(isPresented: $isShowingScanner) {
            CodeScannerView(codeTypes: [.qr]) { response in
                if case .success(let result) = response {
                    handleSubmit(scannedUrl: result.string)
                    isShowingScanner = false
                }
            }
        }
    }

    private var simulatorTools: some View {
        VStack {
            Text("Enter QR Code Data")
                .font(.caption)
            TextField("QR Data", text: $manualUriInput)
                .textFieldStyle(.roundedBorder)
                .padding()
            Button("Submit") {
                handleSubmit(scannedUrl: manualUriInput)
            }
            .padding()
        }
    }
    
    private var bottomButtons: some View {
        Button(action: { isShowingScanner = true }) {
            Text("Submit with QR Code")
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.green)
                .foregroundStyle(.white)
                .cornerRadius(10)
        }
        .padding()
    }

    private func loadVcFile() {
        do {
            let fileURL = try getVcFileUrl()
            let data = try Data(contentsOf: fileURL)
            let walletDataArray = try JSONDecoder().decode([WalletData].self, from: data)
            
            guard let walletData = walletDataArray.first else {
                vcState = .error(message: "No saved VC found.")
                return
            }
            parseAndDisplay(walletData: walletData)
            
        } catch { 
            vcState = .error(message: "Failed to load or parse VC file: \(error.localizedDescription)")
        }
    }
    
    private func parseAndDisplay(walletData: WalletData) {
        print("--- response.format ---")
        print(walletData.format)
        print("--------------------------------")
        if walletData.format.contains("NationalID") || walletData.format.contains("mDL") || walletData.format.contains("NationalIDCert") {
            do {
                let sdJwtString = walletData.credential
                let claimsDictionary = try manualParseSdJwt(sdJwtString: sdJwtString)
                let claimViewModels = createClaimViewModels(from: claimsDictionary)
                vcState = .loaded(claims: claimViewModels)
                
            } catch {
                vcState = .error(message: "SD-JWT parsing failed: \(error.localizedDescription)")
            }

        } else if walletData.format.contains("TEC") || walletData.format.contains("UCR") {
            
            let credentialString = walletData.credential

            print("--- Base64 Decoding ---")
            print("Format: \(walletData.format)")
            print("Input String Length: \(credentialString.count)")
            print("------------------------------------")

            do {
                let decodedData = try decodeBase64URL(credentialString)
                 
                let vc = try JSONDecoder().decode(VerifiableCredential.self, from: decodedData)
                let claims = vc.credentialSubject.claims.map { ClaimViewModel(key: $0.caption, value: .string($0.value)) }
                vcState = .loaded(claims: claims)
            } catch {
                vcState = .error(message: "Failed to parse JSON for TEC/UCR format: \(error.localizedDescription)")
            }
        } else {
            vcState = .error(message: "Unsupported VC format: \(walletData.format)")
        }
    }
    
    private func manualParseSdJwt(sdJwtString: String) throws -> [String: Any] {
           let components = sdJwtString.components(separatedBy: "~")
           guard let jwtPart = components.first, components.count > 1 else {
               throw SdJwtParsingError.invalidFormat(description: "JWT and Disclosures must be separated by '~'")
           }
           
           let jwtComponents = jwtPart.components(separatedBy: ".")
           guard jwtComponents.count == 3 else {
               throw SdJwtParsingError.invalidFormat(description: "JWT part must have 3 components (header.payload.signature)")
           }
           let payloadBase64Url = jwtComponents[1]
           
           var allClaims = [String: Any]()
           
           let payloadData = try decodeBase64URL(payloadBase64Url)
           if let payloadJson = try JSONSerialization.jsonObject(with: payloadData) as? [String: Any] {
               for (key, value) in payloadJson {
                   if !["_sd", "cnf", "iss", "iat", "exp", "vct", "status"].contains(key) {
                       allClaims[key] = value
                   }
               }
           } else {
               throw SdJwtParsingError.jsonParsingFailed(description: "JWT payload is not a valid JSON object.")
           }
           
           let disclosureParts = components.dropFirst()
           for disclosureBase64Url in disclosureParts {
               if disclosureBase64Url.isEmpty { continue }
               
               let disclosureData = try decodeBase64URL(disclosureBase64Url)
               if let disclosureArray = try JSONSerialization.jsonObject(with: disclosureData) as? [Any],
                  disclosureArray.count == 3,
                  let claimName = disclosureArray[1] as? String {
                   
                   let claimValue = disclosureArray[2]
                   allClaims[claimName] = claimValue
               } else {
                   throw SdJwtParsingError.jsonParsingFailed(description: "A disclosure is not a valid 3-element JSON array.")
               }
           }
           
           return allClaims
       }
    
    private func deleteVcFile() {
        do {
            let fileURL = try getVcFileUrl()
            try FileManager.default.removeItem(at: fileURL)
            dismiss()
        } catch {
            vcState = .error(message: "Failed to delete VC file.")
        }
    }
    
    private func handleSubmit(scannedUrl: String) {
        guard let url = URL(string: scannedUrl) else { return }
        UIApplication.shared.open(url)
    }
    
    private func getVcFileUrl() throws -> URL {
        let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        return documentsDirectory.appendingPathComponent("vc.json")
    }
    
    private func createClaimViewModels(from dictionary: [String: Any]) -> [ClaimViewModel] {
        var viewModels: [ClaimViewModel] = []
        for key in dictionary.keys.sorted() {
            if let value = dictionary[key] {
                viewModels.append(ClaimViewModel(key: key, value: convertToClaimValue(value)))
            }
        }
        return viewModels
    }

    private func convertToClaimValue(_ anyValue: Any) -> ClaimValue {
        if let stringValue = anyValue as? String {
            return .string(stringValue)
        } else if let intValue = anyValue as? Int {
            return .string(String(intValue))
        } else if let boolValue = anyValue as? Bool {
            return .string(String(boolValue))
        } else if let dictionaryValue = anyValue as? [String: Any] {
            let subViewModels = createClaimViewModels(from: dictionaryValue)
            return .dictionary(subViewModels)
        } else if let arrayValue = anyValue as? [Any] {
            let subViewModels = arrayValue.map { ClaimViewModel(key: "", value: convertToClaimValue($0)) }
            return .array(subViewModels)
        } else {
            return .string(String(describing: anyValue))
        }
    }
}

// MARK: - Recursive Claim View

struct ClaimViewModel: Identifiable {
    let id = UUID()
    let key: String
    let value: ClaimValue
}

enum ClaimValue {
    case string(String)
    case dictionary([ClaimViewModel])
    case array([ClaimViewModel])
    case any(Any)
}

struct ClaimView: View {
    let claim: ClaimViewModel
    let indentLevel: Int
    
    init(claim: ClaimViewModel, indentLevel: Int = 0) {
        self.claim = claim
        self.indentLevel = indentLevel
    }
    
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Text(claim.key.capitalized)
                    .font(.caption)
                    .foregroundStyle(.gray)
                Spacer()
            }
            
            valueView
                .padding(.leading, CGFloat(indentLevel * 20))
        }
    }
    
    @ViewBuilder
    private var valueView: some View {
        switch claim.value {
        case .string(let str):
            Text(str)
                .font(.headline)
                .fontWeight(.bold)
        case .dictionary(let claims):
            ForEach(claims) { subClaim in
                ClaimView(claim: subClaim, indentLevel: indentLevel + 1)
            }
        case .array(let claims):
            ForEach(claims) { subClaim in
                ClaimView(claim: subClaim, indentLevel: indentLevel + 1)
            }
        case .any(let anyValue):
            Text(String(describing: anyValue))
                .font(.headline)
                .fontWeight(.bold)
        }
    }

}
