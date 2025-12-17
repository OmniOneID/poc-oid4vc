package com.example.oid4vc.issuer.util;

import java.security.PrivateKey;
import java.security.Signature;

/**
 * COSE Sign1 서명 헬퍼 (수동 구현)
 * RFC 8152 COSE 참고
 * 
 * COSE_Sign1 구조:
 * Sig_structure = [
 *   context = "Signature1",
 *   body_protected = protected_header,
 *   external_aad = b'',
 *   payload = payload
 * ]
 */
public class CoseHelper {
    
    /**
     * MSO를 COSE Sign1로 서명 (IA의 개인키 사용)
     * Section 9.1.2.4 참고
     * 
     * @param payload MSO를 CBOR 인코딩한 바이트
     * @param issuerPrivateKey IA의 개인키 (ES256)
     * @return COSE Sign1 구조의 바이트
     */
    public static byte[] coseSign1Sign(
            byte[] payload,
            PrivateKey issuerPrivateKey) throws Exception {
        
        // 1. Protected Header 생성
        // {
        //   1: -7  // alg: ES256
        // }
        byte[] protectedHeader = buildProtectedHeader();
        
        // 2. Sig_structure 생성
        // [
        //   "Signature1",
        //   protected_header,
        //   b'',
        //   payload
        // ]
        byte[] sigStructure = buildSigStructure(protectedHeader, payload);
        
        // 3. ES256으로 서명 (SHA-256 with ECDSA)
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(issuerPrivateKey);
        sig.update(sigStructure);
        byte[] signatureBytes = sig.sign();
        
        // 4. COSE_Sign1 구조 생성
        // [
        //   protected_header,
        //   unprotected_header,
        //   payload,
        //   signature
        // ]
        byte[] coseSign1 = buildCoseSign1(protectedHeader, payload, signatureBytes);
        
        return coseSign1;
    }
    
    /**
     * Protected Header 빌드
     * { 1: -7 } (alg: ES256)
     * 
     * CBOR 인코딩:
     * A1 (map with 1 item)
     * 01 (key: 1)
     * 26 (value: -7)
     */
    private static byte[] buildProtectedHeader() throws Exception {
        // CBOR map: {1: -7}
        // A1 01 26
        return new byte[] {(byte) 0xA1, 0x01, 0x26};
    }
    
    /**
     * Sig_structure 생성
     * 
     * CBOR array: [
     *   "Signature1",
     *   protected_header,
     *   b'',
     *   payload
     * ]
     */
    private static byte[] buildSigStructure(
            byte[] protectedHeader,
            byte[] payload) throws Exception {
        
        // [
        //   "Signature1",          (tstr)
        //   protected_header,      (bstr)
        //   b'',                   (bstr - external_aad)
        //   payload                (bstr)
        // ]
        
        // "Signature1" = 0x74 ("Signature1")
        byte[] sig1Context = new byte[] {0x74, 0x53, 0x69, 0x67, 0x6e, 0x61, 0x74, 0x75, 
                                         0x72, 0x65, 0x31};
        
        // "Signature1" CBOR encoding: 6b + bytes
        byte[] encodedContext = encodeTextString("Signature1");
        
        // protected_header as bstr: 58 + length + bytes
        byte[] encodedProtected = encodeByteString(protectedHeader);
        
        // external_aad as bstr: 40 (empty)
        byte[] encodedExternalAad = new byte[] {0x40};
        
        // payload as bstr
        byte[] encodedPayload = encodeByteString(payload);
        
        // Combine as array: 84 + items
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.write(0x84); // array with 4 items
        baos.write(encodedContext);
        baos.write(encodedProtected);
        baos.write(encodedExternalAad);
        baos.write(encodedPayload);
        
        return baos.toByteArray();
    }
    
    /**
     * COSE_Sign1 구조 빌드
     * 
     * [
     *   protected_header,
     *   unprotected_header,
     *   payload,
     *   signature
     * ]
     */
    private static byte[] buildCoseSign1(
            byte[] protectedHeader,
            byte[] payload,
            byte[] signature) throws Exception {
        
        // Combine as array: 84 + items
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.write(0x84); // array with 4 items
        
        // protected_header as bstr
        baos.write(encodeByteString(protectedHeader));
        
        // unprotected_header as empty map: A0
        baos.write(0xA0);
        
        // payload as bstr
        baos.write(encodeByteString(payload));
        
        // signature as bstr
        baos.write(encodeByteString(signature));
        
        return baos.toByteArray();
    }
    
    /**
     * CBOR text string 인코딩
     * 
     * String 길이에 따라:
     * - 0-23: 0x60-0x77
     * - 24-255: 0x78 + 1 byte length
     * - 256-65535: 0x79 + 2 byte length
     */
    private static byte[] encodeTextString(String text) throws Exception {
        byte[] utf8 = text.getBytes("UTF-8");
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        
        int length = utf8.length;
        
        if (length < 24) {
            baos.write(0x60 + length);
        } else if (length <= 255) {
            baos.write(0x78);
            baos.write(length);
        } else if (length <= 65535) {
            baos.write(0x79);
            baos.write((length >> 8) & 0xFF);
            baos.write(length & 0xFF);
        }
        
        baos.write(utf8);
        return baos.toByteArray();
    }
    
    /**
     * CBOR byte string 인코딩
     * 
     * Byte string 길이에 따라:
     * - 0-23: 0x40-0x57
     * - 24-255: 0x58 + 1 byte length
     * - 256-65535: 0x59 + 2 byte length
     */
    private static byte[] encodeByteString(byte[] data) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        
        int length = data.length;
        
        if (length < 24) {
            baos.write(0x40 + length);
        } else if (length <= 255) {
            baos.write(0x58);
            baos.write(length);
        } else if (length <= 65535) {
            baos.write(0x59);
            baos.write((length >> 8) & 0xFF);
            baos.write(length & 0xFF);
        } else {
            baos.write(0x5A);
            baos.write((length >> 24) & 0xFF);
            baos.write((length >> 16) & 0xFF);
            baos.write((length >> 8) & 0xFF);
            baos.write(length & 0xFF);
        }
        
        baos.write(data);
        return baos.toByteArray();
    }
}
