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

struct SelectionView: View {
    let details: [AuthorizationDetails]
    @Binding var selection: String?
    var onConfirm: () -> Void
    
    @Environment(\.dismiss) var dismiss

    var body: some View {
        VStack {
            Text("Select Identifier")
                .font(.headline)
                .padding()

            Divider()
            
            ScrollView {
                VStack(alignment: .leading) {
                    ForEach(details, id: \.self) { detail in
                        Text(detail.credentialConfigurationId)
                            .font(.title3).fontWeight(.bold)
                            .padding(.top)
                        
                        ForEach(detail.credentialIdentifiers ?? [], id: \.self) { identifier in
                            Button(action: {
                                self.selection = identifier
                            }) {
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
}
