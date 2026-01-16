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
import MultiFormatVC_iOS

struct AuthRequest: Codable {
    let responseUri: String
    let nonce: String
    let state: String
    let clientId: String
    
    let responseType: String
    let responseMode: String
    let dcqlQuery: String
//    let clientMetadata: [String: AnyCodable]
    
    enum CodingKeys: String, CodingKey {
        case responseUri = "response_uri"
        case nonce, state
        case clientId = "client_id"
        case responseType = "response_type"
        case responseMode = "response_mode"
        case dcqlQuery = "dcql_query"
//        case clientMetadata = "client_metadata"
    }
}

protocol Signer {
    var algorithm: String { get }
    func sign(data: Data) throws -> Data
    func getPublicKeyJwk() -> [String: Any]?
}

private class HolderSigner: Signer {
    private var holderPrivateKey = P256.Signing.PrivateKey()


    init(pkcs8PrivateKeyBase64: String) throws {
        guard let privateKeyData = Data(base64Encoded: pkcs8PrivateKeyBase64) else {
            throw "Invalid Base64 for PKCS#8 key"
        }
        self.holderPrivateKey = try P256.Signing.PrivateKey(derRepresentation: privateKeyData)
    }
    
    var algorithm: String { "ES256" }

    func sign(data: Data) throws -> Data {
        let signature = try holderPrivateKey.signature(for: data)
        return signature.rawRepresentation
    }

