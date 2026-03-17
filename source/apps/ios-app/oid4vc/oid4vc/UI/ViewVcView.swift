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
import CodeScanner

/// A view that displays the details and claims of a Verifiable Credential.
struct ViewVcView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var navigationManager: NavigationManager
    
    /// Represents the different states of the VC viewing process.
    enum VcState {
        case loading
        case loaded
        case error(message: String)
    }
    
    @State private var vcState: VcState = .loading
    @State private var isShowingDeleteAlert = false
    @State private var isShowingScanner = false
    @State private var claimViewModels: [ClaimViewModel] = []
    
    @State private var manualUriInput: String = ""

    /// The user interface body of the VC view.
    var body: some View {
        VStack {
            switch vcState {
            case .loading:
                ProgressView("Loading VC...")
                Spacer()
            case .loaded:
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        ForEach(claimViewModels) { claim in
                            ClaimView(viewModel: claim)
                        }
                    }
                    .padding()
                }
                Spacer()
                
                #if targetEnvironment(simulator)
                simulatorTools
                #endif
                
                bottomButtons
            case .error(let message):
                Text(message)
                    .foregroundColor(.red)
                    .padding()
                Spacer()
            }
        }
        .navigationTitle("Verifiable Credential")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { isShowingDeleteAlert = true }) {
                    Image(systemName: "trash")
                }
            }
        }
        .onAppear(perform: loadVcFile)
        .alert("Delete VC File", isPresented: $isShowingDeleteAlert) {
            Button("Yes", role: .destructive, action: deleteVcFile)
            Button("No", role: .cancel) { }
        } message: {
            Text("Are you sure you want to delete the VC file?")
        }
        .sheet(isPresented: $isShowingScanner) {
            CodeScannerView(codeTypes: [.qr]) { response in
                if case .success(let result) = response {
                    handleSubmit(scannedUrl: result.string)
                    isShowingScanner = false
                }
            }
        }
    }

    /// A view containing tools for the simulator environment.
    private var simulatorTools: some View {
        VStack {
            Text("Enter QR Code Data")
                .font(.caption)
            TextField("QR Data", text: $manualUriInput)
                .textFieldStyle(.roundedBorder)
                .padding()
            Button("Submit") {
                handleSubmit(scannedUrl: manualUriInput)
            }
            .padding()
        }
    }
    
    /// A view containing the bottom action buttons.
    private var bottomButtons: some View {
        Button(action: { launchQrScanner() }) {
            Text("Submit with QR Code")
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.green)
                .foregroundStyle(.white)
                .cornerRadius(10)
        }
        .padding()
    }

    /// Loads the Verifiable Credential from the local storage.
    private func loadVcFile() {
        do {
            let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            let fileURL = documentsDirectory.appendingPathComponent("vc.json")
            let data = try Data(contentsOf: fileURL)
            
            if let content = String(data: data, encoding: .utf8) {
                LogUtil.logLongString("sangjun", "Wallet file content: \(content)")
            }
            
            let walletDataArray = try JSONDecoder().decode([WalletData].self, from: data)
            
            if !walletDataArray.isEmpty {
                displayVcClaims(credentialList: walletDataArray)
            } else {
                vcState = .error(message: "No saved VC found.")
            }
        } catch { 
            vcState = .error(message: "Failed to load or parse VC file: \(error.localizedDescription)")
        }
    }
    
    /// Displays the claims from a list of Verifiable Credentials.
    /// - Parameter credentialList: The list of Verifiable Credentials.
    private func displayVcClaims(credentialList: [WalletData]) {
        let walletData = credentialList[0]
        let format = walletData.format
        let credentialData = walletData.credential
        
        self.claimViewModels.removeAll()
        
        if SDJWT.isSupported(format: format) {
            let claims = SDJWT.getClaims(sdJwtVc: credentialData)
            for (key, value) in claims {
                addClaimView(caption: key, value: value, namespace: nil, indentLevel: 0, isSelectableParent: false)
            }
            vcState = .loaded
        } else if OpenDid.isSupported(format: format) {
            let claims = OpenDid.getClaims(vcCredential: credentialData)
            for claim in claims {
                renderOpenDidClaim(claim: claim)
            }
            vcState = .loaded
        } else if Mdoc.isSupported(format: format) {
            addClaimView(caption: "Format", value: "mDoc", namespace: nil, indentLevel: 0, isSelectableParent: false)
            let nsMap = Mdoc.getClaims(mDlData: credentialData)
            for (namespace, nsClaims) in nsMap {
                addClaimView(caption: "Namespace", value: namespace, namespace: nil, indentLevel: 0, isSelectableParent: false)
                if let claims = nsClaims as? [String: Any] {
                    for (key, value) in claims {
                        let isSelectableParent = (key == "driving_privileges")
                        addClaimView(caption: key, value: value, namespace: namespace, indentLevel: 1, isSelectableParent: isSelectableParent)
                    }
                }
            }
            vcState = .loaded
        } else {
            vcState = .error(message: "Unsupported VC format: \(format)")
        }
    }
    
    /// Renders a single OpenDID claim.
    /// - Parameter claim: A dictionary representing the OpenDID claim.
    private func renderOpenDidClaim(claim: [String: Any]) {
        let type = claim["type"] as? String ?? ""
        if type.lowercased() != "image" {
            let caption = claim["caption"] as? String ?? ""
            let value = claim["value"] ?? ""
            addClaimView(caption: caption, value: value, namespace: nil, indentLevel: 0, isSelectableParent: false)
        }
    }
    
    /// Adds a claim to the view models list.
    /// - Parameters:
    ///   - caption: The caption of the claim.
    ///   - value: The value of the claim.
    ///   - namespace: The namespace of the claim, if any.
    ///   - indentLevel: The indentation level for the claim.
    ///   - isSelectableParent: A boolean indicating if the claim is a selectable parent.
    private func addClaimView(caption: String, value: Any, namespace: String?, indentLevel: Int, isSelectableParent: Bool) {
        let claimValue = convertToClaimValue(caption: caption, anyValue: value, namespace: namespace, indentLevel: indentLevel, isInteractive: !isSelectableParent)
        
        let metadataCaptions = ["Format", "Namespace", "Issuer", "Subject", "vct"]
        let isMetadata = metadataCaptions.contains(caption)
        
        let viewModel = ClaimViewModel(key: caption, value: claimValue, indentLevel: indentLevel, namespace: namespace, isSelectableParent: isSelectableParent, isMetadata: isMetadata, isInteractive: true)
        self.claimViewModels.append(viewModel)
    }

    /// Handles the submission of selected claims.
    /// - Parameter scannedUrl: The URL scanned from a QR code.
    private func handleSubmit(scannedUrl: String) {
        var selectedClaimsKeys: [String] = []
        var selectedClaimsNamespaces: [String] = []
        
        func collectSelected(viewModels: [ClaimViewModel]) {
            for vm in viewModels {
                if !vm.isMetadata && vm.isSelected {
                    if !vm.isSelectableParent {
                        if vm.key != "-" {
                            selectedClaimsKeys.append(vm.key)
                            selectedClaimsNamespaces.append(vm.namespace ?? "")
                        }
                    } else {
                        selectedClaimsKeys.append(vm.key)
                        selectedClaimsNamespaces.append(vm.namespace ?? "")
                    }
                }
                
                if case .nested(let children) = vm.value {
                    collectSelected(viewModels: children)
                }
            }
        }
        
        collectSelected(viewModels: claimViewModels)
        
        navigationManager.path.append(.verification(uri: scannedUrl, selectedClaimsKeys: selectedClaimsKeys, selectedClaimsNamespaces: selectedClaimsNamespaces))
    }
    
    /// Deletes the Verifiable Credential file from local storage.
    private func deleteVcFile() {
        do {
            let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            let fileURL = documentsDirectory.appendingPathComponent("vc.json")
            try FileManager.default.removeItem(at: fileURL)
            dismiss()
        } catch {
            vcState = .error(message: "Failed to delete VC file.")
        }
    }
    
    /// Launches the QR code scanner.
    private func launchQrScanner() {
        isShowingScanner = true
    }

    /// Converts an arbitrary value to a structured ClaimValue.
    /// - Parameters:
    ///   - caption: The caption for the value.
    ///   - anyValue: The value to convert.
    ///   - namespace: The namespace of the value.
    ///   - indentLevel: The indentation level.
    ///   - isInteractive: Whether the value is interactive.
    /// - Returns: A structured ClaimValue.
    private func convertToClaimValue(caption: String, anyValue: Any, namespace: String?, indentLevel: Int, isInteractive: Bool) -> ClaimValue {
        if let dataValue = anyValue as? Data {
            return .image(dataValue)
        } else if let stringValue = anyValue as? String {
            if caption.lowercased() == "portrait" {
                if let data = Data(base64Encoded: stringValue) {
                    return .image(data)
                }
            }
            return .string(stringValue)
        } else if let intValue = anyValue as? Int {
            return .string(String(intValue))
        } else if let boolValue = anyValue as? Bool {
            return .string(String(boolValue))
        } else if let dictionaryValue = anyValue as? [String: Any] {
            var subViewModels: [ClaimViewModel] = []
            for (key, value) in dictionaryValue {
                let cv = convertToClaimValue(caption: key, anyValue: value, namespace: namespace, indentLevel: indentLevel + 1, isInteractive: false)
                subViewModels.append(ClaimViewModel(key: key, value: cv, indentLevel: indentLevel + 1, namespace: namespace, isInteractive: isInteractive))
            }
            return .nested(subViewModels)
        } else if let arrayValue = anyValue as? [Any] {
            var subViewModels: [ClaimViewModel] = []
            for value in arrayValue {
                let cv = convertToClaimValue(caption: "-", anyValue: value, namespace: namespace, indentLevel: indentLevel + 1, isInteractive: false)
                subViewModels.append(ClaimViewModel(key: "-", value: cv, indentLevel: indentLevel + 1, namespace: namespace, isInteractive: isInteractive))
            }
            return .nested(subViewModels)
        } else {
            return .string(String(describing: anyValue))
        }
    }
}

