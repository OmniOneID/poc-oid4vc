package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums;

import android.os.Parcel;
import android.os.Parcelable;

import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.ClaimItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum AttestationType implements Parcelable {
    PID("eu.europa.ec.eudi.pid.1", "eu.europa.ec.eudi.pid.1", "PID",
        Arrays.asList(
            new ClaimItem("family_name"), new ClaimItem("given_name"),
            new ClaimItem("birth_date"), new ClaimItem("expiry_date"),
            new ClaimItem("issuing_country"), new ClaimItem("issuing_authority"),
            new ClaimItem("document_number"), new ClaimItem("portrait"),
            new ClaimItem("sex"), new ClaimItem("nationality"),
            new ClaimItem("issuing_jurisdiction"), new ClaimItem("resident_address"),
            new ClaimItem("resident_country"), new ClaimItem("resident_state"),
            new ClaimItem("resident_city"), new ClaimItem("resident_postal_code"),
            new ClaimItem("age_in_years"), new ClaimItem("age_birth_year"),
            new ClaimItem("age_over_18"), new ClaimItem("issuance_date"),
            new ClaimItem("email_address"), new ClaimItem("resident_street"),
            new ClaimItem("resident_house_number"), new ClaimItem("personal_administrative_number"),
            new ClaimItem("mobile_phone_number"), new ClaimItem("family_name_birth"),
            new ClaimItem("given_name_birth"), new ClaimItem("place_of_birth"),
            new ClaimItem("trust_anchor")
        )),
    MDL("org.iso.18013.5.1", "org.iso.18013.5.1.mDL", "mDL",
        Arrays.asList(
            new ClaimItem("family_name"), new ClaimItem("given_name"),
            new ClaimItem("birth_date"), new ClaimItem("expiry_date"),
            new ClaimItem("issue_date"), new ClaimItem("issuing_country"),
            new ClaimItem("issuing_authority"), new ClaimItem("document_number"),
            new ClaimItem("portrait"), new ClaimItem("sex"),
            new ClaimItem("nationality"), new ClaimItem("issuing_jurisdiction"),
            new ClaimItem("resident_address"), new ClaimItem("resident_country"),
            new ClaimItem("resident_state"), new ClaimItem("resident_city"),
            new ClaimItem("resident_postal_code"), new ClaimItem("age_in_years"),
            new ClaimItem("age_birth_year"), new ClaimItem("age_over_18"),
            new ClaimItem("driving_privileges"), new ClaimItem("un_distinguishing_sign"),
            new ClaimItem("administrative_number"), new ClaimItem("height"),
            new ClaimItem("weight"), new ClaimItem("eye_colour"),
            new ClaimItem("hair_colour"), new ClaimItem("birth_place"),
            new ClaimItem("portrait_capture_date"), new ClaimItem("biometric_template_xx"),
            new ClaimItem("family_name_national_character"), new ClaimItem("given_name_national_character"),
            new ClaimItem("signature_usual_mark")
        ));

    private final String namespace;
    private final String docType;
    private final String displayName;
    private final List<ClaimItem> claims;

    AttestationType(String namespace, String docType, String displayName, List<ClaimItem> claims) {
        this.namespace = namespace;
        this.docType = docType;
        this.displayName = displayName;
        this.claims = Collections.unmodifiableList(claims);
    }

    public String getNamespace() { return namespace; }
    public String getDocType() { return docType; }
    public String getDisplayName() { return displayName; }
    public List<ClaimItem> getClaims() { return claims; }

    public static AttestationType fromDocType(String docType) {
        for (AttestationType type : values()) {
            if (type.docType.equals(docType)) return type;
        }
        throw new IllegalArgumentException("Unknown docType: " + docType);
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public static final Creator<AttestationType> CREATOR = new Creator<AttestationType>() {
        @Override
        public AttestationType createFromParcel(Parcel in) {
            return AttestationType.values()[in.readInt()];
        }
        @Override
        public AttestationType[] newArray(int size) {
            return new AttestationType[size];
        }
    };
}
