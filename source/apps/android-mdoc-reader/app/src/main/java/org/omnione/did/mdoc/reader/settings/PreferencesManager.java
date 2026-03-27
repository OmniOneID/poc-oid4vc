/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnione.did.mdoc.reader.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.*;

public class PreferencesManager {
    private static final String PREFS_NAME = "mdoc_reader_prefs";

    public static final String KEY_RETAIN_DATA = "retain_data";
    public static final String KEY_BLE_PERIPHERAL_SERVER = "ble_peripheral_server";
    public static final String KEY_SKIP_ISSUER_TRUST = "skip_issuer_trust";

    // Document selection related keys
    private static final String KEY_SELECTED_MODES = "selected_modes";
    private static final String KEY_CHECKED_CLAIMS_PREFIX = "checked_claims_";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void toggleBoolean(String key) {
        boolean current = getBoolean(key, false);
        putBoolean(key, !current);
    }

    // Default values
    public boolean isRetainData() { return getBoolean(KEY_RETAIN_DATA, false); }
    public boolean isBlePeripheralServer() { return getBoolean(KEY_BLE_PERIPHERAL_SERVER, false); }
    public boolean isSkipIssuerTrust() { return getBoolean(KEY_SKIP_ISSUER_TRUST, false); }

    // Document selection persistence

    // Save selected modes (format: "docId:MODE,docId:MODE,...")
    public void saveSelectedModes(Map<String, String> modes) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : modes.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        prefs.edit().putString(KEY_SELECTED_MODES, sb.toString()).apply();
    }

    public Map<String, String> loadSelectedModes() {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = prefs.getString(KEY_SELECTED_MODES, "");
        if (raw.isEmpty()) return result;
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) result.put(parts[0], parts[1]);
        }
        return result;
    }

    // Save selected claims per document
    public void saveCheckedClaims(String docId, Set<String> claims) {
        prefs.edit().putStringSet(KEY_CHECKED_CLAIMS_PREFIX + docId, claims).apply();
    }

    public Set<String> loadCheckedClaims(String docId) {
        Set<String> stored = prefs.getStringSet(KEY_CHECKED_CLAIMS_PREFIX + docId, null);
        return stored != null ? new LinkedHashSet<>(stored) : null;
    }
}
