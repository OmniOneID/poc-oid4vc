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
//  SelectionView.swift
//  oid4vc
//
//  Created by sjkim on 10/10/25.
//

import SwiftUI

/// A view that allows the user to select a credential from a list of available options.
struct SelectionView: View {
    let details: [AuthorizationDetails]
    @Binding var selection: String?
    var onConfirm: () -> Void
    
    @Environment(\.dismiss) var dismiss

    /// The user interface body of the selection view.
    var body: some View {
        VStack {
            Text("Select Credential")
                .font(.headline)
                .padding()

            Divider()
            
            ScrollView {
                VStack(alignment: .leading) {
                    ForEach(details.filter { !($0.credentialIdentifiers ?? []).isEmpty }, id: \.self) { detail in
                        Text(detail.credentialConfigurationId)
                            .font(.title3).fontWeight(.bold)
                            .padding(.top)
                        
                        ForEach(detail.credentialIdentifiers ?? [], id: \.self) { identifier in
                            Button(action: {
                                self.selection = identifier
                            }) {
                                geometryBasedButtonContent(identifier: identifier)
                            }
                        }
                        Divider()
                    }
                }
            }
            .padding(.horizontal)

            Spacer()

            Button(action: onConfirm) {
                Text("Confirm")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(selection == nil ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .disabled(selection == nil)
            .padding()
        }
    }
    
    /// Generates the view content for a credential button based on its identifier.
    /// - Parameter identifier: The unique identifier of the credential.
    /// - Returns: A view representing the button content.
    @ViewBuilder
    private func geometryBasedButtonContent(identifier: String) -> some View {
        HStack {
            Text(identifier)
                .foregroundColor(.primary)
            Spacer()
            if selection == identifier {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.accentColor)
            } else {
                Image(systemName: "circle")
                    .foregroundColor(.gray)
            }
        }
        .padding(.vertical, 8)
    }
}
