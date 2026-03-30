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
import CryptoKit

/// A view that handles the verification process of a credential.
struct VerifyView: View {
    let verificationUri: String
    
    @State private var infoText: String = "Preparing..."
    @State private var authorizationRequestState: String?
    @State private var nonce: String?
    @State private var aud: String?
    @State private var responseUri: String?
    @State private var selectedClaimsKeys: [String]?
    @State private var selectedClaimsNamespaces: [String]?
    
    /// Initializes a new instance of VerifyView.
    /// - Parameters:
    ///   - verificationUri: The URI used for verification.
    ///   - selectedClaimsKeys: Optional keys of selected claims.
    ///   - selectedClaimsNamespaces: Optional namespaces of selected claims.
    init(verificationUri: String, selectedClaimsKeys: [String]? = nil, selectedClaimsNamespaces: [String]? = nil) {
        self.verificationUri = verificationUri
        self._selectedClaimsKeys = State(initialValue: selectedClaimsKeys)
        self._selectedClaimsNamespaces = State(initialValue: selectedClaimsNamespaces)
    }
    
    /// Represents the different states of the verification process.
    enum VerificationState {
        case processing
        case completed(message: String)
        case failed(error: String)
    }
    
    @State private var state: VerificationState = .processing
    private let apiService = APIService()
    
    /// The user interface body of the verification view.
    var body: some View {
        VStack(spacing: 20) {
            switch state {
            case .processing:
                ProgressView()
                Text(infoText)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .padding()
            case .completed(let message):
                Image(systemName: "checkmark.circle.fill")
                    .font(.largeTitle)
                    .foregroundColor(.green)
                Text("Verification Successful!")
                    .font(.headline)
                ScrollView {
                    Text(message)
                        .font(.caption)
                        .padding()
                }
            case .failed(let error):
                Image(systemName: "xmark.circle.fill")
                    .font(.largeTitle)
                    .foregroundColor(.red)
                Text("Verification Failed")
                    .font(.headline)
                Text(error)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding()
            }
        }
        .navigationTitle("Verifying Credential")
        .onAppear(perform: startVerification)
    }
    
    /// Initiates the verification process.
    private func startVerification() {
        handleIntent(uriString: verificationUri)
    }

    /// Processes the incoming URI and extracts request data.
    /// - Parameter uriString: The URI string to be processed.
    private func handleIntent(uriString: String) {
        guard let url = URL(string: uriString),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
              let queryItems = components.queryItems else {
            statusUpdate("Error: Deep link data not found.")
            state = .failed(error: "Invalid verification URI")
            return
        }
        
        if let requestUri = queryItems.first(where: { $0.name == "request_uri" })?.value {
            LogUtil.logLongString("sangjun", "OID4VP Request URI: \(requestUri)")
            statusUpdate("OID4VP Request URI: \(requestUri)")
            step1_fetchAuthorizationRequest(requestUrl: requestUri)
        } else {
            statusUpdate("Error: request_uri not found.")
            state = .failed(error: "Missing request_uri")
        }
    }

    /// Fetches the Authorization Request Object from the Verifier.
    /// - Parameter requestUrl: The URL from which to fetch the authorization request.
    private func step1_fetchAuthorizationRequest(requestUrl: String) {
        statusUpdate("Fetching Authorization Request...")
        
        Task {
            do {
                let requestData = try await apiService.getAuthorizationRequest(url: requestUrl)
                if let responseString = String(data: requestData, encoding: .utf8) {
                    try await step2_processRequestAndGenerateVp(responseData: responseString)
                } else {
                    throw NSError(domain: "VerifyView", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to decode response data"])
                }
            } catch {
                statusUpdate("Error: A problem occurred while fetching authorization request.")
                state = .failed(error: error.localizedDescription)
            }
        }
    }

    /// Parses the authorization request, extracts metadata, and initiates VP token generation.
    /// - Parameter responseData: The raw response data from the authorization request.
    private func step2_processRequestAndGenerateVp(responseData: String) async throws {
        statusUpdate("Processing Request and Generating VP...")
        
        var responseDataString = responseData
        if responseData.components(separatedBy: ".").count == 3 {
             let components = responseData.components(separatedBy: ".")
             if let payloadData = Base64URL.decode(components[1]),
                let payloadString = String(data: payloadData, encoding: .utf8) {
                 responseDataString = payloadString
             }
        }
        
        self.responseUri = LogUtil.extractJsonField(responseDataString, "response_uri")
        self.authorizationRequestState = LogUtil.extractJsonField(responseDataString, "state")
        self.nonce = LogUtil.extractJsonField(responseDataString, "nonce")
        self.aud = LogUtil.extractJsonField(responseDataString, "client_id")
        
        guard let responseUri = self.responseUri,
              let state = self.authorizationRequestState else {
            statusUpdate("Error: Required fields not found in the response.")
            return
        }

        do {
            let vpToken = try generateVpToken()
            step3_submitVpToken(responseUri: responseUri, vpToken: vpToken, state: state)
        } catch {
            statusUpdate("Error: Failed to generate VP Token.")
            self.state = .failed(error: error.localizedDescription)
        }
    }

    /// Submits the generated VP token to the verifier's response URI.
    /// - Parameters:
    ///   - responseUri: The URI to which the VP token is submitted.
    ///   - vpToken: The generated VP token string.
    ///   - state: The state associated with the authorization request.
    private func step3_submitVpToken(responseUri: String, vpToken: String, state: String) {
        statusUpdate("Submitting VP Token to Verifier...")
        
        Task {
            do {
                let responseBody = try await apiService.postVpToken(url: responseUri, vpToken: vpToken, state: state)
                statusUpdate("VC submission complete! : \(responseBody)")
                self.state = .completed(message: responseBody)
            } catch {
                statusUpdate("Error: VP Token submission failed.")
                self.state = .failed(error: error.localizedDescription)
            }
        }
    }

    /// Generates a VP token based on the stored VC and requested format.
    /// - Returns: A string representing the generated VP token.
    private func generateVpToken() throws -> String {
        let walletDataList = try loadVcFile()
        guard let walletData = walletDataList.first else { return "" }
        
        let format = walletData.format
        let credential = walletData.credential
        let nonce = self.nonce ?? ""
        let aud = self.aud ?? ""

        if OpenDid.isSupported(format: format) {
            return OpenDid.createVpToken(nonce: nonce, aud: aud, vcCredential: credential)
        } else if SDJWT.isSupported(format: format) {
            return try SDJWT.createVpToken(sdJwtVc: credential, selectedClaimsKeys: selectedClaimsKeys, aud: aud, nonce: nonce)
        } else if Mdoc.isSupported(format: format) {
            return try Mdoc.createVpToken(mDoc: credential, selectedClaimsKeys: selectedClaimsKeys, selectedClaimsNamespaces: selectedClaimsNamespaces, aud: aud, nonce: nonce, responseUri: self.responseUri ?? "")
        }
        
        return ""
    }

    /// Loads the Verifiable Credential from the local file system.
    /// - Returns: An array of WalletData containing the VC.
    private func loadVcFile() throws -> [WalletData] {
        let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        let fileURL = documentsDirectory.appendingPathComponent("vc.json")
        let data = try Data(contentsOf: fileURL)
        return try JSONDecoder().decode([WalletData].self, from: data)
    }

    /// Updates the status message displayed in the user interface.
    /// - Parameter message: The new status message.
    private func statusUpdate(_ message: String) {
        self.infoText = message
    }
}
