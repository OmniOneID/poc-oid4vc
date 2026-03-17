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
import CryptoKit
import AuthenticationServices

/// A model representing the information needed to select a credential.
struct SelectionInfo: Identifiable {
    let id = UUID()
    let title: String
    let details: [AuthorizationDetails]
}

/// A provider for the authentication presentation context.
private class AuthContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    /// Provides the presentation anchor for the authentication session.
    /// - Parameter session: The authentication session.
    /// - Returns: The presentation anchor.
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        return UIApplication.shared.windows.first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}

/// A view that manages the lifecycle of credential issuance.
struct CredentialIssuanceView: View {
    let credentialOfferUri: String
    
    @State private var statusText: String = ""
    @State private var isIssuing = true
    @State private var showSuccess = false
    @State private var errorMessage: String = ""
    
    @State private var preAuthCode: String = ""
    @State private var issuerState: String?
    @State private var issuerUrl: String?
    @State private var tokenEndpointUrl: String?
    @State private var credentialConfigurationIds: [String] = []
    @State private var issuerSupportedConfigurations: [String: CredentialConfiguration]?
    
    @State private var showPinView = false
    @State private var selectionInfo: SelectionInfo?
    @State private var selectedIdentifier: String?
    @State private var accessToken: String?
    @State private var tokenResponseAuthDetails: [AuthorizationDetails]?
    
    @State private var pkceCodeVerifier: String?
    @State private var authContextProvider = AuthContextProvider()

    private let apiService = APIService()

