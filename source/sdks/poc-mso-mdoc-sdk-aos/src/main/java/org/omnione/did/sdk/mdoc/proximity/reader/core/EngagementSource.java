package org.omnione.did.sdk.mdoc.proximity.reader.core;

import android.nfc.tech.IsoDep;

// DeviceEngagement 데이터 소스 추상화 (QR 코드 또는 NFC)
public sealed interface EngagementSource {

    DeviceEngagement resolve() throws Exception;

    record QrCode(String qrCode) implements EngagementSource {
        @Override
        public DeviceEngagement resolve() throws Exception {
            return DeviceEngagement.fromQrCode(qrCode);
        }
    }

    record Nfc(byte[] data) implements EngagementSource {
        @Override
        public DeviceEngagement resolve() throws Exception {
            return DeviceEngagement.fromBytes(data);
        }
    }

    record NfcDataTransfer(byte[] data, IsoDep isoDep, int maxCommandDataLength, int maxResponseDataLength) implements EngagementSource {
        @Override
        public DeviceEngagement resolve() throws Exception {
            return DeviceEngagement.fromBytes(data);
        }
    }
}
