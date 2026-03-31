package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReceivedDocument {
    private final boolean trusted;
    private final String docType;
    private final Map<String, Object> claims;
    private final DocumentValidity validity;

    public ReceivedDocument(boolean trusted, String docType,
                           Map<String, Object> claims, DocumentValidity validity) {
        this.trusted = trusted;
        this.docType = docType;
        this.claims = claims != null ? claims : new LinkedHashMap<>();
        this.validity = validity != null ? validity : new DocumentValidity();
    }

    public boolean isTrusted() { return trusted; }
    public String getDocType() { return docType; }
    public Map<String, Object> getClaims() { return claims; }
    public DocumentValidity getValidity() { return validity; }
}
