package org.omnione.did.sdk.mdoc.proximity.reader.core;

import com.upokecenter.cbor.CBORObject;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;
import java.util.Map;

public class DeviceRequestBuilder {

    public static byte[] build(java.util.List<DocRequest> docRequests) throws Exception {
        CBORObject deviceRequest = CBORObject.NewMap();
        deviceRequest.set(CBORObject.FromObject("version"), CBORObject.FromObject("1.0"));
        deviceRequest.set(CBORObject.FromObject("docRequests"), buildDocRequests(docRequests));

        byte[] result = deviceRequest.EncodeToBytes();
        ProtocolLogger.logDeviceRequestComplete(result);
        return result;
    }

    private static CBORObject buildDocRequests(java.util.List<DocRequest> docRequests) throws Exception {
        CBORObject docReqArray = CBORObject.NewArray();
        for (DocRequest dr : docRequests) {
            CBORObject docReqMap = CBORObject.NewMap();
            CBORObject itemsRequestTagged = CBORObject.FromObjectAndTag(CBORObject.FromObject(buildItemsRequestBytes(dr)), 24);
            docReqMap.set(CBORObject.FromObject("itemsRequest"), itemsRequestTagged);
            docReqArray.Add(docReqMap);
        }
        return docReqArray;
    }

    private static CBORObject buildItemsRequest(DocRequest dr) throws Exception {
        CBORObject tagged = CBORObject.FromObjectAndTag(CBORObject.FromObject(buildItemsRequestBytes(dr)), 24);
        return tagged;
    }

    private static byte[] buildItemsRequestBytes(DocRequest dr) throws Exception {
        CBORObject itemsReq = CBORObject.NewMap();
        itemsReq.set(CBORObject.FromObject("docType"), CBORObject.FromObject(dr.getDocType()));

        CBORObject nameSpaces = CBORObject.NewMap();
        for (java.util.Map.Entry<String, java.util.Map<String, Boolean>> nsEntry : dr.getItemsRequest().entrySet()) {
            ProtocolLogger.logDeviceRequestBuilding(dr.getDocType(), nsEntry.getKey(), nsEntry.getValue());
            CBORObject nsMap = CBORObject.NewMap();
            for (java.util.Map.Entry<String, Boolean> claim : nsEntry.getValue().entrySet()) {
                nsMap.set(CBORObject.FromObject(claim.getKey()), claim.getValue() ? CBORObject.True : CBORObject.False);
            }
            nameSpaces.set(CBORObject.FromObject(nsEntry.getKey()), nsMap);
        }
        itemsReq.set(CBORObject.FromObject("nameSpaces"), nameSpaces);

        byte[] result = itemsReq.EncodeToBytes();
        ProtocolLogger.logItemsRequestCbor(dr.getDocType(), result);
        return result;
    }

    public static class DocRequest {
        private final String docType;
        private final java.util.Map<String, java.util.Map<String, Boolean>> itemsRequest;

        public DocRequest(String docType, java.util.Map<String, java.util.Map<String, Boolean>> itemsRequest) {
            this.docType = docType;
            this.itemsRequest = itemsRequest;
        }

        public String getDocType() { return docType; }
        public java.util.Map<String, java.util.Map<String, Boolean>> getItemsRequest() { return itemsRequest; }
    }
}
