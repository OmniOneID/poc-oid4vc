package org.omnione.did.sdk.mdoc.proximity.reader.utility;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class CertificateLoader {
    private static final String TAG = "MDR/CertLoader";
    private static final String CERTS_DIR = "certs";

    private CertificateLoader() {}

    // assets/certs/ 디렉토리의 모든 PEM 인증서를 로드하여 문자열 목록으로 반환
    public static List<String> loadCertificates(Context context) {
        List<String> certs = new ArrayList<>();
        try {
            String[] files = context.getAssets().list(CERTS_DIR);
            if (files == null) return certs;

            for (String fileName : files) {
                if (!fileName.endsWith(".pem") && !fileName.endsWith(".crt")) continue;
                String filePath = CERTS_DIR + "/" + fileName;
                try (InputStream inputStream = context.getAssets().open(filePath)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] readBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(readBuffer)) != -1) {
                        buffer.write(readBuffer, 0, bytesRead);
                    }
                    certs.add(buffer.toString("UTF-8"));
                } catch (Exception e) {
                    Log.w(TAG, "인증서 로드 실패: " + filePath, e);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "인증서 디렉토리 조회 실패: " + CERTS_DIR, e);
        }
        return certs;
    }
}
