package com.example.oid4vc.issuer.util;

import java.util.*;

/**
 * mDL Document 구성 (IA 책임)
 * Section 8.3.2.1.2.2 (mdoc response structure) 참고
 * 
 * ✅ IA는 IssuerSigned만 생성하여 반환
 * ❌ DeviceSigned는 Issuer가 생성하지 않음 (Device 책임)
 * Section 9.1.3.4: "The mdoc shall generate this structure"
 */
public class MdocDocumentBuilder {
    
    /**
     * mDL Document 생성 (IssuerSigned만)
     * 
     * 최종 구조:
     * {
     *   "docType": "org.iso.18013.5.1.mDL",
     *   "issuerSigned": {...완성...}     ✅ IA 책임
     * }
     * 
     * Device가 나중에 DeviceSigned를 추가하면:
     * {
     *   "docType": "org.iso.18013.5.1.mDL",
     *   "issuerSigned": {...},           ✅ IA 생성
     *   "deviceSigned": {...}            ✅ Device 추가
     * }
     */
    public static Map<String, Object> buildDocument(
            List<Map<String, Object>> issuerSignedItems,
            byte[] issuerAuthCose) throws Exception {
        
        Map<String, Object> document = new LinkedHashMap<>();
        
        // 1. Document Type
        document.put("docType", "org.iso.18013.5.1.mDL");
        
        // 2. IssuerSigned (IA가 완성) ✅
        // 이것이 유일한 책임
        Map<String, Object> issuerSigned = buildIssuerSigned(
            issuerSignedItems,
            issuerAuthCose
        );
        document.put("issuerSigned", issuerSigned);
        
        return document;
    }
    
    /**
     * IssuerSigned 구조 생성 (IA 완성)
     * Section 9.1.2 (Issuer Data Authentication) 참고
     * 
     * IssuerSigned = {
     *   "nameSpaces": NameSpaces,
     *   "issuerAuth": IssuerAuth (COSE_Sign1)
     * }
     * 
     * NameSpaces = {
     *   NameSpace => [+ IssuerSignedItem]
     * }
     */
    private static Map<String, Object> buildIssuerSigned(
            List<Map<String, Object>> issuerSignedItems,
            byte[] issuerAuthCose) throws Exception {
        
        Map<String, Object> issuerSigned = new LinkedHashMap<>();
        
        // NameSpaces 구조 생성
        // 각 IssuerSignedItem을 CBOR 바이트로 변환하여 배열로 구성
        Map<String, List<byte[]>> nameSpaces = new LinkedHashMap<>();
        List<byte[]> items = new ArrayList<>();
        
        for (Map<String, Object> item : issuerSignedItems) {
            byte[] itemBytes = CborHelper.encode(item);
            items.add(itemBytes);
        }
        
        nameSpaces.put("org.iso.18013.5.1", items);
        issuerSigned.put("nameSpaces", nameSpaces);
        
        // issuerAuth: COSE Sign1 (IA 서명)
        // Section 9.1.2.4: "The MSO is encapsulated and signed by the untagged 
        // COSE_Sign1 structure... The payload shall be MobileSecurityObjectBytes"
        issuerSigned.put("issuerAuth", issuerAuthCose);
        
        return issuerSigned;
    }
}
