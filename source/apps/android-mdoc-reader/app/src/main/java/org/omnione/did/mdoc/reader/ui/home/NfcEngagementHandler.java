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
package org.omnione.did.mdoc.reader.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.util.Log;

public class NfcEngagementHandler implements NfcAdapter.ReaderCallback {
    private static final String TAG = "MDR/NfcHandler";

    public interface NfcEngagementListener {
        void onDeviceEngagementReceived(byte[] deviceEngagementBytes, android.nfc.Tag nfcTag);
    }

    private final Activity activity;
    private final NfcAdapter nfcAdapter;
    private final NfcEngagementListener listener;

    public NfcEngagementHandler(Activity activity, NfcEngagementListener listener) {
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        this.listener = listener;
    }

    public boolean isAvailable() {
        return nfcAdapter != null;
    }

    public void enableReaderMode() {
        if (nfcAdapter != null) {
            int flags = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B;
            nfcAdapter.enableReaderMode(activity, this, flags, null);
        }
    }

    public void disableReaderMode() {
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(activity);
        }
    }

    public void checkIntent(Intent intent) {
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null && rawMessages.length > 0) {
                NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
                NdefRecord record = ndefMessage.getRecords()[0];
                byte[] deviceEngagementBytes = record.getPayload();
                Log.i(TAG, "Received Device Engagement (Intent) size: " + deviceEngagementBytes.length);
                // When received via Intent, extract the Tag from a separate extra
                Tag intentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                listener.onDeviceEngagementReceived(deviceEngagementBytes, intentTag);
                intent.setAction(null);
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null && ndefMessage.getRecords().length > 0) {
                    NdefRecord record = ndefMessage.getRecords()[0];
                    byte[] deviceEngagementBytes = record.getPayload();
                    Log.i(TAG, "Received Device Engagement (Reader Mode) size: " + deviceEngagementBytes.length);
                    activity.runOnUiThread(() -> listener.onDeviceEngagementReceived(deviceEngagementBytes, tag));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to read NDEF tag", e);
            } finally {
                try { ndef.close(); } catch (Exception ignore) {}
            }
        }
    }
}
