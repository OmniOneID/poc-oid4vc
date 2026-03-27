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
package org.omnione.did.mdoc.reader.ui.showDocuments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.Constants;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.AttestationType;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.DocumentValidity;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.ReceivedDocument;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ClaimValueParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReceivedDocumentsAdapter extends RecyclerView.Adapter<ReceivedDocumentsAdapter.ViewHolder> {
    private static final String TAG = "MDR/ReceivedDocs";
    private static final int PORTRAIT_MAX_HEIGHT_PX = 200;

    private List<ReceivedDocument> documents = new ArrayList<>();

    public void submitList(List<ReceivedDocument> docs) {
        this.documents = docs != null ? docs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_received_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReceivedDocument doc = documents.get(position);
        bindDocTypeHeader(holder, doc);
        bindTrustStatus(holder, doc);
        bindClaims(holder, doc);
        bindValidity(holder, doc);
    }

    // Bind document type header
    private void bindDocTypeHeader(ViewHolder holder, ReceivedDocument doc) {
        try {
            AttestationType type = AttestationType.fromDocType(doc.getDocType());
            holder.tvDocType.setText(type.getDisplayName());
        } catch (Exception e) {
            holder.tvDocType.setText(doc.getDocType());
        }
    }

    // Bind trust status
    private void bindTrustStatus(ViewHolder holder, ReceivedDocument doc) {
        holder.tvTrusted.setText(doc.isTrusted() ? R.string.show_docs_trusted : R.string.show_docs_not_trusted);
        holder.tvTrusted.setTextColor(holder.itemView.getContext().getColor(
            doc.isTrusted() ? R.color.success : R.color.error));
    }

    // Bind claims list
    private void bindClaims(ViewHolder holder, ReceivedDocument doc) {
        holder.layoutClaims.removeAllViews();
        for (Map.Entry<String, Object> entry : doc.getClaims().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (Constants.BASE64_IMAGE_KEYS.contains(key) && value instanceof String) {
                if (tryAddImageClaim(holder, key, (String) value)) continue;
            }
            addTextClaim(holder, key, value);
        }
    }

    // Add Base64 image claim (returns true on success)
    private boolean tryAddImageClaim(ViewHolder holder, String key, String base64Value) {
        try {
            byte[] imageBytes = Base64.decode(base64Value, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) return false;

            ImageView imageView = new ImageView(holder.itemView.getContext());
            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setMaxHeight(PORTRAIT_MAX_HEIGHT_PX);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 8);
            holder.layoutClaims.addView(imageView, params);

            TextView label = new TextView(holder.itemView.getContext());
            label.setText(key);
            label.setTextSize(12);
            holder.layoutClaims.addView(label);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to decode image claim: " + key, e);
            return false;
        }
    }

    // Add text claim
    private void addTextClaim(ViewHolder holder, String key, Object value) {
        View claimView = LayoutInflater.from(holder.itemView.getContext())
            .inflate(R.layout.item_claim_value, holder.layoutClaims, false);
        ((TextView) claimView.findViewById(R.id.tv_claim_key)).setText(key);
        ((TextView) claimView.findViewById(R.id.tv_claim_value)).setText(ClaimValueParser.parseValue(key, value));
        holder.layoutClaims.addView(claimView);
    }

    // Bind validity information
    private void bindValidity(ViewHolder holder, ReceivedDocument doc) {
        holder.layoutValidity.removeAllViews();
        DocumentValidity validity = doc.getValidity();
        if (validity == null) return;

        addValidityItem(holder, R.string.show_docs_device_sig, boolToString(validity.isDeviceSignatureValid()));
        addValidityItem(holder, R.string.show_docs_issuer_sig, boolToString(validity.isIssuerSignatureValid()));
        addValidityItem(holder, R.string.show_docs_data_integrity, boolToString(validity.isDataIntegrityIntact()));
        if (validity.getSigned() != null) addValidityItem(holder, R.string.show_docs_signed, validity.getSigned());
        if (validity.getValidFrom() != null) addValidityItem(holder, R.string.show_docs_valid_from, validity.getValidFrom());
        if (validity.getValidUntil() != null) addValidityItem(holder, R.string.show_docs_valid_until, validity.getValidUntil());
    }

    private void addValidityItem(ViewHolder holder, int labelResId, String value) {
        View view = LayoutInflater.from(holder.itemView.getContext())
            .inflate(R.layout.item_claim_value, holder.layoutValidity, false);
        ((TextView) view.findViewById(R.id.tv_claim_key)).setText(labelResId);
        ((TextView) view.findViewById(R.id.tv_claim_value)).setText(value != null ? value : "N/A");
        holder.layoutValidity.addView(view);
    }

    private String boolToString(Boolean val) {
        if (val == null) return "N/A";
        return val ? "yes" : "no";
    }

    @Override
    public int getItemCount() { return documents.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocType, tvTrusted;
        LinearLayout layoutClaims, layoutValidity;

        ViewHolder(View itemView) {
            super(itemView);
            tvDocType = itemView.findViewById(R.id.tv_doc_type);
            tvTrusted = itemView.findViewById(R.id.tv_trusted);
            layoutClaims = itemView.findViewById(R.id.layout_claims);
            layoutValidity = itemView.findViewById(R.id.layout_validity);
        }
    }
}
