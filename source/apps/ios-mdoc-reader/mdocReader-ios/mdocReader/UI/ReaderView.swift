import SwiftUI
import CodeScanner

enum SelectionType {
    case none
    case full
    case custom
}

public struct ReaderView: View {
    @StateObject private var viewModel = ReaderViewModel()
    @State private var isScanning = false
    @State private var navigateToResult = false
    
    @State private var pidSelection: SelectionType = .none
    @State private var mdlSelection: SelectionType = .full
    
    private let primaryBrandColor = Color(red: 226/255, green: 111/255, blue: 33/255)
    
    public init() {}
    
    public var body: some View {
        VStack(spacing: 0) {
            // Custom Navigation Bar
                HStack {
                    Button(action: {}) {
                        Image(systemName: "line.horizontal.3")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }
                    
                    Spacer()
                    
                    Text("mDoc Proximity Verifier")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Color.clear.frame(width: 44, height: 44)
                }
                .padding(.horizontal, 8)
                .frame(height: 48)
                .background(primaryBrandColor)
                
                // Body Background & Content
                ZStack(alignment: .top) {
                    Color(UIColor.systemGroupedBackground)
                        .ignoresSafeArea(.all, edges: .bottom)
                    
                    VStack(spacing: 16) {
                        VStack(spacing: 16) {
                            DocumentSelectionCard(
                                title: "PID", 
                                selection: $pidSelection, 
                                brandColor: primaryBrandColor
                            )
                            
                            DocumentSelectionCard(
                                title: "mDL", 
                                selection: $mdlSelection, 
                                brandColor: primaryBrandColor
                            )
                        }
                        .padding(.top, 24)
                        .padding(.horizontal, 16)
                        
                        Spacer()
                        
                        Text(viewModel.statusText)
                            .font(.footnote)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                            .padding(.bottom, 8)
                        
                        HStack(spacing: 8) {
                            NFCIconShape()
                                .fill(Color.secondary)
                                .frame(width: 24, height: 24)
                            
                            Text("NFC not supported")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.bottom, 16)
                        
                        Button(action: {
                            isScanning = true
                        }) {
                            Text("SCAN QR CODE")
                                .font(.headline)
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(primaryBrandColor)
                                .foregroundColor(.white)
                                .cornerRadius(25)
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 30)
                        
                        NavigationLink(destination: ClaimsResultView(result: viewModel.result ?? [], status: viewModel.statusText), isActive: $navigateToResult) {
                            EmptyView()
                        }
                    }
                }
            }
            .background(primaryBrandColor.ignoresSafeArea(.all, edges: .top))
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarHidden(true)
        .sheet(isPresented: $isScanning) {
            CodeScannerView(codeTypes: [.qr], completion: handleScan)
        }
        .onChange(of: viewModel.isDataReady) { ready in
            if ready {
                navigateToResult = true
            }
        }
    }
    
    private func handleScan(result: Result<ScanResult, ScanError>) {
        isScanning = false
        switch result {
        case .success(let scan):
            viewModel.startReader(qrData: scan.string)
        case .failure(let error):
            viewModel.statusText = "Scanning failed: \(error.localizedDescription)"
        }
    }
}

private struct DocumentSelectionCard: View {
    let title: String
    @Binding var selection: SelectionType
    let brandColor: Color
    
    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.primary)
            
            Spacer()
            
            HStack(spacing: 12) {
                SelectionButton(
                    title: "Full",
                    isSelected: selection == .full,
                    brandColor: brandColor,
                    action: {
                        selection = selection == .full ? .none : .full
                    }
                )
                
                SelectionButton(
                    title: "Custom",
                    isSelected: selection == .custom,
                    brandColor: brandColor,
                    action: {
                        selection = selection == .custom ? .none : .custom
                    }
                )
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 20)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(UIColor.secondarySystemGroupedBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(UIColor.separator).opacity(0.5), lineWidth: 1)
        )
    }
}

private struct SelectionButton: View {
    let title: String
    let isSelected: Bool
    let brandColor: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .foregroundColor(isSelected ? brandColor : .primary)
                .background(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(isSelected ? brandColor : Color(UIColor.separator).opacity(0.5), lineWidth: 1)
                )
        }
    }
}

