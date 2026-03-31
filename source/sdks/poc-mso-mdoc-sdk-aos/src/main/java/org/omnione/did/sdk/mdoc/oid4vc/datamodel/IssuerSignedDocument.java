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

import java.util.List;

public class IssuerSignedDocument {
    private byte[] taggedMsoBytes;
    private List<CBORObject> issuerSignedItems;

    public IssuerSignedDocument(byte[] taggedMsoBytes, List<CBORObject> issuerSignedItems) {
        this.taggedMsoBytes = taggedMsoBytes;
        this.issuerSignedItems = issuerSignedItems;
    }

    public byte[] getTaggedMsoBytes() {
        return taggedMsoBytes;
    }

    public void setTaggedMsoBytes(byte[] taggedMsoBytes) {
        this.taggedMsoBytes = taggedMsoBytes;
    }

    public List<CBORObject> getIssuerSignedItems() {
        return issuerSignedItems;
    }

    public void setIssuerSignedItems(List<CBORObject> issuerSignedItems) {
        this.issuerSignedItems = issuerSignedItems;
    }
}
