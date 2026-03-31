package org.omnione.did.sdk.mdoc.proximity.reader.utility;

import java.util.Arrays;
import java.util.List;

public final class Constants {
    private Constants() {}

    public static final String MDOC_PREFIX = "mdoc:";

    public static final String KEY_PORTRAIT = "portrait";
    public static final String KEY_SIGNATURE = "signature_usual_mark";
    public static final String KEY_USER_PSEUDONYM = "user_pseudonym";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_SEX = "sex";

    public static final List<String> GENDER_KEYS = Arrays.asList(KEY_GENDER, KEY_SEX);
    public static final List<String> BASE64_IMAGE_KEYS = Arrays.asList(KEY_PORTRAIT, KEY_SIGNATURE);
}
