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

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.omnione.did.sdk.oid4vc.data.dto.WalletData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WalletUtil {
    private static final Gson gson = new Gson();

    /**
     * Loads the VC file and parses it into a list of WalletData objects.
     *
     * @param context The application context.
     * @return A list of WalletData items, or an empty list if the file is not found or an error occurs.
     */
    public static List<WalletData> loadVcFileAsVcItem(Context context) {
        File file = new File(context.getFilesDir(), "vc.json");
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            String vcData = new String(buffer, StandardCharsets.UTF_8);
            Type listType = new TypeToken<ArrayList<WalletData>>() {}.getType();
            return gson.fromJson(vcData, listType);
        } catch (IOException | JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Saves a string to a file in the application's internal storage.
     *
     * @param context The application context.
     * @param data The string data to be saved.
     * @param fileName The name of the file to save.
     */
    public static void saveStringToFile(Context context, String data, String fileName) {
        File file = new File(context.getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show();
        }
    }
}
