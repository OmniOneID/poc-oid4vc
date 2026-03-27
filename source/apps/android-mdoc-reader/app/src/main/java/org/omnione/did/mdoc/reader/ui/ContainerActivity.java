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
package org.omnione.did.mdoc.reader.ui;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.mdoc.reader.MdocReaderApplication;

public class ContainerActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private static final String TAG = "ContainerActivity";

    private NavController navController;
    private NfcAdapter nfcAdapter;

    // Delegate callback registered by Fragment — if null, tag is ignored
    private volatile NfcAdapter.ReaderCallback delegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        MdocReaderApplication.getPlatformController().registerActivity(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep Reader Mode active for the entire Activity lifecycle — never disable it
        // Include NDEF check so tags can be read in HomeFragment
        if (nfcAdapter != null) {
            nfcAdapter.enableReaderMode(this, this,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B,
                null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    // NfcAdapter.ReaderCallback — always active, delegates to the registered callback
    @Override
    public void onTagDiscovered(Tag tag) {
        NfcAdapter.ReaderCallback cb = delegate;
        if (cb != null) {
            cb.onTagDiscovered(tag);
        } else {
            Log.d(TAG, "NFC tag ignored (no active delegate)");
        }
    }

    // Called from Fragment: only swaps the delegate, does not re-call enableReaderMode
    public void setNfcReaderCallback(NfcAdapter.ReaderCallback callback) {
        this.delegate = callback;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    public NavController getNavController() {
        return navController;
    }
}
