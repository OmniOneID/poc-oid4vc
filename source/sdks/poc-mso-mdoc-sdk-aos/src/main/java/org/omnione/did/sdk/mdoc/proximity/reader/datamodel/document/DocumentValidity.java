package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document;

public class DocumentValidity {
    private final Boolean deviceSignatureValid;
    private final Boolean issuerSignatureValid;
    private final Boolean dataIntegrityIntact;
    private final String signed;
    private final String validFrom;
    private final String validUntil;

    public DocumentValidity() {
        this(null, null, null, null, null, null);
    }

    public DocumentValidity(Boolean deviceSignatureValid, Boolean issuerSignatureValid,
                           Boolean dataIntegrityIntact, String signed,
                           String validFrom, String validUntil) {
        this.deviceSignatureValid = deviceSignatureValid;
        this.issuerSignatureValid = issuerSignatureValid;
        this.dataIntegrityIntact = dataIntegrityIntact;
        this.signed = signed;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public Boolean isDeviceSignatureValid() { return deviceSignatureValid; }
    public Boolean isIssuerSignatureValid() { return issuerSignatureValid; }
    public Boolean isDataIntegrityIntact() { return dataIntegrityIntact; }
    public String getSigned() { return signed; }
    public String getValidFrom() { return validFrom; }
    public String getValidUntil() { return validUntil; }
}
