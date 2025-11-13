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

package org.omnione.did.sdk.oid4vc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import org.omnione.did.sdk.oid4vc.R;

public class PinActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int MAX_PIN_LENGTH = 4;
    private StringBuilder pinBuilder = new StringBuilder();

    private List<ImageView> dots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
    }

    private void initViews() {
        dots.add(findViewById(R.id.dot1));
        dots.add(findViewById(R.id.dot2));
        dots.add(findViewById(R.id.dot3));
        dots.add(findViewById(R.id.dot4));

        findViewById(R.id.button0).setOnClickListener(this);
        findViewById(R.id.button1).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
        findViewById(R.id.button4).setOnClickListener(this);
        findViewById(R.id.button5).setOnClickListener(this);
        findViewById(R.id.button6).setOnClickListener(this);
        findViewById(R.id.button7).setOnClickListener(this);
        findViewById(R.id.button8).setOnClickListener(this);
        findViewById(R.id.button9).setOnClickListener(this);
        findViewById(R.id.buttonClear).setOnClickListener(this);
        findViewById(R.id.buttonBackspace).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            Button button = (Button) v;
            String key = button.getText().toString();

            if (key.equals("X")) {
                pinBuilder.setLength(0);
            } else if (key.equals("⌫")) {
                if (pinBuilder.length() > 0) {
                    pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                }
            } else {
                if (pinBuilder.length() < MAX_PIN_LENGTH) {
                    pinBuilder.append(key);
                }
            }
            updatePinIndicator();
        }
    }

    private void updatePinIndicator() {
        for (int i = 0; i < dots.size(); i++) {
            if (i < pinBuilder.length()) {
                dots.get(i).setImageResource(R.drawable.pin_dot_filled);
            } else {
                dots.get(i).setImageResource(R.drawable.pin_dot_empty);
            }
        }

        if (pinBuilder.length() == MAX_PIN_LENGTH) {
            onPinComplete();
        }
    }

    private void onPinComplete() {
        String pin = pinBuilder.toString();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("PIN_CODE", pin);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}