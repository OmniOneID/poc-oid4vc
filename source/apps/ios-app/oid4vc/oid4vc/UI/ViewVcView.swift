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

    @State private var vcFormat: String = ""
    @State private var rawCredential: String = ""
    
    // iOS 15용 내부 내비게이션 상태
    @State private var isOfflineActiveInternal = false
    @State private var offlineDataInternal: (mDoc: String, keys: [String], namespaces: [String])?

    /// The user interface body of the VC view.
    var body: some View {
        VStack {
            switch vcState {
            case .loading:
                ProgressView("Loading VC...")
                Spacer()
            case .loaded:
                ZStack {
                    // iOS 15 전용 숨겨진 내비게이션 링크
                    if #unavailable(iOS 16.0) {
                        if let data = offlineDataInternal {
                            NavigationLink(
                                destination: QRGeneratorView(mDoc: data.mDoc, selectedKeys: data.keys, namespaces: data.namespaces),
                                isActive: $isOfflineActiveInternal
                            ) { EmptyView() }
                        }
                    }
                    
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            ForEach(claimViewModels) { claim in
                                ClaimView(viewModel: claim)
                            }
                        }
                        .padding()
                    }
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
        VStack(spacing: 12) {
            Button(action: { 
                print("DEBUG: [ViewVcView] Submit with QR Code tapped.")
                launchQrScanner() 
            }) {
                Text("Submit with QR Code")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            
            if Mdoc.isSupported(format: vcFormat) {
                Button(action: { 
                    print("DEBUG: [ViewVcView] Submit Offline (ISO 18013-5) tapped. Format: \(vcFormat)")
                    handleOfflineSubmit() 
                }) {
                    Text("Submit Offline (ISO 18013-5)")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
            }
        }
        .padding()
    }

    /// Loads the Verifiable Credential from the local storage.
    private func loadVcFile() {
        print("DEBUG: [ViewVcView] Loading VC file...")
        do {
            let documentsDirectory = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            let fileURL = documentsDirectory.appendingPathComponent("vc.json")
            let data = try Data(contentsOf: fileURL)
            
            let walletDataArray = try JSONDecoder().decode([WalletData].self, from: data)
            
            if !walletDataArray.isEmpty {
                print("DEBUG: [ViewVcView] VC file loaded. Format: \(walletDataArray[0].format)")
                displayVcClaims(credentialList: walletDataArray)
            } else {
                vcState = .error(message: "No saved VC found.")
            }
        } catch { 
            print("ERROR: [ViewVcView] Failed to load VC: \(error.localizedDescription)")
            vcState = .error(message: "Failed to load or parse VC file: \(error.localizedDescription)")
        }
    }
    
    /// Displays the claims from a list of Verifiable Credentials.
    private func displayVcClaims(credentialList: [WalletData]) {
        let walletData = credentialList[0]
        let format = walletData.format
        let credentialData = walletData.credential
        
        self.vcFormat = format
        self.rawCredential = credentialData
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
    
    private func renderOpenDidClaim(claim: [String: Any]) {
        let type = claim["type"] as? String ?? ""
        if type.lowercased() != "image" {
            let caption = claim["caption"] as? String ?? ""
            let value = claim["value"] ?? ""
            addClaimView(caption: caption, value: value, namespace: nil, indentLevel: 0, isSelectableParent: false)
        }
    }
    
    private func addClaimView(caption: String, value: Any, namespace: String?, indentLevel: Int, isSelectableParent: Bool) {
        let claimValue = convertToClaimValue(caption: caption, anyValue: value, namespace: namespace, indentLevel: indentLevel, isInteractive: !isSelectableParent)
        let metadataCaptions = ["Format", "Namespace", "Issuer", "Subject", "vct"]
        let isMetadata = metadataCaptions.contains(caption)
        let viewModel = ClaimViewModel(key: caption, value: claimValue, indentLevel: indentLevel, namespace: namespace, isSelectableParent: isSelectableParent, isMetadata: isMetadata, isInteractive: true)
        self.claimViewModels.append(viewModel)
    }

    private func handleSubmit(scannedUrl: String) {
        var selectedClaimsKeys: [String] = []
        var selectedClaimsNamespaces: [String] = []
        collectAllSelected(keys: &selectedClaimsKeys, namespaces: &selectedClaimsNamespaces)
        
        print("DEBUG: [ViewVcView] Submitting with QR Code. Selected keys: \(selectedClaimsKeys.count)")
        navigationManager.path.append(.verification(uri: scannedUrl, selectedClaimsKeys: selectedClaimsKeys, selectedClaimsNamespaces: selectedClaimsNamespaces))
    }
    
    private func handleOfflineSubmit() {
        var selectedClaimsKeys: [String] = []
        var selectedClaimsNamespaces: [String] = []
        collectAllSelected(keys: &selectedClaimsKeys, namespaces: &selectedClaimsNamespaces)
        
        print("DEBUG: [ViewVcView] Handling Offline Submit. Selected keys: \(selectedClaimsKeys.count)")
        
        if #available(iOS 16.0, *) {
            navigationManager.path.append(.mdocOffline(mDoc: rawCredential, selectedKeys: selectedClaimsKeys, namespaces: selectedClaimsNamespaces))
        } else {
            self.offlineDataInternal = (rawCredential, selectedClaimsKeys, selectedClaimsNamespaces)
            self.isOfflineActiveInternal = true
        }
    }
    
    private func collectAllSelected(keys: inout [String], namespaces: inout [String]) {
        func collect(viewModels: [ClaimViewModel]) {
            for vm in viewModels {
                if !vm.isMetadata && vm.isSelected {
                    if !vm.isSelectableParent {
                        if vm.key != "-" {
                            keys.append(vm.key)
                            namespaces.append(vm.namespace ?? "")
                        }
                    } else {
                        keys.append(vm.key)
                        namespaces.append(vm.namespace ?? "")
                    }
                }
                if case .nested(let children) = vm.value {
                    collect(viewModels: children)
                }
            }
        }
        collect(viewModels: claimViewModels)
    }
    
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
    
    private func launchQrScanner() {
        isShowingScanner = true
    }

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
                        .foregroundColor(.gray)
                    
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
    
    private var isLeaf: Bool {
        switch viewModel.value {
        case .string, .image: return true
        default: return false
        }
    }
    
    private var shouldShowToggle: Bool {
        guard !viewModel.isMetadata && viewModel.key != "-" else { return false }
        return viewModel.isSelectableParent || isLeaf
    }
    
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
                    .foregroundColor(.red)
            }
        case .nested:
            EmptyView()
        }
    }
}

/// A custom toggle style that renders as a checkbox.
struct CheckboxToggleStyle: ToggleStyle {
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
