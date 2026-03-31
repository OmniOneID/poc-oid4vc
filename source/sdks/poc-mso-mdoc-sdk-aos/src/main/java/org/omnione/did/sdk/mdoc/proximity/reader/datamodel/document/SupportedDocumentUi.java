package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document;

import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.AttestationType;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.DocumentMode;

import java.util.List;

public class SupportedDocumentUi {
    private final String id;
    private final AttestationType documentType;
    private final List<DocumentMode> modes;

    public SupportedDocumentUi(String id, AttestationType documentType, List<DocumentMode> modes) {
        this.id = id;
        this.documentType = documentType;
        this.modes = modes;
    }

    public String getId() { return id; }
    public AttestationType getDocumentType() { return documentType; }
    public List<DocumentMode> getModes() { return modes; }
}
