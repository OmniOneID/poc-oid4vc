package com.example.oid4vc.issuer.issuer;

import com.example.oid4vc.issuer.util.CborHelper;
import com.example.oid4vc.issuer.util.InMemoryCertificateChainLoader;
import com.example.oid4vc.issuer.util.TestCertificateGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * mDL Issuer 테스트
 * Section 9.1.2 (Issuer 책임) 검증
 */
@DisplayName("MdocIssuer 테스트")
public class MdocIssuerTest {
    
    private MdocIssuer mdocIssuer;
    
    @BeforeEach
    void setUp() throws Exception {
        // 1. 테스트용 KeyPair 생성
        KeyPair testKeyPair = TestCertificateGenerator.generateTestDSKeyPair();
        
        // 2. CertificateChainLoader 생성
        InMemoryCertificateChainLoader certChainLoader = 
            new InMemoryCertificateChainLoader(testKeyPair);
        
        // 3. MdocIssuer 생성
        mdocIssuer = new MdocIssuer(certChainLoader);
    }
    
    @Test
    @DisplayName("mDL 발급 성공")
    void testIssueMdl() throws Exception {
        // Given
        String userId = "test-user-123";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "test-nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        
        // Then
        assertNotNull(result, "mDL은 null이 아니어야 함");
        assertInstanceOf(String.class, result, "mDL은 String(Base64)이어야 함");
        
        // 1. Base64 디코딩 가능성 확인
        String mdlBase64 = (String) result;
        byte[] mdlBytes = null;
        try {
            mdlBytes = Base64.getDecoder().decode(mdlBase64);
        } catch (Exception e) {
            fail("Base64 디코딩 실패: " + e.getMessage());
        }
        
        assertNotNull(mdlBytes, "CBOR 바이트는 null이 아니어야 함");
        assertTrue(mdlBytes.length > 0, "mDL 크기는 0보다 커야 함");
        
        // 📋 mDL을 JSON으로 출력
        String jsonPretty = CborHelper.toJsonString(mdlBytes);
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 발급된 mDL (JSON 형식)");
        System.out.println("=".repeat(80));
        System.out.println(jsonPretty);
        System.out.println("=".repeat(80));
        
        System.out.println("✅ mDL 발급 성공");
        System.out.println("   mDL 크기: " + mdlBytes.length + " bytes");
        System.out.println("   Base64 길이: " + mdlBase64.length() + " chars");
        System.out.println("   Base64 값:");
        System.out.println("   " + mdlBase64);
    }
    
    @Test
    @DisplayName("Document 구조 검증")
    void testIssueMdlStructure() throws Exception {
        // Given
        String userId = "test-user";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        
        // Then
        String mdlBase64 = (String) result;
        byte[] mdlBytes = Base64.getDecoder().decode(mdlBase64);
        
        // 2. CBOR 디코딩
        @SuppressWarnings("unchecked")
        Map<String, Object> document = CborHelper.decode(mdlBytes, Map.class);
        
        assertNotNull(document, "Document은 null이 아니어야 함");
        
        // 3. 필수 필드 검증
        assertTrue(document.containsKey("docType"), "docType 필드 필요");
        assertTrue(document.containsKey("issuerSigned"), "issuerSigned 필드 필요");
        
        System.out.println("✅ Document 필드 검증 완료");
    }
    
    @Test
    @DisplayName("docType 검증")
    void testDocType() throws Exception {
        // Given
        String userId = "test-user";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        String mdlBase64 = (String) result;
        byte[] mdlBytes = Base64.getDecoder().decode(mdlBase64);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> document = CborHelper.decode(mdlBytes, Map.class);
        
        // Then
        String docType = (String) document.get("docType");
        assertEquals("org.iso.18013.5.1.mDL", docType, "docType은 mDL이어야 함");
        
        System.out.println("✅ docType 검증 완료: " + docType);
    }
    
    @Test
    @DisplayName("IssuerSigned 포함 확인")
    void testIssuerSignedPresence() throws Exception {
        // Given
        String userId = "test-user";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        String mdlBase64 = (String) result;
        byte[] mdlBytes = Base64.getDecoder().decode(mdlBase64);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> document = CborHelper.decode(mdlBytes, Map.class);
        
        // Then
        assertTrue(document.containsKey("issuerSigned"), "IssuerSigned 필요");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> issuerSigned = (Map<String, Object>) document.get("issuerSigned");
        assertNotNull(issuerSigned, "IssuerSigned는 null이 아니어야 함");
        
        // IssuerSigned 필드 검증
        assertTrue(issuerSigned.containsKey("nameSpaces"), "nameSpaces 필요");
        assertTrue(issuerSigned.containsKey("issuerAuth"), "issuerAuth 필요");
        
        System.out.println("✅ IssuerSigned 검증 완료");
        System.out.println("   - nameSpaces: 포함됨");
        System.out.println("   - issuerAuth: 포함됨");
    }
    
    @Test
    @DisplayName("DeviceSigned는 없어야 함 (Device 책임)")
    void testDeviceSignedAbsent() throws Exception {
        // Given
        String userId = "test-user";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        String mdlBase64 = (String) result;
        byte[] mdlBytes = Base64.getDecoder().decode(mdlBase64);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> document = CborHelper.decode(mdlBytes, Map.class);
        
        // Then
        assertFalse(document.containsKey("deviceSigned"), 
            "DeviceSigned는 없어야 함 (Device가 추가함) - Section 9.1.3.4");
        
        System.out.println("✅ DeviceSigned 미포함 확인 (정상, Device 책임)");
    }
    
    @Test
    @DisplayName("IssuerAuth 바이트 검증")
    void testIssuerAuthBytes() throws Exception {
        // Given
        String userId = "test-user";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        String mdlBase64 = (String) result;
        byte[] mdlBytes = Base64.getDecoder().decode(mdlBase64);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> document = CborHelper.decode(mdlBytes, Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> issuerSigned = (Map<String, Object>) document.get("issuerSigned");
        Object issuerAuth = issuerSigned.get("issuerAuth");
        
        // Then
        assertNotNull(issuerAuth, "issuerAuth는 null이 아니어야 함");
        assertTrue(issuerAuth instanceof byte[], "issuerAuth는 바이트 배열이어야 함");
        
        byte[] issuerAuthBytes = (byte[]) issuerAuth;
        assertTrue(issuerAuthBytes.length > 0, "issuerAuth 크기는 0보다 커야 함");
        
        System.out.println("✅ IssuerAuth 검증 완료");
        System.out.println("   - 크기: " + issuerAuthBytes.length + " bytes");
    }
    
    @Test
    @DisplayName("mDL 데이터 요소 검증")
    void testMdocDataElements() throws Exception {
        // Given
        String userId = "test-user";
        String credentialType = "org.iso.18013.5.1.mDL";
        String cNonce = "nonce";
        
        // When
        Object result = mdocIssuer.issue(userId, credentialType, cNonce);
        String mdlBase64 = (String) result;
        byte[] mdlBytes = Base64.getDecoder().decode(mdlBase64);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> document = CborHelper.decode(mdlBytes, Map.class);
        
        // Then
        assertNotNull(document, "Document은 null이 아니어야 함");
        
        // 발급 가능함 확인
        assertTrue(document.containsKey("docType"), "발급된 Document 확인");
        assertTrue(document.containsKey("issuerSigned"), "IssuerSigned 확인");
        
        System.out.println("✅ mDL 데이터 요소 검증 완료");
        System.out.println("   - Document 생성: ✅");
        System.out.println("   - IssuerSigned 생성: ✅");
    }
}