private struct NFCIconShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let scaleX = rect.width / 24.0
        let scaleY = rect.height / 24.0
        let transform = CGAffineTransform(scaleX: scaleX, y: scaleY)
        
        path.move(to: CGPoint(x: 20, y: 2))
        path.addLine(to: CGPoint(x: 4, y: 2))
        path.addCurve(to: CGPoint(x: 2, y: 4), control1: CGPoint(x: 2.9, y: 2), control2: CGPoint(x: 2, y: 2.9))
        path.addLine(to: CGPoint(x: 2, y: 20))
        path.addCurve(to: CGPoint(x: 4, y: 22), control1: CGPoint(x: 2, y: 21.1), control2: CGPoint(x: 2.9, y: 22))
        path.addLine(to: CGPoint(x: 20, y: 22))
        path.addCurve(to: CGPoint(x: 22, y: 20), control1: CGPoint(x: 21.1, y: 22), control2: CGPoint(x: 22, y: 21.1))
        path.addLine(to: CGPoint(x: 22, y: 4))
        path.addCurve(to: CGPoint(x: 20, y: 2), control1: CGPoint(x: 22, y: 2.9), control2: CGPoint(x: 21.1, y: 2))
        path.closeSubpath()
        
        path.move(to: CGPoint(x: 20, y: 20))
        path.addLine(to: CGPoint(x: 4, y: 20))
        path.addLine(to: CGPoint(x: 4, y: 4))
        path.addLine(to: CGPoint(x: 20, y: 4))
        path.addLine(to: CGPoint(x: 20, y: 20))
        path.closeSubpath()
        
        path.move(to: CGPoint(x: 18, y: 6))
        path.addLine(to: CGPoint(x: 13, y: 6))
        path.addCurve(to: CGPoint(x: 11, y: 8), control1: CGPoint(x: 11.9, y: 6), control2: CGPoint(x: 11, y: 6.9))
        path.addLine(to: CGPoint(x: 11, y: 10.28))
        path.addCurve(to: CGPoint(x: 10, y: 12), control1: CGPoint(x: 10.4, y: 10.63), control2: CGPoint(x: 10, y: 11.26))
        path.addCurve(to: CGPoint(x: 12, y: 14), control1: CGPoint(x: 10, y: 13.1), control2: CGPoint(x: 10.9, y: 14))
        path.addCurve(to: CGPoint(x: 14, y: 12), control1: CGPoint(x: 13.1, y: 14), control2: CGPoint(x: 14, y: 13.1))
        path.addCurve(to: CGPoint(x: 13, y: 10.28), control1: CGPoint(x: 14, y: 11.26), control2: CGPoint(x: 13.6, y: 10.62))
        
        path.addLine(to: CGPoint(x: 13, y: 8))
        path.addLine(to: CGPoint(x: 16, y: 8))
        path.addLine(to: CGPoint(x: 16, y: 16))
        path.addLine(to: CGPoint(x: 8, y: 16))
        path.addLine(to: CGPoint(x: 8, y: 8))
        path.addLine(to: CGPoint(x: 10, y: 8))
        path.addLine(to: CGPoint(x: 10, y: 6))
        path.addLine(to: CGPoint(x: 6, y: 6))
        path.addLine(to: CGPoint(x: 6, y: 18))
        path.addLine(to: CGPoint(x: 18, y: 18))
        path.addLine(to: CGPoint(x: 18, y: 6))
        path.closeSubpath()
        
        return path.applying(transform)
    }
}

struct ClaimsResultView: View {
    let result: [ReceivedDocument]
    let status: String
    @Environment(\.presentationMode) var presentationMode
    
    private let headerColor = Color(red: 226/255, green: 111/255, blue: 33/255)
    
    var body: some View {
        VStack(spacing: 0) {
            // Custom Navigation Bar
            HStack {
                Text("Show Documents")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                
                Spacer()
                
                Button(action: {
                    presentationMode.wrappedValue.dismiss()
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
            }
            .padding(.horizontal, 16)
            .frame(height: 56)
            .background(headerColor)
            
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("Number of documents returned: \(result.count)")
                            .font(.system(size: 16))
                            .foregroundColor(.primary)
                            .padding(.top, 16)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    
                    if result.isEmpty {
                        Text("No documents received yet.")
                            .foregroundColor(.secondary)
                            .padding()
                    } else {
                        ForEach(result, id: \.docType) { doc in
                            DocumentCardView(document: doc)
                        }
                    }
                }
                .padding(.bottom, 24)
            }
            .background(Color(UIColor.systemGroupedBackground))
        }
        .navigationTitle("")
        .navigationBarHidden(true)
    }
}

struct DocumentCardView: View {
    let document: ReceivedDocument
    