/// Represents the value of a claim, which can be a string, image, or nested claims.
enum ClaimValue {
    case string(String)
    case image(Data)
    case nested([ClaimViewModel])
}

/// A view model representing a single claim.
class ClaimViewModel: Identifiable, ObservableObject {
    let id = UUID()
    let key: String
    let value: ClaimValue
    let indentLevel: Int
    let namespace: String?
    let isSelectableParent: Bool
    let isMetadata: Bool
    let isInteractive: Bool
    
    /// A boolean indicating whether the claim is selected.
    @Published var isSelected: Bool = true {
        didSet {
            if case .nested(let children) = value {
                for child in children {
                    if child.isSelected != isSelected {
                        child.isSelected = isSelected
                    }
                }
            }
        }
    }
    
    /// Initializes a new instance of ClaimViewModel.
    init(key: String, value: ClaimValue, indentLevel: Int, namespace: String? = nil, isSelectableParent: Bool = false, isMetadata: Bool = false, isInteractive: Bool = true) {
        self.key = key
        self.value = value
        self.indentLevel = indentLevel
        self.namespace = namespace
        self.isSelectableParent = isSelectableParent
        self.isMetadata = isMetadata
        self.isInteractive = isInteractive
    }
}

/// A view that displays a claim and its children recursively.
struct ClaimView: View {
    @ObservedObject var viewModel: ClaimViewModel
    
