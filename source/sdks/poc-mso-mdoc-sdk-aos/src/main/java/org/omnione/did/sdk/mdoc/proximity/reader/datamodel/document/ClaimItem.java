package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class ClaimItem implements Parcelable {
    private final String label;

    public ClaimItem(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaimItem that = (ClaimItem) o;
        return Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() { return Objects.hash(label); }

    @Override
    public String toString() { return label; }

    // Parcelable 구현
    protected ClaimItem(Parcel in) { label = in.readString(); }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) { dest.writeString(label); }

    public static final Creator<ClaimItem> CREATOR = new Creator<ClaimItem>() {
        @Override
        public ClaimItem createFromParcel(Parcel in) { return new ClaimItem(in); }
        @Override
        public ClaimItem[] newArray(int size) { return new ClaimItem[size]; }
    };
}
