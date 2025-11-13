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
import CryptoKit // PKCE
import AuthenticationServices // For custom tabs, etc.

struct SelectionInfo: Identifiable {
    let id = UUID()
    let title: String
    let details: [AuthorizationDetails]
}

private class AuthContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        return UIApplication.shared.windows.first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}

struct CredentialIssuanceView: View {
    let credentialOfferUri: String
    
    @State private var statusText: String = ""
    @State private var showErrorAlert = false
    @State private var errorMessage: String = ""
    @State private var showSuccess = false
    @State private var showPinView = false
    @State private var showSelectionDialog = false
    @State private var isIssuing = true
    
    @State private var credentialOffer: CredentialOfferResponse?
    @State private var issuerMetadata: IssuerMetadataResponse?
    @State private var tokenResponse: TokenResponse?
    @State private var availableCredentialIds: [String] = []
    
    @State private var issuerState: String?
    @State private var pkceCodeVerifier: String?
    @State private var authContextProvider = AuthContextProvider()
    
    @State private var preAuthCode: String?
    @State private var issuerUrl: String?
    @State private var tokenEndpointUrl: String?
    @State private var selectedCredentialConfigurationId: String?
    @State private var issuerSupportedConfigurations: [String: CredentialConfiguration]?
    
    @State private var selectionInfo: SelectionInfo?
    @State private var selectedIdentifier: String?

    private let apiService = APIService()

