package com.example.oid4vc.issuer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * mDL 데이터 검증
 * Section 7.2 참고
 */
public class MdocValidator {
    
    private static final int MAX_STRING_LENGTH = 150;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * mDL 필수 필드 검증
     */
    public static void validateMdocRequired(java.util.Map<String, Object> mdocData) {
        String[] requiredFields = {
            "family_name", "given_name", "birth_date", "issue_date", "expiry_date",
            "issuing_country", "issuing_authority", "document_number", "portrait",
            "driving_privileges", "un_distinguishing_sign"
        };
        
        for (String field : requiredFields) {
            if (!mdocData.containsKey(field) || mdocData.get(field) == null) {
                throw new IllegalArgumentException("Required field missing: " + field);
            }
        }
    }
    
    /**
     * 텍스트 필드 검증 (Latin-1, 최대 길이)
     * Section 7.2.1: 모든 tstr 필드는 Latin-1 (ISO/IEC 8859-1)
     */
    public static void validateTextField(String fieldName, String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                fieldName + " exceeds max length: " + value.length() + " > " + maxLength
            );
        }
        
        // Latin-1 (ISO/IEC 8859-1) 검증
        // 문자가 255를 초과하면 Latin-1 범위 밖
        for (char c : value.toCharArray()) {
            if (c > 255) {
                throw new IllegalArgumentException(
                    fieldName + " contains non-Latin1 character: " + c
                );
            }
        }
    }
    
    /**
     * 날짜 필드 검증 (YYYY-MM-DD 포맷)
     * Section 9.1.2.4: 날짜는 YYYY-MM-DD 또는 RFC 3339 형식
     */
    public static LocalDate validateDateField(String fieldName, String dateStr) {
        try {
            return LocalDate.parse(dateStr, ISO_DATE);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                fieldName + " must be in YYYY-MM-DD format: " + dateStr, e
            );
        }
    }
    
    /**
     * 증명사진 검증 (JPEG/JPEG2000)
     * Section 7.2.1: portrait는 JPEG 또는 JPEG2000 형식
     */
    public static void validatePortrait(byte[] portraitBytes) {
        if (portraitBytes == null || portraitBytes.length == 0) {
            throw new IllegalArgumentException("Portrait cannot be empty");
        }
        
        if (portraitBytes.length < 4) {
            throw new IllegalArgumentException("Portrait too small to verify format");
        }
        
        // JPEG 매직 넘버: 0xFF 0xD8
        boolean isJpeg = portraitBytes[0] == (byte) 0xFF && portraitBytes[1] == (byte) 0xD8;
        
        // JPEG2000 매직 넘버: 0x00 0x00 0x00 0x0C
        boolean isJpeg2000 = portraitBytes[0] == 0x00 && portraitBytes[1] == 0x00 && 
                             portraitBytes[2] == 0x00 && portraitBytes[3] == 0x0C;
        
        if (!isJpeg && !isJpeg2000) {
            throw new IllegalArgumentException(
                "Portrait must be JPEG (FF D8) or JPEG2000 (00 00 00 0C) format. " +
                "Got: " + String.format("%02X %02X", portraitBytes[0] & 0xFF, portraitBytes[1] & 0xFF)
            );
        }
    }
    
    /**
     * ISO 3166-1 Alpha-2 국가코드 검증
     */
    public static void validateCountryCode(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            throw new IllegalArgumentException(
                "Country code must be 2 characters (ISO 3166-1 Alpha-2): " + countryCode
            );
        }
        
        for (char c : countryCode.toCharArray()) {
            if (!Character.isUpperCase(c) || !Character.isLetter(c)) {
                throw new IllegalArgumentException(
                    "Country code must be uppercase letters (ISO 3166-1 Alpha-2): " + countryCode
                );
            }
        }
    }
}
