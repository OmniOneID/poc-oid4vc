package com.example.oid4vc.issuer.util;

import java.util.*;

/**
 * IssuerSignedItem 배열 생성 및 Digest 계산
 * Section 8.3.2.1.2.2, Section 9.1.2.5 참고
 * 
 * IA가 IssuerSignedItem 배열을 생성하고 각 항목의 Digest를 계산합니다.
 */
public class IssuerSignedItemBuilder {
    
    /**
     * mDL Namespace에서 IssuerSignedItem 배열 생성
     * 
     * 각 IssuerSignedItem 구조:
     * {
     *   "digestID": uint,
     *   "random": bstr (32 bytes),
     *   "elementIdentifier": tstr,
     *   "elementValue": any
     * }
     * 
     * Section 9.1.2.5: "Each IssuerSignedItem shall also contain an unpredictable 
     * random or pseudorandom value. This value shall be different for each 
     * IssuerSignedItem and shall have a minimum length of 16 bytes."
     */
    public static List<Map<String, Object>> buildItems(
            Map<String, Object> mdocNamespace) throws Exception {
        
        List<Map<String, Object>> items = new ArrayList<>();
        int digestId = 0;
        
        for (Map.Entry<String, Object> entry : mdocNamespace.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            
            // digestID: 0부터 순차적 증가 (namespace 내에서 유일)
            item.put("digestID", digestId);
            
            // random: 32바이트 난수 (최소 16바이트)
            item.put("random", DigestCalculator.generateRandom(32));
            
            // elementIdentifier: 필드명
            item.put("elementIdentifier", entry.getKey());
            
            // elementValue: 실제 값
            item.put("elementValue", entry.getValue());
            
            items.add(item);
            digestId++;
        }
        
        return items;
    }
    
    /**
     * IssuerSignedItem의 Digest 계산
     * 
     * Section 9.1.2.5: "A digest shall be calculated separately for each data element 
     * present on the mdoc and stored in the MSO. The input for the digest calculation 
     * shall be the IssuerSignedItemBytes element"
     * 
     * 흐름:
     * 1. IssuerSignedItem을 CBOR 인코딩 (IssuerSignedItemBytes)
     * 2. SHA-256으로 digest 계산
     * 3. digestID를 키로 하여 digest 저장
     */
    public static Map<Integer, byte[]> calculateDigests(
            List<Map<String, Object>> items) throws Exception {
        
        Map<Integer, byte[]> digests = new LinkedHashMap<>();
        
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            
            // IssuerSignedItem을 CBOR 인코딩
            byte[] itemBytes = CborHelper.encode(item);
            
            // SHA-256 계산
            byte[] digest = DigestCalculator.calculateDigest(itemBytes);
            
            // digestID를 키로 저장
            Integer digestId = (Integer) item.get("digestID");
            digests.put(digestId, digest);
        }
        
        return digests;
    }
}
