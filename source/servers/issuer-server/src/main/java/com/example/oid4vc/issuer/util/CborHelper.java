package com.example.oid4vc.issuer.util;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CBOR 인코딩/디코딩 헬퍼
 * RFC 7049 CBOR 포맷 사용
 */
public class CborHelper {
    
    private static final ObjectMapper CBOR_MAPPER = createCborMapper();
    
    private static ObjectMapper createCborMapper() {
        CBORFactory factory = new CBORFactory();
        
        // CBOR 인코딩 설정
        // WRITE_MINIMAL_INTS: 정수를 가능한 가장 작은 형태로 인코딩
        factory.enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS);
        
        ObjectMapper mapper = new ObjectMapper(factory);
        return mapper;
    }
    
    /**
     * 객체를 CBOR 바이트로 인코딩
     */
    public static byte[] encode(Object obj) throws Exception {
        return CBOR_MAPPER.writeValueAsBytes(obj);
    }
    
    /**
     * CBOR 바이트를 객체로 디코딩
     */
    public static <T> T decode(byte[] bytes, Class<T> clazz) throws Exception {
        return CBOR_MAPPER.readValue(bytes, clazz);
    }
    
    /**
     * CBOR 바이트를 JSON 문자열로 변환 (Pretty-printed)
     * 
     * 콘솔 출력이나 로깅 용도로 사용
     */
    public static String toJsonString(byte[] cborBytes) throws Exception {
        // CBOR을 Map으로 디코딩
        Object decoded = CBOR_MAPPER.readValue(cborBytes, Object.class);
        
        // JSON Mapper로 Pretty-print
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(decoded);
    }
}
