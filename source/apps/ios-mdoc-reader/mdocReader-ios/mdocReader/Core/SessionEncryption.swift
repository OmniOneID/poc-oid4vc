import Foundation
import CryptoKit
import SwiftCBOR

public class SessionEncryption {
    private let readerPrivateKey: P256.KeyAgreement.PrivateKey
    private let devicePublicKey: P256.KeyAgreement.PublicKey
    private let encodedEngagement: Data
    
    private var skReader: SymmetricKey?
    private var skDevice: SymmetricKey?
    private var readerCounter: UInt32 = 1
    private var deviceCounter: UInt32 = 1
    
    private let labelSkReader = "SKReader".data(using: .utf8)!
    private let labelSkDevice = "SKDevice".data(using: .utf8)!
    
    public init(devicePublicKeyRaw: Data, encodedEngagement: Data) throws {
        print("MDR/Crypto: Initializing SessionEncryption with \(devicePublicKeyRaw.count) bytes pubkey")
        self.readerPrivateKey = P256.KeyAgreement.PrivateKey()
        
        // 65 bytes (0x04 prefix) must use x963Representation in CryptoKit
        self.devicePublicKey = try P256.KeyAgreement.PublicKey(x963Representation: devicePublicKeyRaw)
        self.encodedEngagement = encodedEngagement
        
        try deriveSessionKeys()
    }
    
    private func deriveSessionKeys() throws {
        let sharedSecret = try readerPrivateKey.sharedSecretFromKeyAgreement(with: devicePublicKey)
        
        // SessionTranscript = [DeviceEngagementBytes, EReaderKeyBytes, Handover]
        let readerKeyCBOR = try getEReaderKeyCBOR()
        let transcript: [CBOR] = [
            .tagged(CBOR.Tag(rawValue: 24), .byteString(encodedEngagement.map { $0 })),
            .tagged(CBOR.Tag(rawValue: 24), .byteString(readerKeyCBOR.encode())),
            .null
        ]
        let encodedTranscript = CBOR.array(transcript).encode()
        
        // Salt = SHA-256(CBOR(Tag(24, bstr(encodedSessionTranscript))))
        let taggedTranscript = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(encodedTranscript)).encode()
        let salt = Data(SHA256.hash(data: Data(taggedTranscript)))
        
        // Use CryptoKit's built-in derivation from SharedSecret (HKDF-Extract & Expand)
        skReader = sharedSecret.hkdfDerivedSymmetricKey(using: SHA256.self, salt: salt, sharedInfo: labelSkReader, outputByteCount: 32)
        skDevice = sharedSecret.hkdfDerivedSymmetricKey(using: SHA256.self, salt: salt, sharedInfo: labelSkDevice, outputByteCount: 32)
        
        print("Reader: Derived session keys successfully")
    }
    
    public func getEReaderKeyCBOR() throws -> CBOR {
        let pub = readerPrivateKey.publicKey.rawRepresentation // 65 bytes: 0x04 + X + Y
        let x = pub.subdata(in: 1..<33)
        let y = pub.subdata(in: 33..<65)
        
        var coseKey: [CBOR: CBOR] = [:]
        coseKey[1] = 2 // kty: EC2
        coseKey[-1] = 1 // crv: P-256
        coseKey[-2] = .byteString(x.map { $0 })
        coseKey[-3] = .byteString(y.map { $0 })
        
        return .map(coseKey)
    }
    
    public func encryptRequest(_ message: Data) throws -> Data {
        guard let key = skReader else { throw NSError(domain: "SessionEncryption", code: 1, userInfo: [NSLocalizedDescriptionKey: "Keys not derived"]) }
        let nonce = buildIv(counter: readerCounter, isDevice: false)
        let sealedBox = try AES.GCM.seal(message, using: key, nonce: nonce)
        readerCounter += 1
        
        // ISO 18013-5: Send only ciphertext + tag (do not include nonce)
        let result = sealedBox.ciphertext + sealedBox.tag
        print("Reader: Encrypted request (counter: \(readerCounter-1), size: \(result.count))")
        return result
    }
    
    public func decryptResponse(_ message: Data) throws -> Data {
        guard let key = skDevice else { throw NSError(domain: "SessionEncryption", code: 2, userInfo: [NSLocalizedDescriptionKey: "Keys not derived"]) }
        let nonce = buildIv(counter: deviceCounter, isDevice: true)
        
        // message is ciphertext + tag (16 bytes). Reconstruct SealedBox.
        let tagSize = 16
        guard message.count > tagSize else { throw NSError(domain: "SessionEncryption", code: 3, userInfo: [NSLocalizedDescriptionKey: "Message too short"]) }
        
        let ciphertext = message.subdata(in: 0..<(message.count - tagSize))
        let tag = message.suffix(tagSize)
        
        let sealedBox = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
        let decrypted = try AES.GCM.open(sealedBox, using: key)
        deviceCounter += 1
        print("Reader: Decrypted response (counter: \(deviceCounter-1), size: \(decrypted.count))")
        return decrypted
    }
    
    private func buildIv(counter: UInt32, isDevice: Bool) -> AES.GCM.Nonce {
        var ivData = Data(count: 12)
        // ISO 18013-5: [0x00000000] [identifier:4bytes] [counter:4bytes]
        // counter is big-endian
        let identifier: UInt32 = isDevice ? 1 : 0
        
        var counterBE = counter.bigEndian
        var identifierBE = identifier.bigEndian
        
        withUnsafeBytes(of: &identifierBE) { ivData.replaceSubrange(4..<8, with: $0) }
        withUnsafeBytes(of: &counterBE) { ivData.replaceSubrange(8..<12, with: $0) }
        
        return try! AES.GCM.Nonce(data: ivData)
    }
}
