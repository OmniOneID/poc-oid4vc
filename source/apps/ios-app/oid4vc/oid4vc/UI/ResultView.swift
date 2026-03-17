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

//
//  ResultView.swift
//  oid4vc
//
//  Created by sjkim on 8/29/25.
//


import SwiftUI

/// A view that displays the results of an API operation, including request and response bodies.
struct ResultView: View {

    let result: APIResult
    
    /// The user interface body of the result view.
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading) {
                Text("Request Body:")
                    .font(.headline)
                
                TextEditor(text: .constant(result.requestBody))
                    .font(.system(.body, design: .monospaced))
                    .frame(height: 200)
                    .padding(5)
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(8)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
            }
            
            VStack(alignment: .leading) {
                Text("Response:")
                    .font(.headline)
                
                TextEditor(text: .constant(result.responseBody))
                    .font(.system(.body, design: .monospaced))
                    .frame(height: 200)
                    .padding(5)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(8)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
            }
        }
        .padding()
        .navigationTitle("API Result")
        .navigationBarTitleDisplayMode(.inline)
    }
}