    /// The user interface body of the claim view.
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .center) {
                if shouldShowToggle {
                    Toggle("", isOn: $viewModel.isSelected)
                        .labelsHidden()
                        .toggleStyle(CheckboxToggleStyle())
                        .disabled(!viewModel.isInteractive)
                }
                
                VStack(alignment: .leading) {
                    Text(viewModel.key.capitalized)
                        .font(.caption)
                        .foregroundStyle(.gray)
                    
                    valueView
                }
                Spacer()
            }
            .padding(.leading, CGFloat(viewModel.indentLevel * 20))
            
            if case .nested(let children) = viewModel.value {
                ForEach(children) { child in
                    ClaimView(viewModel: child)
                }
            }
        }
    }
    
    /// Whether the current claim is a leaf node.
    private var isLeaf: Bool {
        switch viewModel.value {
        case .string, .image: return true
        default: return false
        }
    }
    
    /// Whether a selection toggle should be shown for this claim.
    private var shouldShowToggle: Bool {
        guard !viewModel.isMetadata && viewModel.key != "-" else { return false }
        return viewModel.isSelectableParent || isLeaf
    }
    
    /// The view representing the value of the claim.
    @ViewBuilder
    private var valueView: some View {
        switch viewModel.value {
        case .string(let str):
            Text(str)
                .font(.headline)
                .fontWeight(.bold)
        case .image(let data):
            if let uiImage = UIImage(data: data) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 200)
            } else {
                Text("Invalid Image Data")
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        case .nested:
            EmptyView()
        }
    }
}

/// A custom toggle style that renders as a checkbox.
struct CheckboxToggleStyle: ToggleStyle {
    /// Builds the body of the checkbox toggle.
    /// - Parameter configuration: The toggle configuration.
    /// - Returns: A view representing the toggle.
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                .foregroundColor(configuration.isOn ? .blue : .gray)
                .font(.system(size: 20))
        }
    }
}
