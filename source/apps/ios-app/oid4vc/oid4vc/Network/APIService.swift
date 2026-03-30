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

import Foundation

class APIService {
    // todo : Needs to be modified.. process after receiving
//    private let baseURL = "http://10.48.17.124:8080/"
    private let baseURL = "http://192.168.3.130:8096/"

    func get<T: Decodable>(endpoint: String? = nil, url: URL? = nil) async throws -> T {
        let finalUrl: URL
        if let url = url {
            if let endpoint = endpoint {
                guard let newUrl = URL(string: url.absoluteString + "/" + endpoint) else {
                    throw APIError.badURL
                }
                finalUrl = newUrl
            } else {
                finalUrl = url
            }
        } else if let endpoint = endpoint, let baseUrl = URL(string: baseURL + endpoint) {
            finalUrl = baseUrl
        } else {
            throw APIError.badURL
        }
        
        var request = URLRequest(url: finalUrl)
        request.httpMethod = "GET"
        
        LogUtil.logLongString("sangjun", "HTTP GET Request: \(finalUrl.absoluteString)")
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            let responseString = String(data: data, encoding: .utf8) ?? "N/A"
            LogUtil.logLongString("sangjun", "HTTP GET Response from \(finalUrl.absoluteString): \(responseString)")
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.badServerResponse(statusCode: 500)
            }
            guard (200...299).contains(httpResponse.statusCode) else {
                let errorText = String(data: data, encoding: .utf8) ?? "Unknown server error"
                throw APIError.serverErrorString(body: errorText)
            }
            
