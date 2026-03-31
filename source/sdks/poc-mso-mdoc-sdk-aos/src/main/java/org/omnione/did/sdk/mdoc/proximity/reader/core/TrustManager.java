package org.omnione.did.sdk.mdoc.proximity.reader.core;

import android.util.Base64;
import android.util.Log;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustManager {
    private static final String TAG = "MDR/Trust";
    private final List<X509Certificate> trustedCertificates;
    private final boolean includeSystemRoots;

    public TrustManager(List<X509Certificate> trustedCertificates) {
        this(trustedCertificates, false);
    }

    public TrustManager(List<X509Certificate> trustedCertificates, boolean includeSystemRoots) {
        this.trustedCertificates = trustedCertificates;
        this.includeSystemRoots = includeSystemRoots;
    }

    public static X509Certificate pemToX509Certificate(String pem) throws Exception {
        String base64 = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.decode(base64, Base64.DEFAULT);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

    // 문서의 발급자 인증서가 신뢰 루트 인증서로 체이닝되는지 검증
    // 1. COSE_Sign1 unprotected header에서 x5chain 인증서 추출
    // 2. 인증서 경로 구성
    // 3. PKIX를 사용하여 신뢰 루트 대비 체인 검증
    public boolean isDocumentTrusted(DeviceResponseParser.ParsedDocument document) {
        if (document.getIssuerSignedInfo() == null) return false;
        CBORObject coseSign1 = document.getIssuerSignedInfo().getCoseSign1Items();
        if (coseSign1 == null || coseSign1.size() < 4) return false;
        if (trustedCertificates == null || trustedCertificates.isEmpty()) {
            Log.w(TAG, "No trusted certificates configured");
            return false;
        }

        try {
            // 1. unprotected 헤더에서 x5chain 추출
            List<X509Certificate> chain = extractCertChain(coseSign1.get(1));
            if (chain.isEmpty()) {
                Log.w(TAG, "No x5chain certificate found in issuerAuth");
                return false;
            }

            // 2. 신뢰 루트 인증서 대비 인증서 체인 검증
            return validateCertChain(chain);
        } catch (Exception e) {
            Log.w(TAG, "Trust validation failed", e);
            return false;
        }
    }

    // COSE_Sign1 unprotected header에서 x5chain 인증서 추출 (COSE label 33)
    private List<X509Certificate> extractCertChain(CBORObject unprotectedHeaders) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        if (unprotectedHeaders.getType() != CBORType.Map) return chain;

        // x5chain: COSE 라벨 33 (정수 키)
        CBORObject x5chainItem = unprotectedHeaders.get(CBORObject.FromObject(33));
        if (x5chainItem == null) {
            // 폴백: 문자열 키 "x5chain"
            x5chainItem = unprotectedHeaders.get(CBORObject.FromObject("x5chain"));
        }
        if (x5chainItem == null) return chain;

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        if (x5chainItem.getType() == CBORType.ByteString) {
            // 단일 인증서
            byte[] certBytes = x5chainItem.GetByteString();
            chain.add((X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes)));
        } else if (x5chainItem.getType() == CBORType.Array) {
            // 인증서 배열 (리프 먼저, 루트 마지막)
            for (int i = 0; i < x5chainItem.size(); i++) {
                CBORObject certItem = x5chainItem.get(i);
                if (certItem.getType() == CBORType.ByteString) {
                    byte[] certBytes = certItem.GetByteString();
                    chain.add((X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes)));
                }
            }
        }

        return chain;
    }

    // PKIX를 사용하여 신뢰 루트 인증서 대비 인증서 체인 검증
    // 오프라인 BLE 시나리오이므로 폐기 확인은 비활성화
    private boolean validateCertChain(List<X509Certificate> chain) {
        try {
            Set<TrustAnchor> trustAnchors = new HashSet<>();
            for (X509Certificate trusted : trustedCertificates) {
                trustAnchors.add(new TrustAnchor(trusted, null));
            }
            if (includeSystemRoots) {
                for (X509Certificate systemRoot : getSystemRootCertificates()) {
                    trustAnchors.add(new TrustAnchor(systemRoot, null));
                }
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(chain);

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXParameters params = new PKIXParameters(trustAnchors);
            params.setRevocationEnabled(false);
            validator.validate(certPath, params);

            Log.i(TAG, "Certificate chain validated successfully against trusted roots");
            return true;
        } catch (CertPathValidatorException e) {
            Log.w(TAG, "Certificate chain validation failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Certificate chain validation error", e);
            return false;
        }
    }

    public static List<X509Certificate> getSystemRootCertificates() {
        List<X509Certificate> systemCerts = new ArrayList<>();
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    X509Certificate[] accepted = ((X509TrustManager) tm).getAcceptedIssuers();
                    if (accepted != null) {
                        Collections.addAll(systemCerts, accepted);
                    }
                }
            }
            Log.i(TAG, "Loaded " + systemCerts.size() + " system root certificates");
        } catch (Exception e) {
            Log.w(TAG, "Failed to load system root certificates", e);
        }
        return systemCerts;
    }

    public static List<X509Certificate> loadCertificatesFromPem(List<String> pemStrings) {
        List<X509Certificate> certs = new ArrayList<>();
        for (String pem : pemStrings) {
            try {
                certs.add(pemToX509Certificate(pem));
            } catch (Exception e) {
                Log.w(TAG, "PEM 인증서 파싱 실패", e);
            }
        }
        return certs;
    }
}
