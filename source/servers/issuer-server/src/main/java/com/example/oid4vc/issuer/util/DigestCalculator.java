package com.example.oid4vc.issuer.util;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * SHA-256 Digest 계산 및 Random 생성
 * Section 9.1.2.5 참고
 */
public class DigestCalculator {
    
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 데이터의 SHA-256 Digest 계산
     */
    public static byte[] calculateDigest(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        return digest.digest(data);
    }
    
    /**
     * 난수 생성 (최소 16바이트 권장, 32바이트 사용)
     * Section 9.1.2.5: "shall have a minimum length of 16 bytes"
     */
    public static byte[] generateRandom(int length) {
        byte[] random = new byte[length];
        SECURE_RANDOM.nextBytes(random);
        return random;
    }
    
    /**
     * 기본 길이(32 바이트)로 난수 생성
     */
    public static byte[] generateRandom() {
        return generateRandom(32);
    }
}
