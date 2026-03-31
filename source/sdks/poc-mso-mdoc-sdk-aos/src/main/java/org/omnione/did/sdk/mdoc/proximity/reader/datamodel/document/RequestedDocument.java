package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document;

import android.os.Parcel;
import android.os.Parcelable;

import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.AttestationType;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.DocumentMode;

import java.util.ArrayList;
import java.util.List;

public class RequestedDocument implements Parcelable {
    private final String id;
    private final AttestationType documentType;
    private final DocumentMode mode;
    private final List<String> claims;

    public RequestedDocument(String id, AttestationType documentType,
                            DocumentMode mode, List<String> claims) {
        this.id = id;
        this.documentType = documentType;
        this.mode = mode;
        this.claims = claims != null ? claims : new ArrayList<>();
    }

    public String getId() { return id; }
    public AttestationType getDocumentType() { return documentType; }
    public DocumentMode getMode() { return mode; }
    public List<String> getClaims() { return claims; }
    public String getDocType() { return documentType.getDocType(); }
    public String getNamespace() { return documentType.getNamespace(); }

    // Parcelable 구현
    protected RequestedDocument(Parcel in) {
        id = in.readString();
        documentType = AttestationType.values()[in.readInt()];
        mode = DocumentMode.values()[in.readInt()];
        claims = new ArrayList<>();
        in.readStringList(claims);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeInt(documentType.ordinal());
        dest.writeInt(mode.ordinal());
        dest.writeStringList(claims);
    }

    public static final Creator<RequestedDocument> CREATOR = new Creator<RequestedDocument>() {
        @Override
        public RequestedDocument createFromParcel(Parcel in) { return new RequestedDocument(in); }
        @Override
        public RequestedDocument[] newArray(int size) { return new RequestedDocument[size]; }
    };
}