    var body: some View {
        VStack(spacing: 20) {
            if isIssuing {
                ProgressView()
                Text(statusText)
                    .font(.headline)
                    .padding()
            } else if showSuccess {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.green)
                Text("Credential Issued Successfully!")
            } else {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.red)
                Text("Error")
                    .font(.largeTitle)
                Text(errorMessage)
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .padding()
            }
        }
        .navigationTitle("Credential Issuance")
        .onAppear(perform: startIssuanceFlow)
        .sheet(isPresented: $showPinView) {
            PinView { pin in
                self.showPinView = false
                Task { await step2_getToken(pinCode: pin) }
            }
        }
        .sheet(item: $selectionInfo) { info in
            SelectionView(
                details: info.details,
                selection: $selectedIdentifier,
                onConfirm: {
                    self.selectionInfo = nil
                    Task { await step3_getCredential() }
                }
            )
        }
    }
    
    private func startIssuanceFlow() {
        Task { await step0_fetchCredentialOffer() }
    }

    private func step0_fetchCredentialOffer() async {
        statusText = "Verifying Credential Offer..."
        
        guard let url = URL(string: credentialOfferUri),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
              let queryItems = components.queryItems,
              let offerUriItem = queryItems.first(where: { $0.name == "credential_offer_uri" }),
              let offerUriString = offerUriItem.value,
              let offerUrl = URL(string: offerUriString) else {
            handleFailure(message: "Invalid Credential Offer URI format.")
            return
        }
        
        do {
            let offer: CredentialOfferResponse = try await apiService.get(url: offerUrl)
            self.credentialOffer = offer
            self.issuerUrl = offer.credentialIssuer
            if let preAuth = offer.grants.preAuthorizedCode {
                self.preAuthCode = preAuth.preAuthorizedCode
                            
            } else if let authCode = offer.grants.authorizationCode {
                self.issuerState = authCode.issuerState
                
            } else {
                handleFailure(message: "No valid Grant type in Offer.")
                return
            }
            guard let ids = offer.credentialConfigurationIds, !ids.isEmpty else {
                handleFailure(message: "No issuable Credential ID in Offer.")
                return
            }
            await step1_getIssuerInfo()
            
        } catch {
            handleFailure(message: "Failed to fetch Credential Offer", error: error)
        }
    }
    
    private func step1_getIssuerInfo() async {
        statusText = "Fetching issuer information..."
        guard let issuerUrl = self.issuerUrl else {
            handleFailure(message: "Issuer URL not found.")
            return
        }
        print("Issuer URL: \(issuerUrl)")
        do {
            let metadata: IssuerMetadataResponse = try await apiService.get(endpoint: ".well-known/openid-credential-issuer", url: URL(string: issuerUrl))
            self.issuerMetadata = metadata
            self.tokenEndpointUrl = metadata.tokenEndpoint
            
            if self.preAuthCode != nil {
                self.showPinView = true
            
            // If issuerState exists, it's an Authorization Code flow
            } else if self.issuerState != nil {
                startAuthorizationCodeFlow()
                
            } else {
                handleFailure(message: "Grant Type not found.")
            }
            
            
        } catch {
            handleFailure(message: "Failed to fetch issuer information", error: error)
        }
    }
    
    private func step2_getToken(pinCode: String) async {
        statusText = "Issuing token..."
        
        guard let endpointUrlString = tokenEndpointUrl,
              let endpointUrl = URL(string: endpointUrlString) else {
            handleFailure(message: "Token endpoint URL is invalid or missing.")
            return
        }
        
        guard let preAuthCode = self.preAuthCode,
              let credentialIds = self.credentialOffer?.credentialConfigurationIds else {
            handleFailure(message: "Information required for token request is missing.")
            return
        }
        
        let authDetailsArray = credentialIds.map { id in
            return AuthorizationDetails(type: "openid_credential", credentialConfigurationId: id, credentialIdentifiers: nil)
        }
        
        let tokenRequest = TokenRequest(grantType: "urn:ietf:params:oauth:grant-type:pre-authorized_code", preAuthorizedCode: preAuthCode, txCode: pinCode, authorizationDetails: authDetailsArray)
        
        do {
            let response: TokenResponse = try await apiService.getTokenByPreAuthCode(tokenRequest: tokenRequest, url: endpointUrl)
            self.tokenResponse = response
            guard let details = response.authorizationDetails, !details.isEmpty else {
                handleFailure(message: "Identifier information not found.")
                return
            }
            self.selectionInfo = SelectionInfo(
                title: "Select credential",
                details: details
            )
            
        } catch {
            print("--- Token issuance failed: Detailed error log ---")
            
            if let apiError = error as? APIError {
                print(apiError.localizedDescription)
            } else {
                print(error.localizedDescription)
            }
            print("------------------------------------")
            handleFailure(message: "Token issuance failed", error: error)
        }
    }
    
    private func step3_getCredential() async {
        statusText = "Requesting Credential..."
        
        guard let token = self.tokenResponse?.accessToken else {
            handleFailure(message: "Access Token not found.")
            return
        }
        guard let issuerUrl = self.issuerUrl else {
            handleFailure(message: "Issuer URL not found.")
            return
        }
        do {
            let credentialRequest = try createCredentialRequest()
            let authHeader = "Bearer \(token)"
            let response = try await apiService.getCredential(issuerUrl: issuerUrl, authorization: authHeader, requestBody: credentialRequest)
            try saveCredentialToFile(response)
            handleSuccess()
        } catch {
            handleFailure(message: "Credential issuance failed", error: error)
        }
    }
    
    private func createCredentialRequest() throws -> CredentialRequest {
        let exampleJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        let proof = Proof(proofType: "jwt", jwt: exampleJwt)
        
        guard let identifier = self.selectedIdentifier else {
            throw NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "No Credential Identifier selected."])
        }
        
        return CredentialRequest(credentialIdentifier: identifier, proof: proof)

    }
    
    private func saveCredentialToFile(_ credentialResponse: CredentialResponse) throws {
        let walletData = WalletData(format: self.selectedIdentifier ?? "", credentialResponse: credentialResponse)
        
        let dataToSave = try JSONEncoder().encode([walletData])
        let documentsURL = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        let fileURL = documentsURL.appendingPathComponent("vc.json")
        try dataToSave.write(to: fileURL)
        
        print("✅ Path where VC file is saved:")
        print(fileURL.path)
    }
    
    private func handleSuccess() {
        isIssuing = false
        showSuccess = true
    }
    
    private func handleFailure(message: String, error: Error? = nil) {
        var fullMessage = message
        if let error = error {
            fullMessage += ": \(error.localizedDescription)"
        }
        print(fullMessage)
        self.errorMessage = message
        self.isIssuing = false
        self.showSuccess = false
    }
    

    private func startAuthorizationCodeFlow() {
        statusText = "Opening browser for user authentication..."

        guard let authServerUrlString = issuerMetadata?.authorizationServer?.first,
              let authServerUrl = URL(string: authServerUrlString),
              let state = self.issuerState else {
            handleFailure(message: "Insufficient information for Authorization flow.")
            return
        }

        guard let idsToRequest = self.credentialOffer?.credentialConfigurationIds else {
            handleFailure(message: "No list of Credential IDs to request.")
            return
        }
        
        let codeVerifier = generatePkceCodeVerifier()
        self.pkceCodeVerifier = codeVerifier
        let codeChallenge = generatePkceCodeChallenge(from: codeVerifier)

        Task {
            do {
                let code = try await Authorize.start(
                    authorizationServerUrl: authServerUrl,
                    clientId: "oid4vci-ios",
                    redirectUri: "oid4vc-app://callback",
                    state: state,
                    codeChallenge: codeChallenge,
                    credentialConfigurationIds: idsToRequest,
                    presentationContextProvider: self.authContextProvider
                )
                
                statusText = "Issuing token..."
                
                guard let verifier = self.pkceCodeVerifier,
                          let tokenEndpoint = self.tokenEndpointUrl,
                          let tokenUrl = URL(string: tokenEndpoint) else {
                        throw "Information required for token request (verifier or token endpoint) is missing."
                    }
                    
                let tokenResponse = try await Authorize.exchangeCodeForToken(
                    code: code,
                    pkceCodeVerifier: verifier,
                    tokenEndpointUrl: tokenUrl,
                    apiService: self.apiService
                )
                self.tokenResponse = tokenResponse
                
                guard let details = tokenResponse.authorizationDetails, !details.isEmpty else {
                    handleFailure(message: "Identifier information not found in token response.")
                    return
                }
                
                self.selectionInfo = SelectionInfo(
                    title: "Select credential",
                    details: details
                )
            } catch {
                handleFailure(message: "credentialIssuanceView authentication failed", error: error)
            }
        }
    }
    
    // --- PKCE helper functions ---

    private func generatePkceCodeVerifier() -> String {
        var buffer = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, buffer.count, &buffer)
        return Data(buffer).base64URLEncodedString()
    }

    private func generatePkceCodeChallenge(from verifier: String) -> String {
        guard let data = verifier.data(using: .utf8) else { return "" }
        let hashed = SHA256.hash(data: data)
        return Data(hashed).base64URLEncodedString()
    }
}
