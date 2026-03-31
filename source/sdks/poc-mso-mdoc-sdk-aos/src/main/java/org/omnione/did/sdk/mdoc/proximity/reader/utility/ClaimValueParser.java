package org.omnione.did.sdk.mdoc.proximity.reader.utility;

import android.util.Base64;

import java.util.Collection;
import java.util.Map;

public class ClaimValueParser {

    // ISO 5218 성별 코드
    private static final int GENDER_NOT_KNOWN = 0;
    private static final int GENDER_MALE = 1;
    private static final int GENDER_FEMALE = 2;
    private static final int GENDER_NOT_APPLICABLE = 9;

    // 클레임 값을 사람이 읽을 수 있는 문자열로 변환
    public static String parseValue(String key, Object value) {
        if (value == null) return "";
        if (value instanceof Boolean) return (Boolean) value ? "yes" : "no";
        if (Constants.GENDER_KEYS.contains(key)) return parseGender(value);
        if (Constants.KEY_USER_PSEUDONYM.equals(key) && value instanceof byte[]) return new String((byte[]) value);
        if (value instanceof byte[]) return Base64.encodeToString((byte[]) value, Base64.NO_WRAP);
        if (value instanceof Map) return formatMap((Map<?, ?>) value);
        if (value instanceof Collection) return formatCollection((Collection<?>) value);
        return value.toString();
    }

    // Map 타입 클레임을 줄바꿈으로 구분된 문자열로 변환
    private static String formatMap(Map<?, ?> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (result.length() > 0) result.append("\n");
            result.append(entry.getKey()).append(": ").append(parseValue("", entry.getValue()));
        }
        return result.toString();
    }

    // Collection 타입 클레임을 줄바꿈으로 구분된 문자열로 변환
    private static String formatCollection(Collection<?> collection) {
        StringBuilder result = new StringBuilder();
        for (Object item : collection) {
            if (result.length() > 0) result.append("\n");
            result.append(parseValue("", item));
        }
        return result.toString();
    }

    // ISO 5218 성별 코드를 문자열로 변환
    private static String parseGender(Object value) {
        int code;
        if (value instanceof Number) {
            code = ((Number) value).intValue();
        } else {
            try {
                code = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return value.toString();
            }
        }
        switch (code) {
            case GENDER_NOT_KNOWN: return "Not known";
            case GENDER_MALE: return "Male";
            case GENDER_FEMALE: return "Female";
            case GENDER_NOT_APPLICABLE: return "Not applicable";
            default: return value.toString();
        }
    }
}
