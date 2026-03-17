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

/// A view that provides an interface for testing OID4VC API endpoints.
struct APITestView: View {
    private let apiService = APIService()
    
    @State private var issuerUrl: String = "http://192.168.3.130:8096/"
    @State private var tokenUrl: String = "http://192.168.3.130:18096/"
    
    @State private var preAuthCodeForTest: String = ""
    @State private var txCodeForTest: String = ""
    @State private var accessTokenForTest: String = ""
    
    @State private var resultToShow: APIResult?
    @State private var isResultViewActive = false

    /// The user interface body of the API test view.
    var body: some View {
        Form {
                   Section(header: Text("Endpoints")) {
                       HStack {
                           Text("Issuer")
                           TextField("Issuer URL", text: $issuerUrl)
                               .multilineTextAlignment(.trailing)
                       }
                       HStack {
                           Text("Token")
                           TextField("Token URL", text: $tokenUrl)
                               .multilineTextAlignment(.trailing)
                       }
                   }
                   
                   Section(header: Text("API Calls")) {
                       Button("Credential Offer (Test GET)", action: { Task { await testCredentialOffer() } })
                       Button("Issuer Metadata (.well-known)", action: { Task { await testIssuerInfo() } })
                       Button("Token (pre-authorized)", action: { Task { await testToken() } })
                       VStack(alignment: .leading) {
                           Text("Token Parameters")
                               .font(.caption)
                               .padding(.top)
                           
                           HStack {
                               Text("pre-authorized_code")
                                   .font(.subheadline)
                               TextField("Pre-authorized Code", text: $preAuthCodeForTest)
                                   .multilineTextAlignment(.trailing)
                                   .font(.subheadline)
                           }
                           HStack {
                               Text("tx_code")
                                   .font(.subheadline)
                               TextField("Transaction Code", text: $txCodeForTest)
                                   .multilineTextAlignment(.trailing)
                                   .font(.subheadline)
                           }
                       }
                       
                       Button("Credential Request", action: { Task { await testCredentialRequest() } })
                       VStack(alignment: .leading) {
                           Text("Access Token")
                               .font(.caption)
                               .padding(.top)

                           HStack {
                               Text("access_token")
                                   .font(.subheadline)
                               TextField("Access Token", text: $accessTokenForTest)
                                   .multilineTextAlignment(.trailing)
                                   .font(.subheadline)
                           }
                       }
                   }
               }
        .navigationTitle("API Test")
        .background(
            NavigationLink(
                destination: ResultView(result: resultToShow ?? APIResult(requestBody: "", responseBody: "")),
                isActive: $isResultViewActive
            ) {
                EmptyView()
            }
        )
    }
    
    /// Tests the credential offer API endpoint.
    private func testCredentialOffer() async {
        let requestDesc = "Method: GET\nURL: \(issuerUrl)/credential-offer/test"
        do {
            let response: TestCredentialOfferResponse = try await apiService.getCredentialOfferForTest(issuerUrl: issuerUrl)
            let responseStr = prettyJson(from: response)
            showResult(request: requestDesc, response: responseStr)
        } catch {
            showResult(request: requestDesc, response: "Error: \(error.localizedDescription)")
        }
    }
    
    /// Tests the issuer metadata API endpoint.
    private func testIssuerInfo() async {
        let requestDesc = "Method: GET\nURL: \(issuerUrl)/.well-known/openid-credential-issuer"
        do {
            let response: IssuerMetadataResponse = try await apiService.getIssuerInfo(issuerUrl: issuerUrl)
            let responseStr = prettyJson(from: response)
            showResult(request: requestDesc, response: responseStr)
        } catch {
            showResult(request: requestDesc, response: "Error: \(error.localizedDescription)")
        }
    }
    
    /// Tests the token API endpoint using a pre-authorized code.
    private func testToken() async {
        let grantType = "urn:ietf:params:oauth:grant-type:pre-authorized_code"
        let authDetails = AuthorizationDetails(type: "openid_credential", credentialConfigurationId: "TEC", credentialIdentifiers: nil)
        let tokenRequest = TokenRequest(
            grantType: grantType,
            preAuthorizedCode: preAuthCodeForTest,
            txCode: txCodeForTest,
            authorizationDetails: [authDetails]
        )
        let authDetailsJson = prettyJson(from: [authDetails])
        let requestBodyString = "grant_type=\(grantType)&pre-authorized_code=\(preAuthCodeForTest)&tx_code=\(txCodeForTest)&authorization_details=\(authDetailsJson)"
        let requestDesc = "Method: POST\nURL: \(tokenUrl)oauth2/token\n\n--- Headers ---\nAuthorization: Basic b2lkNHZjaS1jbGllbnQ6c2VjcmV0\n\n--- Request Body ---\n\(requestBodyString)"
        print("--- tokenUrl ---")
        print(tokenUrl)
        print("------------------------------------")
        do {
            let response: TokenResponse = try await apiService.getTokenByPreAuthCode(tokenRequest: tokenRequest, url: URL(string: tokenUrl + "/oauth2/token"))
            let responseStr = prettyJson(from: response)
            showResult(request: requestDesc, response: responseStr)
        } catch {
            showResult(request: requestDesc, response: "Error: \(error.localizedDescription)")
        }
    }
    
    /// Tests the credential request API endpoint.
    private func testCredentialRequest() async {
        let exampleJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM0MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        let proofs = Proofs(diVp: nil, jwt: [exampleJwt], attestation: nil)
        let credentialRequest = CredentialRequest(credentialConfigurationId: nil, credentialIdentifier: "NationalID", proofs: proofs)
        
        let accessToken = "Bearer " + accessTokenForTest
        let requestBodyStr = prettyJson(from: credentialRequest)
        let requestDesc = "Method: POST\nURL: \(issuerUrl)credential\n\n--- Headers ---\nAuthorization: \(accessToken)\n\n--- Request Body ---\n\(requestBodyStr)"

        do {
            let response: CredentialResponse = try await apiService.getCredential(issuerUrl: issuerUrl, authorization: accessToken, requestBody: credentialRequest)
            let responseStr = prettyJson(from: response)
            showResult(request: requestDesc, response: responseStr)
        } catch {
            showResult(request: requestDesc, response: "Error: \(error.localizedDescription)")
        }
    }
    
    /// Displays the API request and response results.
    /// - Parameters:
    ///   - request: The string representation of the request.
    ///   - response: The string representation of the response.
    private func showResult(request: String, response: String) {
        self.resultToShow = APIResult(requestBody: request, responseBody: response)
        self.isResultViewActive = true
    }
    
    /// Converts an Encodable value into a pretty-printed JSON string.
    /// - Parameter value: The value to be encoded.
    /// - Returns: A pretty-printed JSON string.
    private func prettyJson<T: Encodable>(from value: T) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        guard let data = try? encoder.encode(value), let string = String(data: data, encoding: .utf8) else { 
            return "Failed to encode to JSON."
        }
        return string
    }
}
