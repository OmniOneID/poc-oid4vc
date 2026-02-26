/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.oid4vc.oid4vci.dto.qr;

import java.util.Arrays;

public class QrImageData {
    private String mediaType;
    private String format;
    private byte[] qrImage;

    public QrImageData() {
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public byte[] getQrImage() {
        if (qrImage == null) {
            return null;
        }
        return Arrays.copyOf(qrImage, qrImage.length);
    }

    public void setQrImage(byte[] qrImage) {
        if (qrImage == null) {
            this.qrImage = null;
        } else {
            this.qrImage = Arrays.copyOf(qrImage, qrImage.length);
        }
    }
}