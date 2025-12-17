package com.example.oid4vc.issuer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * mDL Namespace 데이터 빌더
 * Section 7.2 (mDL data), Section 9.1.2 (Issuer responsibility) 참고
 * 
 * IA가 mDL 데이터를 준비하여 namespace를 구성합니다.
 */
public class MdocNamespaceBuilder {
    
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String NAMESPACE = "org.iso.18013.5.1";
    
    private Map<String, Object> claims;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuingCountry;
    private String issuingAuthority;
    private String unDistinguishingSign;
    private byte[] portrait;
    
    public MdocNamespaceBuilder withClaims(Map<String, Object> claims) {
        this.claims = new LinkedHashMap<>(claims);
        return this;
    }
    
    public MdocNamespaceBuilder withIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
        return this;
    }
    
    public MdocNamespaceBuilder withExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }
    
    public MdocNamespaceBuilder withIssuingCountry(String issuingCountry) {
        this.issuingCountry = issuingCountry;
        return this;
    }
    
    public MdocNamespaceBuilder withIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
        return this;
    }
    
    public MdocNamespaceBuilder withUnDistinguishingSign(String unDistinguishingSign) {
        this.unDistinguishingSign = unDistinguishingSign;
        return this;
    }
    
    public MdocNamespaceBuilder withPortrait(byte[] portrait) {
        this.portrait = portrait;
        return this;
    }
    
    /**
     * mDL Namespace 데이터 구성 (IA 책임)
     * Section 7.2 mDL data elements 참고
     */
    public Map<String, Object> build() throws Exception {
        Map<String, Object> namespace = new LinkedHashMap<>();
        
        // ===== 필수 필드 =====
        
        // family_name (tstr, Latin-1, max 150)
        String familyName = extractAndValidateText("family_name", 150, true);
        namespace.put("family_name", familyName);
        
        // given_name (tstr, Latin-1, max 150)
        String givenName = extractAndValidateText("given_name", 150, true);
        namespace.put("given_name", givenName);
        
        // birth_date (full-date: YYYY-MM-DD, Tag 1004)
        String birthDateStr = extractAndValidateText("birth_date", -1, true);
        LocalDate parsedBirth = MdocValidator.validateDateField("birth_date", birthDateStr);
        namespace.put("birth_date", parsedBirth.format(ISO_DATE));
        
        // issue_date (tdate/full-date)
        if (issueDate != null) {
            namespace.put("issue_date", issueDate.format(ISO_DATE));
        } else {
            throw new IllegalArgumentException("issue_date is required");
        }
        
        // expiry_date (tdate/full-date)
        if (expiryDate != null) {
            namespace.put("expiry_date", expiryDate.format(ISO_DATE));
        } else {
            throw new IllegalArgumentException("expiry_date is required");
        }
        
        // issuing_country (tstr, ISO 3166-1, max 150)
        if (issuingCountry == null || issuingCountry.isEmpty()) {
            throw new IllegalArgumentException("issuingCountry is required");
        }
        MdocValidator.validateCountryCode(issuingCountry);
        namespace.put("issuing_country", issuingCountry);
        
        // issuing_authority (tstr, Latin-1, max 150)
        if (issuingAuthority == null || issuingAuthority.isEmpty()) {
            throw new IllegalArgumentException("issuingAuthority is required");
        }
        MdocValidator.validateTextField("issuing_authority", issuingAuthority, 150);
        namespace.put("issuing_authority", issuingAuthority);
        
        // document_number (tstr, Latin-1, max 150)
        String documentNumber = extractAndValidateText("id_number", 150, true);
        namespace.put("document_number", documentNumber);
        
        // portrait (bstr: JPEG/JPEG2000 바이너리)
        if (portrait != null && portrait.length > 0) {
            MdocValidator.validatePortrait(portrait);
            namespace.put("portrait", portrait);
        } else {
            throw new IllegalArgumentException("portrait is required");
        }
        
        // driving_privileges (array)
        // Section 7.2.4: 빈 배열 가능
        namespace.put("driving_privileges", new ArrayList<>());
        
        // un_distinguishing_sign (tstr)
        if (unDistinguishingSign == null || unDistinguishingSign.isEmpty()) {
            throw new IllegalArgumentException("unDistinguishingSign is required");
        }
        namespace.put("un_distinguishing_sign", unDistinguishingSign);
        
        // ===== 선택적 필드 =====
        
        // nationality
        if (claims != null && claims.containsKey("nationality")) {
            String nationality = (String) claims.get("nationality");
            if (nationality != null && !nationality.isEmpty()) {
                namespace.put("nationality", nationality);
            }
        }
        
        // sex (uint 0-9, ISO/IEC 5218)
        if (claims != null && claims.containsKey("gender")) {
            Object gender = claims.get("gender");
            if ("male".equalsIgnoreCase(String.valueOf(gender))) {
                namespace.put("sex", 1);  // ISO/IEC 5218: 1=Male
            } else if ("female".equalsIgnoreCase(String.valueOf(gender))) {
                namespace.put("sex", 2);  // ISO/IEC 5218: 2=Female
            }
        }
        
        return namespace;
    }
    
    /**
     * claims에서 텍스트 필드 추출 및 검증
     */
    private String extractAndValidateText(String fieldName, int maxLength, boolean required) {
        Object value = claims != null ? claims.get(fieldName) : null;
        
        if (value == null && required) {
            throw new IllegalArgumentException("Required field missing: " + fieldName);
        }
        
        if (value != null) {
            String strValue = String.valueOf(value);
            if (maxLength > 0) {
                MdocValidator.validateTextField(fieldName, strValue, maxLength);
            }
            return strValue;
        }
        
        return null;
    }
}
