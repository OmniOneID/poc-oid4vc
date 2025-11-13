/*
 * Copyright 2025 OmniOne.
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

package org.omnione.did.sdk.oid4vc.util;

import android.util.Log;

public class LogUtil {
    private static final String TAG = "Test";

    public static void logLongString(String tag, String message) {
        if (message == null || message.length() == 0) {
            Log.d("sangjun", "No message");
            return;
        }

        int maxLogSize = 4000;
        for (int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = Math.min(end, message.length());
            Log.d("sangjun", message.substring(start, end));
        }
    }

    public static void logLongString(String message) {
        logLongString(TAG, message);
    }
}