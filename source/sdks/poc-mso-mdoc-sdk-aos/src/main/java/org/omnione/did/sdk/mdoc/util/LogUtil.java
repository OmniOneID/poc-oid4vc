package org.omnione.did.sdk.mdoc.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.StringReader;

public class LogUtil {

    /**
     * Logs a long string by splitting it into smaller chunks.
     *
     * @param tag The tag to use for logging.
     * @param message The message to log.
     */
    public static void logLongString(String tag, String message) {
        if (message == null || message.length() == 0) {
            android.util.Log.d(tag, "No message");
            return;
        }

        int maxLogSize = 3000;
        for (int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = Math.min(end, message.length());
            android.util.Log.d(tag, message.substring(start, end));
        }
    }

    /**
     * Logs a long string using a default tag.
     *
     * @param message The message to log.
     */
    public static void logLongString(String message) {
        logLongString(null, message);
    }

    /**
     * Extracts a specific field from a JSON response string.
     *
     * @param jsonResponse The JSON response string.
     * @param fieldName The name of the field to extract.
     * @return The extracted field value as a string, or null if not found or an error occurs.
     */
    public static String extractJsonField(String jsonResponse, String fieldName) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
            if (jsonObject.has(fieldName) && !jsonObject.get(fieldName).isJsonNull()) {
                if (jsonObject.get(fieldName).isJsonObject()) {
                    return jsonObject.get(fieldName).toString();
                }
                return jsonObject.get(fieldName).getAsString();
            }
        } catch (JsonSyntaxException e) {
        }
        return null;
    }
}