    // 문서 타이틀 간소화 (예: org.iso.18013.5.1.mDL -> mDL)
    private var docTitle: String {
        let components = document.docType.split(separator: ".")
        return components.last.map { String($0) } ?? document.docType
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack {
                Text(docTitle)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text(document.isTrusted ? "Trusted Issuer" : "Untrusted Issuer")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(document.isTrusted ? Color(red: 46/255, green: 139/255, blue: 87/255) : .red)
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 12)
            
            Divider()
                .padding(.horizontal, 16)
            
            // Claims
            VStack(alignment: .leading, spacing: 16) {
                let sortedKeys = Array(document.claims.keys).sorted()
                ForEach(sortedKeys, id: \.self) { key in
                    renderClaim(key: key, value: document.claims[key])
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            
            Divider()
                .padding(.horizontal, 16)
                .padding(.top, 24)
            
            // Validation Results
            VStack(alignment: .leading, spacing: 16) {
                renderValidationField(key: "Is device signature valid?", value: yesNo(document.validity.isDeviceSignatureValid))
                renderValidationField(key: "Is issuer signature valid?", value: yesNo(document.validity.isIssuerSignatureValid))
                renderValidationField(key: "Is data integrity intact?", value: yesNo(document.validity.isDataIntegrityIntact))
                renderValidationField(key: "Signed", value: formatDate(document.validity.signed))
                renderValidationField(key: "Valid from", value: formatDate(document.validity.validFrom))
                renderValidationField(key: "Valid until", value: formatDate(document.validity.validUntil))
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 24)
        }
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(red: 251/255, green: 246/255, blue: 247/255))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(UIColor.separator).opacity(0.3), lineWidth: 1)
        )
        .padding(.horizontal, 12)
    }
    
    @ViewBuilder
    private func renderClaim(key: String, value: Any?) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if key.lowercased() == "portrait", let data = value as? Data, let uiImage = UIImage(data: data) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 160)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.bottom, 2)
                
                Text(key)
                    .font(.system(size: 15))
                    .foregroundColor(Color(UIColor.secondaryLabel))
            } else {
                Text(key)
                    .font(.system(size: 15))
                    .foregroundColor(Color(UIColor.secondaryLabel))
                
                AnyView(renderValue(value, level: 0))
            }
        }
    }
    
    @ViewBuilder
    private func renderValue(_ value: Any?, level: Int) -> some View {
        let padding: CGFloat = CGFloat(level * 15)
        
        if let dict = value as? [String: Any] {
            VStack(alignment: .leading, spacing: 4) {
                ForEach(Array(dict.keys).sorted(), id: \.self) { subKey in
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(subKey):")
                            .font(.system(size: 16))
                            .foregroundColor(.primary)
                            .padding(.leading, padding)
                        AnyView(renderValue(dict[subKey], level: level + 1))
                    }
                }
            }
        } else if let array = value as? [Any] {
            VStack(alignment: .leading, spacing: 4) {
                ForEach(0..<array.count, id: \.self) { index in
                    AnyView(renderValue(array[index], level: level))
                }
            }
        } else if let data = value as? Data {
            Text("Binary Data (\(data.count) bytes)")
                .font(.system(size: 16))
                .italic()
                .padding(.leading, padding)
        } else {
            Text(String(describing: value ?? "N/A"))
                .font(.system(size: 16))
                .foregroundColor(.primary)
                .padding(.leading, padding)
        }
    }
    
    @ViewBuilder
    private func renderValidationField(key: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(key)
                .font(.system(size: 15))
                .foregroundColor(Color(UIColor.secondaryLabel))
            
            Text(value)
                .font(.system(size: 16))
                .foregroundColor(.primary)
        }
    }
    
    private func yesNo(_ value: Bool?) -> String {
        guard let v = value else { return "N/A" }
        return v ? "yes" : "no"
    }
    
    private func formatDate(_ date: Date?) -> String {
        guard let d = date else { return "N/A" }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter.string(from: d)
    }
}

class ReaderViewModel: ObservableObject {
    @Published var statusText = "Ready to scan"
    @Published var result: [ReceivedDocument]?
    @Published var isDataReady = false
    
    private let controller = TransferController()
    
    func startReader(qrData: String) {
        statusText = "Processing QR..."
        isDataReady = false
        
        do {
            let source = try MdocEngagement.parse(qrData)
            controller.initializeTransferManager(config: TransportConfig())
            controller.startEngagement(source: source)
            
            // ISO 18013-5 mDL standard claims
            let requestedDocs = [
                RequestedDocument(docType: "org.iso.18013.5.1.mDL", namespace: "org.iso.18013.5.1", claims: ["given_name", "family_name", "birth_date", "portrait", "issue_date", "expiry_date", "document_number", "driving_privileges"])
            ]
            
            controller.sendRequest(requestedDocs: requestedDocs, retainData: false) { [weak self] status in
                guard let self = self else { return }
                switch status {
                case .initializing: self.statusText = "Initializing..."
                case .connecting: self.statusText = "Connecting to Holder..."
                case .connected: self.statusText = "Connected! Sending SessionEstablishment..."
                case .deviceEngagementCompleted: self.statusText = "QR Parsed. Searching for Holder..."
                case .requestSent: self.statusText = "Request Sent. Waiting for response..."
                case .responseReceived(let docs):
                    self.statusText = "Response Received (\(docs.count) documents)"
                    self.result = docs
                    self.isDataReady = true
                case .disconnected: self.statusText = "Disconnected from Holder"
                case .error(let msg): self.statusText = "Error: \(msg)"
                }
            }
        } catch {
            statusText = "Invalid QR: \(error.localizedDescription)"
        }
    }
}