    /// The user interface body of the credential issuance view.
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
                Text("Credential issued successfully!")
            } else {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.red)
                Text("Error: \(errorMessage)")
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
                    let selectedFinal = self.selectedIdentifier ?? ""
                    self.selectionInfo = nil
                    Task { await step3_getCredential(accessToken: self.accessToken ?? "", tokenResponseAuthDetails: self.tokenResponseAuthDetails, selectedCredentialIdentifiers: selectedFinal) }
                }
            )
        }
    }
    
    /// Starts the credential issuance flow.
    private func startIssuanceFlow() {
        Task { await step0_fetchCredentialOffer(offerUriString: credentialOfferUri) }
    }

    /// Fetches and parses the Credential Offer from the provided URI.
    /// - Parameter offerUriString: The URI string containing the credential offer.
    private func step0_fetchCredentialOffer(offerUriString: String) async {
        statusText = "Verifying Credential Offer..."
        
        guard let url = URL(string: offerUriString),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
              let queryItems = components.queryItems,
              let offerUriItem = queryItems.first(where: { $0.name == "credential_offer_uri" }),
              let offerUriValue = offerUriItem.value,
              let offerUrl = URL(string: offerUriValue) else {
            handleFailure(message: "Invalid Credential Offer URI format.")
            return
        }
        
        do {
            let offer: CredentialOfferResponse = try await apiService.get(url: offerUrl)
            self.issuerUrl = offer.credentialIssuer
            self.credentialConfigurationIds = offer.credentialConfigurationIds ?? []
            
            if self.credentialConfigurationIds.isEmpty {
                handleFailure(message: "No issuable Credential ID in Offer.")
                return
            }

            if let preAuth = offer.grants.preAuthorizedCode {
                self.preAuthCode = preAuth.preAuthorizedCode
                if self.preAuthCode.isEmpty {
                    handleFailure(message: "Pre-Authorized Code not in Offer.")
                    return
                }
            } else if let authCode = offer.grants.authorizationCode {
                self.issuerState = authCode.issuerState
            } else {
                handleFailure(message: "Unsupported Grant type or missing Grants information.")
                return
            }
            
            await step1_getIssuerInfo()
            
        } catch {
            handleFailure(message: "Failed to fetch Credential Offer", error: error)
        }
    }
    
    /// Fetches metadata from the Credential Issuer.
    private func step1_getIssuerInfo() async {
        statusText = "Fetching issuer information..."
        guard let issuerUrl = self.issuerUrl, let url = URL(string: issuerUrl) else {
            handleFailure(message: "Issuer URL not found.")
            return
        }
        
        do {
            let metadata: IssuerMetadataResponse = try await apiService.get(endpoint: ".well-known/openid-credential-issuer", url: url)
            self.issuerSupportedConfigurations = metadata.credentialConfigurationsSupported
            
            if let authServers = metadata.authorizationServer, !authServers.isEmpty {
                self.tokenEndpointUrl = authServers[0]
            } else {
                let endpoint = metadata.tokenEndpoint ?? ""
                self.tokenEndpointUrl = endpoint.isEmpty ? issuerUrl : endpoint
            }
            
            if self.preAuthCode.isEmpty {
                let targetEndpoint = self.tokenEndpointUrl ?? issuerUrl
                startAuthorizationCodeFlow(authorizationEndpoint: targetEndpoint, issuerState: self.issuerState ?? "", credentialConfigurationIds: self.credentialConfigurationIds)
            } else {
                self.showPinView = true
            }
            
        } catch {
            handleFailure(message: "Failed to fetch issuer information", error: error)
        }
    }
    
    /// Requests an access token using the pre-authorized code and provided PIN.
    /// - Parameter pinCode: The PIN code entered by the user.
    private func step2_getToken(pinCode: String) async {
        statusText = "Issuing token..."
        
        guard let endpointUrlString = tokenEndpointUrl,
              let endpointUrl = URL(string: endpointUrlString) else {
            handleFailure(message: "Token endpoint URL is invalid or missing.")
            return
        }
        
        let authDetailsArray = credentialConfigurationIds.map { id in
            return AuthorizationDetails(type: "openid_credential", credentialConfigurationId: id, credentialIdentifiers: nil)
        }
        
        let tokenRequest = TokenRequest(grantType: "urn:ietf:params:oauth:grant-type:pre-authorized_code", preAuthorizedCode: preAuthCode, txCode: pinCode, authorizationDetails: authDetailsArray)
        
        do {
            let response: TokenResponse = try await apiService.getTokenByPreAuthCode(tokenRequest: tokenRequest, url: endpointUrl)
            let token = response.accessToken
            guard !token.isEmpty else {
                handleFailure(message: "Could not find Access Token in response.")
                return
            }
            
            self.accessToken = "Bearer \(token)"
            self.tokenResponseAuthDetails = response.authorizationDetails
            
            if let responseAuthDetails = response.authorizationDetails,
               responseAuthDetails.contains(where: { !($0.credentialIdentifiers ?? []).isEmpty }) {
                showCredentialSelectionDialog(authorizationDetails: responseAuthDetails)
            } else {
                let defaultId = self.credentialConfigurationIds.first ?? ""
                Task { await step3_getCredential(accessToken: self.accessToken ?? "", tokenResponseAuthDetails: response.authorizationDetails, selectedCredentialIdentifiers: defaultId) }
            }
            
        } catch {
            handleFailure(message: "Token issuance failed", error: error)
        }
    }
    
    /// Displays a dialog for the user to select a credential from the available options.
    /// - Parameter authorizationDetails: The list of available credential authorization details.
    private func showCredentialSelectionDialog(authorizationDetails: [AuthorizationDetails]) {
        self.selectionInfo = SelectionInfo(
            title: "Select credential",
            details: authorizationDetails
        )
    }
    
    /// Requests the actual credential from the issuer using the access token.
    /// - Parameters:
    ///   - accessToken: The access token used for authentication.
    ///   - tokenResponseAuthDetails: Optional authorization details from the token response.
    ///   - selectedCredentialIdentifiers: The identifier of the selected credential.
    private func step3_getCredential(accessToken: String, tokenResponseAuthDetails: [AuthorizationDetails]?, selectedCredentialIdentifiers: String) async {
        statusText = "Requesting Credential..."
        
        guard let issuerUrl = self.issuerUrl else {
            handleFailure(message: "Issuer URL not found.")
            return
        }
        
        let credentialRequest: CredentialRequest
        if let authDetails = tokenResponseAuthDetails, !authDetails.isEmpty {
            credentialRequest = createCredentialRequestWithIdentifier(identifier: selectedCredentialIdentifiers)
        } else {
            if let ids = self.credentialConfigurationIds.first, issuerSupportedConfigurations?[ids] != nil {
                credentialRequest = createCredentialRequestWithIds(id: ids)
            } else {
                handleFailure(message: "Insufficient information for Credential request.")
                return
            }
        }
        
        do {
            let response = try await apiService.getCredential(issuerUrl: issuerUrl, authorization: accessToken, requestBody: credentialRequest)
            
            if let credential = response.credentials.first?.credential {
                let walletData = WalletData(format: selectedCredentialIdentifiers, credential: credential)
                try saveCredentialToFile(walletData: walletData)
                handleSuccess()
            } else {
                handleFailure(message: "Invalid credential response format.")
            }
        } catch {
            handleFailure(message: "Credential issuance failed", error: error)
        }
    }
    
    /// Creates a CredentialRequest object with a specific identifier and proof.
    /// - Parameter identifier: The identifier for the credential.
    /// - Returns: A CredentialRequest instance.
    private func createCredentialRequestWithIdentifier(identifier: String) -> CredentialRequest {
        var jwtProof = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        
        if identifier == "NationalID" || identifier == "mDL" || identifier == "mDoc" || identifier == "mDocPID" || identifier == "VerifiableIdSD" {
            if let jws = try? createJws() {
                jwtProof = jws
            }
        }
        
        let proofs = Proofs(diVp: nil, jwt: [jwtProof], attestation: nil)
        return CredentialRequest(credentialConfigurationId: nil, credentialIdentifier: identifier, proofs: proofs)
    }

    /// Creates a CredentialRequest object with a configuration ID and proof.
    /// - Parameter id: The configuration ID for the credential.
    /// - Returns: A CredentialRequest instance.
    private func createCredentialRequestWithIds(id: String) -> CredentialRequest {
        var jwtProof = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        
        if id == "NationalIDCert" || id == "mDL" || id == "mDoc" || id == "mDocPID" || id == "VerifiableIdSD" {
            if let jws = try? createJws() {
                jwtProof = jws
            }
        }
        
        let proofs = Proofs(diVp: nil, jwt: [jwtProof], attestation: nil)
        return CredentialRequest(credentialConfigurationId: id, credentialIdentifier: nil, proofs: proofs)
    }

    /// Generates a JSON Web Signature (JWS) for use as proof in a credential request.
    /// - Returns: A JWS string.
    private func createJws() throws -> String {
        let certString = try readCertFromFile(named: "holder.crt")
        let headerStr = "{\"alg\":\"ES256\",\"typ\":\"JWT\",\"x5c\":[\"\(certString)\"]}"
        let headerBase64 = headerStr.data(using: .utf8)!.base64URLEncodedString()
        
        let payloadStr = "{\"iss\":\"did:omn:holder\",\"sub\":\"1234567890\",\"name\":\"Raon Kim\",\"iat\":1516239022}"
        let payloadBase64 = payloadStr.data(using: .utf8)!.base64URLEncodedString()
        
        let signingInput = "\(headerBase64).\(payloadBase64)"
        guard let signingInputData = signingInput.data(using: .utf8) else {
            throw "Failed to create signing input data"
        }
        
        let pkcs8PrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"
        let signer = try HolderSigner(pkcs8PrivateKeyBase64: pkcs8PrivateKey)
        
        let signatureData = try signer.sign(data: signingInputData)
        let signatureBase64 = signatureData.base64URLEncodedString()
        
        let jws = "\(signingInput).\(signatureBase64)"
        LogUtil.logLongString("sangjun", "Generated JWS: \(jws)")
        return jws
    }
    
    /// Reads a certificate from a file and returns its cleaned string representation.
    /// - Parameter fileName: The name of the certificate file.
    /// - Returns: A cleaned certificate string.
    private func readCertFromFile(named fileName: String) throws -> String {
        if let path = Bundle.main.path(forResource: (fileName as NSString).deletingPathExtension, ofType: (fileName as NSString).pathExtension) {
            let content = try String(contentsOfFile: path, encoding: .utf8)
            return cleanCert(content)
        }
        throw "Certificate file \(fileName) not found."
    }
    
    /// Removes headers, footers, and whitespace from a certificate string.
    /// - Parameter content: The raw certificate content.
    /// - Returns: A cleaned certificate string.
    private func cleanCert(_ content: String) -> String {
        return content
            .replacingOccurrences(of: "-----BEGIN CERTIFICATE-----", with: "")
            .replacingOccurrences(of: "-----END CERTIFICATE-----", with: "")
            .replacingOccurrences(of: "\n", with: "")
            .replacingOccurrences(of: "\r", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    /// Encodes and saves a Verifiable Credential to the local file system.
    /// - Parameter walletData: The WalletData containing the VC to be saved.
    private func saveCredentialToFile(walletData: WalletData) throws {
        let dataToSave = try JSONEncoder().encode([walletData])
        
        if let jsonString = String(data: dataToSave, encoding: .utf8) {
             LogUtil.logLongString("sangjun", "Saving VC to file: \(jsonString)")
        }
        
        let documentsURL = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        let fileURL = documentsURL.appendingPathComponent("vc.json")
        try dataToSave.write(to: fileURL)
    }
    
    /// Updates the view state to reflect successful credential issuance.
    private func handleSuccess() {
        isIssuing = false
        showSuccess = true
    }
    
    /// Handles failure during the credential issuance process and updates the view state.
    /// - Parameters:
    ///   - message: The error message.
    ///   - error: Optional error object for detailed information.
    private func handleFailure(message: String, error: Error? = nil) {
        var fullMessage = message
        if let error = error {
            fullMessage += ": \(error.localizedDescription)"
        }
        LogUtil.logLongString("sangjun", "Error: \(fullMessage)")
        self.errorMessage = fullMessage
        self.isIssuing = false
        self.showSuccess = false
    }
    
    /// Starts the Authorization Code Flow for credential issuance.
    /// - Parameters:
    ///   - authorizationEndpoint: The endpoint for authorization.
    ///   - issuerState: The state parameter from the issuer.
    ///   - credentialConfigurationIds: The list of credential configuration IDs.
    private func startAuthorizationCodeFlow(authorizationEndpoint: String, issuerState: String, credentialConfigurationIds: [String]) {
        statusText = "Opening browser for user authentication..."

        let authServerUrl = URL(string: authorizationEndpoint)
        if authServerUrl == nil {
            handleFailure(message: "Invalid authorization server URL.")
            return
        }
        
        let codeVerifier = generatePkceCodeVerifier()
        let codeChallenge = generatePkceCodeChallenge(from: codeVerifier)

        Task {
            do {
                let code = try await Authorize.start(
                    authorizationServerUrl: authServerUrl!,
                    clientId: "oid4vci-ios",
                    redirectUri: "oid4vc-app://callback",
                    state: issuerState,
                    codeChallenge: codeChallenge,
                    credentialConfigurationIds: credentialConfigurationIds,
                    presentationContextProvider: self.authContextProvider
                )
                
                statusText = "Issuing token..."
                
                let tokenUrl = URL(string: authorizationEndpoint)
                if tokenUrl == nil {
                    handleFailure(message: "Invalid token endpoint URL.")
                    return
                }
                    
                let response = try await Authorize.exchangeCodeForToken(
                    code: code,
                    pkceCodeVerifier: codeVerifier,
                    tokenEndpointUrl: tokenUrl!,
                    apiService: self.apiService
                )
                
                let token = response.accessToken
                guard !token.isEmpty else {
                    handleFailure(message: "Could not find Access Token in response.")
                    return
                }
                
                self.accessToken = "Bearer \(token)"
                self.tokenResponseAuthDetails = response.authorizationDetails
                
                if let responseAuthDetails = response.authorizationDetails, !responseAuthDetails.isEmpty {
                    showCredentialSelectionDialog(authorizationDetails: responseAuthDetails)
                }
            } catch {
                handleFailure(message: "Authorization flow failed", error: error)
            }
        }
    }
    
    /// Generates a PKCE code verifier.
    /// - Returns: A base64URL-encoded code verifier string.
    private func generatePkceCodeVerifier() -> String {
        var buffer = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, buffer.count, &buffer)
        return Data(buffer).base64URLEncodedString()
    }

    /// Generates a PKCE code challenge from a given verifier.
    /// - Parameter verifier: The code verifier string.
    /// - Returns: A base64URL-encoded code challenge string.
    private func generatePkceCodeChallenge(from verifier: String) -> String {
        guard let data = verifier.data(using: .utf8) else { return "" }
        let hashed = SHA256.hash(data: data)
        return Data(hashed).base64URLEncodedString()
    }
}

extension String: Error {}
