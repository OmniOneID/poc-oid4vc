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

//
//  Authorize.swift
//  oid4vc
//
//  Created by sjkim on 10/20/25.
//

import Foundation
import AuthenticationServices
import CryptoKit

/// An enumeration representing errors that can occur during the authorization process.
enum AuthorizationError: Error, LocalizedError {
    case missingCallbackURL
    case missingAuthorizationCode
    
    /// A localized description of the error.
    var errorDescription: String? {
        switch self {
        case .missingCallbackURL:
            return "Did not receive authentication callback URL."
        case .missingAuthorizationCode:
            return "Could not find code in authentication response."
        }
    }
}

/// A utility structure for handling OID4VC authorization.
struct Authorize {
    
    /// Starts an ASWebAuthenticationSession to request an authorization code.
    /// - Parameters:
    ///   - authorizationServerUrl: The URL of the authorization server.
    ///   - clientId: The client identifier.
    ///   - redirectUri: The redirect URI for the authorization callback.
    ///   - state: The state parameter for the authorization request.
    ///   - codeChallenge: The PKCE code challenge.
    ///   - credentialConfigurationIds: The list of credential configuration IDs.
    ///   - presentationContextProvider: The provider for the authentication session's presentation context.
    /// - Returns: The authorization code received from the server.
    static func start(
        authorizationServerUrl: URL,
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String,
        credentialConfigurationIds: [String],
        presentationContextProvider: ASWebAuthenticationPresentationContextProviding
    ) async throws -> String {
        
        let authDetailsPayload = credentialConfigurationIds.map { id in
            return [
                "type": "openid_credential",
                "credential_configuration_id": id
            ]
        }
        
        let authDetailsData = try JSONSerialization.data(withJSONObject: authDetailsPayload)
        let authDetailsString = String(data: authDetailsData, encoding: .utf8)!
        let doubleEncodedAuthDetails = authDetailsString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        
        var components = URLComponents(url: authorizationServerUrl, resolvingAgainstBaseURL: false)!
        components.path += "/oauth2/authorize"

        let queryParams = [
            "response_type=code",
            "client_id=\(clientId)",
            "redirect_uri=\(redirectUri.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!)",
            "authorization_details=\(doubleEncodedAuthDetails)",
            "state=\(state)",
            "code_challenge=\(codeChallenge)",
            "code_challenge_method=S256"
        ]
        components.percentEncodedQuery = queryParams.joined(separator: "&")
        
        guard let authURL = components.url else {
            throw APIError.badURL
        }
        
        print("--- Opening Authorization URL ---")
        print(authURL.absoluteString)
        print("---------------------------------")
        
        return try await withCheckedThrowingContinuation { continuation in
            let session = ASWebAuthenticationSession(
                url: authURL,
                callbackURLScheme: URL(string: redirectUri)?.scheme
            ) { callbackURL, error in
                
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }
                
                guard let callbackURL = callbackURL else {
                    continuation.resume(throwing: AuthorizationError.missingCallbackURL)
                    return
                }
                
                if let components = URLComponents(url: callbackURL, resolvingAgainstBaseURL: true),
                   let codeItem = components.queryItems?.first(where: { $0.name == "code" }),
                   let code = codeItem.value {
                    continuation.resume(returning: code)
                } else {
                    continuation.resume(throwing: AuthorizationError.missingAuthorizationCode)
                }
            }
            
            session.presentationContextProvider = presentationContextProvider
            session.start()
        }
    }
    
    /// Exchanges an authorization code for an access token.
    /// - Parameters:
    ///   - code: The authorization code.
    ///   - pkceCodeVerifier: The PKCE code verifier.
    ///   - tokenEndpointUrl: The URL of the token endpoint.
    ///   - apiService: The API service instance.
    /// - Returns: A TokenResponse containing the access token.
    static func exchangeCodeForToken(
            code: String,
            pkceCodeVerifier: String,
            tokenEndpointUrl: URL,
            apiService: APIService
        ) async throws -> TokenResponse {
            
            return try await apiService.getTokenByAuthCode(
                tokenEndpointUrl: tokenEndpointUrl,
                code: code,
                redirectUri: "oid4vc-app://callback",
                clientId: "oid4vci-ios",
                pkceCodeVerifier: pkceCodeVerifier
            )
        }
}
