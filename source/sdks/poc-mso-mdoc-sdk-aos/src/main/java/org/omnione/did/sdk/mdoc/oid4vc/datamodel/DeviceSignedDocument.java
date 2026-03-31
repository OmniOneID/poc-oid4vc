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

package org.omnione.did.sdk.mdoc.oid4vc.datamodel;

import com.upokecenter.cbor.CBORObject;
import org.omnione.did.sdk.mdoc.oid4vc.constant.MdocConstants;

public class DeviceSignedDocument {
    private String docType;
    private CBORObject issuerSigned;
    private CBORObject deviceSigned;

    public DeviceSignedDocument(String docType, CBORObject issuerSigned, CBORObject deviceSigned) {
        this.docType = docType;
        this.issuerSigned = issuerSigned;
        this.deviceSigned = deviceSigned;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public CBORObject getIssuerSigned() {
        return issuerSigned;
    }

    public void setIssuerSigned(CBORObject issuerSigned) {
        this.issuerSigned = issuerSigned;
    }

    public CBORObject getDeviceSigned() {
        return deviceSigned;
    }

    public void setDeviceSigned(CBORObject deviceSigned) {
        this.deviceSigned = deviceSigned;
    }

    public CBORObject toDocument() {
        CBORObject document = CBORObject.NewMap();
        document.Add(MdocConstants.Document.DOC_TYPE, docType);
        document.Add(MdocConstants.Document.ISSUER_SIGNED, issuerSigned);
        document.Add(MdocConstants.Document.DEVICE_SIGNED, deviceSigned);
        return document;
    }

    public CBORObject toDeviceResponse() {
        CBORObject deviceResponse = CBORObject.NewMap();
        deviceResponse.Add(MdocConstants.DeviceResponse.VERSION, MdocConstants.DeviceResponse.VERSION_1_0);
        CBORObject documents = CBORObject.NewArray();
        documents.Add(toDocument());
        deviceResponse.Add(MdocConstants.DeviceResponse.DOCUMENTS, documents);
        deviceResponse.Add(MdocConstants.DeviceResponse.STATUS, 0);
        return deviceResponse;
    }
}
