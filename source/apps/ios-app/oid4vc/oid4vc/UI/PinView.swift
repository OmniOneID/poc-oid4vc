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

/// A view that provides a numeric keypad for PIN entry.
struct PinView: View {
    @State private var pin: String = ""
    var onPinEntered: (String) -> Void
    
    private let maxPinLength = 4
    private let columns: [GridItem] = Array(repeating: .init(.flexible()), count: 3)
    
    /// The user interface body of the PIN entry view.
    var body: some View {
        VStack(spacing: 40) {
            Spacer()
            Text("Please Input a PIN")
                .font(.largeTitle)
            
            PinIndicator(pinLength: pin.count, maxPinLength: maxPinLength)
            
            Spacer()
            
            KeypadView(pin: $pin)
            
            Spacer()
        }
        .padding()
        .onChange(of: pin) { newValue in
            if newValue.count == maxPinLength {
                onPinEntered(newValue)
            }
        }
    }
}

/// A view that displays a visual indicator of the number of digits entered in a PIN.
private struct PinIndicator: View {
    let pinLength: Int
    let maxPinLength: Int
    
    /// The user interface body of the PIN indicator.
    var body: some View {
        HStack(spacing: 20) {
            ForEach(0..<maxPinLength, id: \.self) { index in
                Circle()
                    .fill(index < pinLength ? Color.orange : Color.gray.opacity(0.5))
                    .frame(width: 20, height: 20)
            }
        }
    }
}

/// A view that provides a grid of numeric keys for inputting a PIN.
private struct KeypadView: View {
    @Binding var pin: String
    private let maxPinLength = 4
    private let keys = [
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "X", "0", "⌫"
    ]
    private let columns: [GridItem] = Array(repeating: .init(.flexible()), count: 3)
    
    /// The user interface body of the keypad view.
    var body: some View {
        LazyVGrid(columns: columns, spacing: 20) {
            ForEach(keys, id: \.self) { key in
                Button(action: { handleKeyPress(key) }) {
                    Text(key)
                        .font(.system(size: 40, weight: .regular))
                        .foregroundColor(.primary)
                        .frame(width: 80, height: 80)
                        .background(Color.gray.opacity(0.2))
                        .clipShape(Circle())
                }
            }
        }
        .padding(.horizontal)
    }
    
    /// Handles key press events from the keypad.
    /// - Parameter key: The key that was pressed.
    private func handleKeyPress(_ key: String) {
        switch key {
        case "X":
            pin = ""
        case "⌫":
            if !pin.isEmpty {
                pin.removeLast()
            }
        default:
            if pin.count < maxPinLength {
                pin.append(key)
            }
        }
    }
}

/// A preview provider for the PinView.
struct PinView_Previews: PreviewProvider {
    /// Provides previews for the PinView.
    static var previews: some View {
        PinView(onPinEntered: { pin in
            print("Entered PIN: \(pin)")
        })
    }
}