            let decodedResponse = try JSONDecoder().decode(T.self, from: data)
            return decodedResponse
            
        } catch let error as APIError {
            throw error
        } catch is DecodingError {
            throw APIError.decodingError
        } catch {
            throw APIError.requestFailed(error)
        }
    }
    
    private func post<Body: Encodable, Response: Decodable>(endpoint: String? = nil, body: Body, authorization: String? = nil, url: URL? = nil) async throws -> Response {
        let finalUrl: URL
        if let url = url {
            if let endpoint = endpoint {
                guard let newUrl = URL(string: url.absoluteString + "/" + endpoint) else {
                    throw APIError.badURL
                }
                finalUrl = newUrl
            } else {
                finalUrl = url
            }
        } else if let endpoint = endpoint, let baseUrl = URL(string: baseURL + endpoint) {
            finalUrl = baseUrl
        } else {
            throw APIError.badURL
        }
        
        var request = URLRequest(url: finalUrl)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let authHeader = authorization {
            request.setValue(authHeader, forHTTPHeaderField: "Authorization")
        }
        
        do {
            request.httpBody = try JSONEncoder().encode(body)
        } catch {
            throw APIError.jsonEncodingFailed(error)
        }
        
        let bodyString = String(data: request.httpBody ?? Data(), encoding: .utf8) ?? "N/A"
        LogUtil.logLongString("sangjun", "HTTP POST Request: \(finalUrl.absoluteString)\nBody: \(bodyString)")
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            let responseString = String(data: data, encoding: .utf8) ?? "N/A"
            LogUtil.logLongString("sangjun", "HTTP POST Response from \(finalUrl.absoluteString): \(responseString)")
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.badServerResponse(statusCode: 500)
            }
            guard (200...299).contains(httpResponse.statusCode) else {
                let errorText = String(data: data, encoding: .utf8) ?? "Unknown server error"
                throw APIError.serverErrorString(body: errorText)
            }
            
            let decodedResponse = try JSONDecoder().decode(Response.self, from: data)
            return decodedResponse

        } catch let error as APIError {
            throw error
        } catch is DecodingError {
            throw APIError.decodingError
        } catch {
            throw APIError.requestFailed(error)
        }
    }
    
    private func postFormURLEncoded<Response: Decodable>(url: URL, params: [String: String], authorization: String? = nil) async throws -> Response {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        if let authHeader = authorization {
            request.setValue(authHeader, forHTTPHeaderField: "Authorization")
        }
        
        let bodyString = params.map { key, value in
            "\(key.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")=\(value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")"
        }.joined(separator: "&")
        
        request.httpBody = bodyString.data(using: .utf8)
        
        LogUtil.logLongString("sangjun", "HTTP POST Form Request: \(url.absoluteString)\nBody: \(bodyString)")
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            let responseString = String(data: data, encoding: .utf8) ?? "N/A"
            LogUtil.logLongString("sangjun", "HTTP POST Form Response from \(url.absoluteString): \(responseString)")
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.badServerResponse(statusCode: 500)
            }
            guard (200...299).contains(httpResponse.statusCode) else {
                let errorText = String(data: data, encoding: .utf8) ?? "Unknown server error"
                throw APIError.serverErrorString(body: errorText)
            }
            
            let decodedResponse = try JSONDecoder().decode(Response.self, from: data)
            return decodedResponse

        } catch let error as APIError {
            throw error
        } catch is DecodingError {
            throw APIError.decodingError
        } catch {
            throw APIError.requestFailed(error)
        }
    }
    
    // MARK: - OID4VCI API Calls
    
    func getCredentialOfferForTest(issuerUrl: String) async throws -> TestCredentialOfferResponse {
        return try await get(endpoint: "credential-offer/test", url: URL(string: issuerUrl))
    }
    
    func getIssuerInfo(issuerUrl: String) async throws -> IssuerMetadataResponse {
        return try await get(endpoint: ".well-known/openid-credential-issuer", url: URL(string: issuerUrl))
    }
    
    func getCredential(issuerUrl: String, authorization: String, requestBody: CredentialRequest) async throws -> CredentialResponse {
        return try await post(endpoint: "credential", body: requestBody, authorization: authorization, url: URL(string: issuerUrl))
    }
    
    func getTokenByPreAuthCode(tokenRequest: TokenRequest, url: URL?) async throws -> TokenResponse {
        let encoder = JSONEncoder()
        let detailsData = try encoder.encode(tokenRequest.authorizationDetails)
        let detailsString = String(data: detailsData, encoding: .utf8) ?? ""

        var params = [
            "grant_type": tokenRequest.grantType,
            "pre-authorized_code": tokenRequest.preAuthorizedCode,
            "authorization_details": detailsString
        ]
        
        if let txCode = tokenRequest.txCode {
            params["tx_code"] = txCode
        }
        
        guard let url = url else {
            throw APIError.badURL
        }
        
        let tokenUrl = url.appendingPathComponent("oauth2/token")
        let authHeaderValue = "Basic b2lkNHZjaS1jbGllbnQ6c2VjcmV0"
        
        return try await postFormURLEncoded(
            url: tokenUrl,
            params: params,
            authorization: authHeaderValue
        )
    }
    
    func getTokenByAuthCode(
        tokenEndpointUrl: URL,
        code: String,
        redirectUri: String,
        clientId: String,
        pkceCodeVerifier: String
    ) async throws -> TokenResponse {
        let params = [
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": redirectUri,
            "client_id": clientId,
            "code_verifier": pkceCodeVerifier
        ]
        
        let tokenUrl = tokenEndpointUrl.appendingPathComponent("oauth2/token")
        return try await postFormURLEncoded(url: tokenUrl, params: params)
    }
    
    
    // MARK: - OID4VP API Calls
    
    func getAuthorizationRequest(url: String) async throws -> Data {
        guard let fullUrl = URL(string: url) else {
            throw APIError.badURL
        }
        
        LogUtil.logLongString("sangjun", "HTTP GET Authorization Request: \(fullUrl.absoluteString)")
        
        let (data, response) = try await URLSession.shared.data(from: fullUrl)
        
        let responseString = String(data: data, encoding: .utf8) ?? "N/A"
        LogUtil.logLongString("sangjun", "HTTP GET Authorization Response from \(fullUrl.absoluteString): \(responseString)")
        
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw APIError.badServerResponse(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 500)
        }
        return data
    }
    
    func postVpToken(url: String, vpToken: String, state: String) async throws -> String {
        guard let fullUrl = URL(string: url) else {
            throw APIError.badURL
        }
        
        var request = URLRequest(url: fullUrl)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        let params = [
            "vp_token": vpToken,
            "state": state
        ]
        
        let bodyString = params.map { key, value in
            "\(key.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")=\(value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")"
        }.joined(separator: "&")
        
        request.httpBody = bodyString.data(using: .utf8)
        
        LogUtil.logLongString("sangjun", "HTTP POST VP Token Request: \(fullUrl.absoluteString)\nBody: \(bodyString)")
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            let responseString = String(data: data, encoding: .utf8) ?? "N/A"
            LogUtil.logLongString("sangjun", "HTTP POST VP Token Response from \(fullUrl.absoluteString): \(responseString)")
            
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorText = responseString
                throw APIError.serverErrorString(body: errorText)
            }
            
            return responseString

        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.requestFailed(error)
        }
    }
}
