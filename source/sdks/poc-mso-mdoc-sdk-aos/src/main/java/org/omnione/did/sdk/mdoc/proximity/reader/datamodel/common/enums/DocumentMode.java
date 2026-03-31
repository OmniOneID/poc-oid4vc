package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums;

public enum DocumentMode {
    FULL("Full"),
    CUSTOM("Custom");

    private final String displayName;

    DocumentMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