    func getPublicKeyJwk() -> [String : Any]? {
        let publicKey = holderPrivateKey.publicKey
        let x963Data = publicKey.x963Representation
        let x = x963Data.subdata(in: 1..<33)
        let y = x963Data.subdata(in: 33..<65)
        
        let jwk: [String: Any] = [
            "kty": "EC",
            "crv": "P-256",
            "x": x.base64URLEncodedString(),
            "y": y.base64URLEncodedString()
        ]
        return jwk
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

struct VerifyView: View {
    let verificationUri: String
    
    enum VcState {
        case loading
        case loaded(claims: [ClaimViewModel])
        case error(message: String)
    }
    
    @State private var vcState: VcState = .loading
    
    enum VerificationState {
        case idle
        case fetchingRequest
        case creatingVp
        case submittingVp
        case completed(message: String)
        case failed(error: String)
    }
    
    @State private var state: VerificationState = .idle
    
    private let apiService = APIService()
    
    var body: some View {
        VStack(spacing: 20) {
            switch state {
            case .idle:
                Text("Preparing...")
            case .fetchingRequest:
                ProgressView("Fetching verification request...")
            case .creatingVp:
                ProgressView("Creating VP...")
            case .submittingVp:
                ProgressView("Submitting VP...")
            case .completed(let message):
                Image(systemName: "checkmark.circle.fill")
                    .font(.largeTitle)
                    .foregroundColor(.green)
                Text("Submission Complete!")
                ScrollView { Text(message).font(.caption) }
            case .failed(let error):
                Image(systemName: "xmark.circle.fill")
                    .font(.largeTitle)
                    .foregroundColor(.red)
                Text("Error")
                Text(error).font(.caption)
            }
        }
        .navigationTitle("Verifying Credential")
        .onAppear(perform: startVerificationProcess)
    }
    
    private func loadVcFile() throws -> WalletData {
        let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        let fileURL = documentsDirectory.appendingPathComponent("vc.json")
        let data = try Data(contentsOf: fileURL)
        let walletDataArray = try JSONDecoder().decode([WalletData].self, from: data)
        
        guard let walletData = walletDataArray.first else {
            throw "No saved VC found."
        }
        
        return walletData
    }
    
    private func getVcFileUrl() throws -> URL {
        let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        return documentsDirectory.appendingPathComponent("vc.json")
    }
    
//    private func startVerificationProcess() {
//        Task {
//            do {
//                state = .fetchingRequest
//                guard let requestUri = getRequestUri() else { throw "Invalid verification URI." }
//                let authRequestData = try await apiService.getAuthorizationRequest(url: requestUri)
//                let authRequest = try JSONDecoder().decode(AuthRequest.self, from: authRequestData)
//                
//                let walletData = try loadVcFile()
//                
//                state = .creatingVp
//                
//                let vpToken: String
//                let format = walletData.format
//                
//                if format.contains("NationalID") || format.contains("mDL") {
//                    guard let credentials = walletData.credentialResponse.credentials.first else {
//                        vcState = .error(message: "No saved VC found.")
//                        return
//                    }
//                    let sdJwtString = credentials.credential
//                    let pkcs8PrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"
//                    let signer = try HolderSigner(pkcs8PrivateKeyBase64: pkcs8PrivateKey)
//                    
//                    vpToken = try createVpToken(
//                        from: sdJwtString,
//                        nonce: authRequest.nonce,
//                        aud: authRequest.clientId,
//                        signer: signer
//                    )
//
//                }  else if format.contains("TEC") || format.contains("UCR") {
//                    guard let credentials = walletData.credentialResponse.credentials.first else {
//                        vcState = .error(message: "No saved VC found.")
//                        return
//                    }
//                    let vcData = credentials.credential
//                    let pkcs8PrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"
//                    let signer = try HolderSigner(pkcs8PrivateKeyBase64: pkcs8PrivateKey)
//
//                    vpToken = try createUnsignedVpToken(
//                        from: vcData,
//                        nonce: authRequest.nonce,
//                        aud: authRequest.clientId,
//                        signer: signer
//                    )
//                    
//                } else {
//                    throw "Unsupported VC format: \(format)"
//                }
//
//                                      
//                state = .submittingVp
//                let finalResponse = try await apiService.postVpToken(url: authRequest.responseUri,
//                                                                     vpToken: vpToken,
//                                                                     state: authRequest.state)
//                
//                state = .completed(message: finalResponse)
//                
//                } catch {
//                    state = .failed(error: "\(error)")
//                }
//        }
//    }

    private func startVerificationProcess() {
        Task {
            do {
                state = .fetchingRequest
                guard let requestUri = getRequestUri() else { throw "Invalid verification URI." }
                let authRequestData = try await apiService.getAuthorizationRequest(url: requestUri)
                let authRequest : AuthRequest = try JSONDecoder().decode(AuthRequest.self, from: authRequestData)
                
                let walletData = try loadVcFile()
                
                state = .creatingVp
                
                let vpToken: String
                let format = walletData.format
                let dcqlId: String // dcql id as a key
                
                if format.contains("NationalID") || format.contains("mDL") {
                    guard let credentials = walletData.credentialResponse.credentials.first else {
                        vcState = .error(message: "No saved VC found.")
                        return
                    }
                    
                    vpToken = try createVpTokenSdJwt(authRequest: authRequest,
                                                     credential: credentials.credential)
                    
                    state = .submittingVp
                    let finalResponse = try await apiService.postVpToken(url: authRequest.responseUri,
                                                                         vpToken: vpToken,
                                                                         state: authRequest.state)
                    
                    state = .completed(message: finalResponse)
                    
                }  else if format.contains("TEC") || format.contains("UCR") {
                    guard let credentials = walletData.credentialResponse.credentials.first else {
                        vcState = .error(message: "No saved VC found.")
                        return
                    }
                    let vcData = credentials.credential
                    let pkcs8PrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"
                    let signer = try HolderSigner(pkcs8PrivateKeyBase64: pkcs8PrivateKey)
                    
                    vpToken = try createUnsignedVpToken(
                        from: vcData,
                        nonce: authRequest.nonce,
                        aud: authRequest.clientId,
                        signer: signer
                    )
                    
                    state = .submittingVp
                    let finalResponse = try await apiService.postVpToken(url: authRequest.responseUri,
                                                                         vpToken: vpToken,
                                                                         state: authRequest.state)
                    
                    state = .completed(message: finalResponse)
                } else {
                    throw "Unsupported VC format: \(format)"
                }
            } catch {
                state = .failed(error: "\(error)")
            }
        }
    }
    
    private func createVpTokenSdJwt(authRequest : AuthRequest, credential: String) throws -> String {
        
        let pkcs8PrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"
        let signer = try HolderSigner(pkcs8PrivateKeyBase64: pkcs8PrivateKey)
                
        
        let dcqlQuery : DCQLQuery = try .init(from: authRequest.dcqlQuery)
        let validationResult = DCQLQueryValidator.validate(dcqlQuery)
        print("DCQL Query validation result: \(validationResult.isValid())")
        
        
        let parsedVC : SDJWT = SDJWT.parse(raw: credential)
        
        let isMatching = DCQLCredentialMatcher.matchesMetadata(sdjwt: parsedVC,
                                                               metadata: dcqlQuery.credentials?.first?.meta)
        
        if !isMatching
        {
            throw "No matching VC"
        }
        
        let dcqlRequiredClaims = DCQLCredentialMatcher.extractMatchingClaimNames(dcqlQuery: dcqlQuery,
                                                                                 sdjwt: parsedVC)
        
        let vpToken = try createVpToken(
            from: credential,
            requiredClaims: dcqlRequiredClaims,
            nonce: authRequest.nonce,
            aud: authRequest.clientId,
            signer: signer
        )
        
        let vpJsonObject: [String: [String]] = ["national_id" : [vpToken]]
        let vpJsonData = try JSONSerialization.data(withJSONObject: vpJsonObject, options: [])
        guard let finalVpJsonString = String(data: vpJsonData, encoding: .utf8) else {
            throw "Failed to create final VP JSON string"
        }
        return finalVpJsonString
    }
    
    private func createUnsignedVpToken(from vcBase64String: String, nonce: String, aud: String, signer: Signer) throws -> String {

        let vcData = try decodeBase64URL(vcBase64String)
        let vc = try JSONDecoder().decode(VerifiableCredential.self, from: vcData)

        let header: [String: Any] = ["alg": signer.algorithm, "typ": "JWT"]
        let headerData = try JSONSerialization.data(withJSONObject: header, options: .sortedKeys)
        let headerBase64Url = headerData.base64URLEncodedString()
        
       let verifiableCredentialData = try JSONEncoder().encode(vc)
       guard let vcDictionary = try JSONSerialization.jsonObject(with: verifiableCredentialData) as? [String: Any] else {
           throw "Failed to convert VC model to Dictionary."
       }
       
       let vpClaim: [String: Any] = [
           "@context": ["https://www.w3.org/2018/credentials/v1"],
           "type": ["VerifiablePresentation"],
           "verifiableCredential": [vcDictionary]
       ]
        
        let payload: [String: Any] = [
            "iss": "did:example:holder",
            "jti": UUID().uuidString,
            "aud": aud,
            "nonce": nonce,
            "iat": Int(Date().timeIntervalSince1970),
            "vp": vpClaim
        ]
        let payloadData = try JSONSerialization.data(withJSONObject: payload, options: .sortedKeys)
        let payloadBase64Url = payloadData.base64URLEncodedString()
        
        let signingInput = "\(headerBase64Url).\(payloadBase64Url)"
        guard let signingInputData = signingInput.data(using: .utf8) else {
            throw "Failed to create signing input data"
        }
        let signature = try signer.sign(data: signingInputData)
        let signatureBase64Url = signature.base64URLEncodedString()
        
        return "\(signingInput).\(signatureBase64Url)"
    }

    
    // --- Function in charge of VP Token creation logic ---
    
    /// - Parameters:
    ///   - sdJwt: Original SD-JWT VC string
    ///   - nonce: nonce value received from Verifier
    ///   - aud: Verifier's client_id (audience)
    ///   - signer: Signer object to be used for signing
    /// - Returns: The final VP Token string to be submitted (VC~VP)
    private func createVpToken(from sdJwt: String,requiredClaims: Set<String>, nonce: String, aud: String, signer: Signer) throws -> String {

        let jwt = SDJWT.parse(raw: sdJwt)
        
        let selectiveDisclosure = jwt.disclosures.filter { requiredClaims.contains($0.claimName!) }
        
        let header: [String: Any] = [
            "alg": signer.algorithm,
            "typ": "kb+jwt", // Key Binding JWT type
            "jwk": signer.getPublicKeyJwk() as Any
        ]
        let headerData = try JSONSerialization.data(withJSONObject: header)
        let headerBase64Url = headerData.base64URLEncodedString()
        
        guard let sdJwtData = sdJwt.data(using: .utf8) else {
            throw "Failed to convert SD-JWT string to data"
        }
        let sdHash = SHA256.hash(data: sdJwtData)
        let sdHashBase64Url = Data(sdHash).base64URLEncodedString()

        let payload: [String: Any] = [
            "nonce": "dcql-nonce-456",
            "aud": "did:omn:issuer",
            "iat": Int(Date().timeIntervalSince1970),
            "sd_hash": sdHashBase64Url
        ]
        let payloadData = try JSONSerialization.data(withJSONObject: payload)
        let payloadBase64Url = payloadData.base64URLEncodedString()
        
        let signingInput = "\(headerBase64Url).\(payloadBase64Url)"
        guard let signingInputData = signingInput.data(using: .utf8) else {
            throw "Failed to create signing input data"
        }
        let signature = try signer.sign(data: signingInputData)
        let signatureBase64Url = signature.base64URLEncodedString()
        
        let keyBindingJwt = "\(signingInput).\(signatureBase64Url)"
        
//        return "\(sdJwt)\(keyBindingJwt)"
        let keyBindedJWT = SDJWT.init(credentialJwt: jwt.credentialJwt,
                                      disclosures: selectiveDisclosure,
                                      keyBindingJwt: keyBindingJwt)
//        jwt.keyBindingJwt = keyBindingJwt
        return keyBindedJWT.toString()
    }
    
    
    private func getRequestUri() -> String? {
        guard let url = URL(string: verificationUri),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
              let queryItems = components.queryItems,
              let uriItem = queryItems.first(where: { $0.name == "request_uri" }) else {
            return nil
        }
        return uriItem.value
    }
    
//    private func loadSdJwtFromVcJson() throws -> String {
//        let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
//        let fileURL = documentsDirectory.appendingPathComponent("vc.json")
//        let data = try Data(contentsOf: fileURL)
//        let responses = try JSONDecoder().decode([WalletData].self, from: data)
//
//        guard let response = responses.first, response.format.contains("NationalID") else {
//            throw "Could not find a valid sd-jwt format VC in vc.json."
//        }
//        return response.credentialResponse.credentials.first?.credential
//    }
}

extension String: Error {}

extension Data {
    func base64URLEncodedString() -> String {
        return self.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

