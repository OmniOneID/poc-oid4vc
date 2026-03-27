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

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.AttestationType;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.DocumentMode;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.ClaimItem;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.SupportedDocumentUi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeDocumentAdapter extends RecyclerView.Adapter<HomeDocumentAdapter.ViewHolder> {

    public interface StateProvider {
        DocumentMode getSelectedMode(String docId);
        Set<String> getCheckedClaims(String docId);
        List<ClaimItem> getClaimsForDoc(AttestationType type);
    }

    public interface Callback {
        void onModeSelected(SupportedDocumentUi doc, DocumentMode mode);
        // Returns true on successful deselection, false if rejected because it is the last selection
        boolean onDocDeselected(String docId);
        void onClaimToggled(String docId, String claimLabel);
        void onToggleAllClaims(String docId);
    }

    private List<SupportedDocumentUi> items = new ArrayList<>();
    private final Callback callback;
    private StateProvider stateProvider;
    private final Set<String> claimsExpanded = new HashSet<>();

    public HomeDocumentAdapter(Callback callback) {
        this.callback = callback;
    }

    public void submitList(List<SupportedDocumentUi> newItems, StateProvider provider) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        this.stateProvider = provider;
        notifyDataSetChanged();
    }

    public void refreshState() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_home_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportedDocumentUi doc = items.get(position);
        holder.tvDocName.setText(doc.getDocumentType().getDisplayName());

        DocumentMode selectedMode = stateProvider != null ? stateProvider.getSelectedMode(doc.getId()) : null;

        // Set up mode chips
        holder.chipGroup.removeAllViews();
        for (DocumentMode mode : doc.getModes()) {
            Chip chip = new Chip(holder.chipGroup.getContext());
            chip.setText(mode.getDisplayName());
            chip.setCheckable(true);
            chip.setChecked(mode == selectedMode);

            int primaryColor = holder.itemView.getContext().getColor(R.color.primary);
            int accentColor = holder.itemView.getContext().getColor(R.color.accent);
            int chipBgSelected = holder.itemView.getContext().getColor(R.color.chip_selected);
            int white = holder.itemView.getContext().getColor(R.color.white);
            int onSurface = holder.itemView.getContext().getColor(R.color.on_surface);

            chip.setChipBackgroundColor(new ColorStateList(
                new int[][]{ {android.R.attr.state_checked}, {} },
                new int[]{ chipBgSelected, white }
            ));
            chip.setTextColor(new ColorStateList(
                new int[][]{ {android.R.attr.state_checked}, {} },
                new int[]{ primaryColor, onSurface }
            ));
            chip.setChipStrokeColor(new ColorStateList(
                new int[][]{ {android.R.attr.state_checked}, {} },
                new int[]{ primaryColor, onSurface }
            ));
            chip.setChipStrokeWidth(mode == selectedMode ? 2f : 1f);

            chip.setOnClickListener(v -> {
                if (chip.isChecked()) {
                    // Mode selected
                    if (mode == DocumentMode.CUSTOM) {
                        claimsExpanded.add(doc.getId());
                    } else {
                        claimsExpanded.remove(doc.getId());
                    }
                    callback.onModeSelected(doc, mode);
                } else {
                    // Attempt to deselect
                    if (!callback.onDocDeselected(doc.getId())) {
                        // Cannot deselect the last item; only toggle claims display
                        chip.setChecked(true);
                        if (mode == DocumentMode.CUSTOM) {
                            if (claimsExpanded.contains(doc.getId())) {
                                claimsExpanded.remove(doc.getId());
                            } else {
                                claimsExpanded.add(doc.getId());
                            }
                            notifyItemChanged(holder.getAdapterPosition());
                        }
                    } else {
                        claimsExpanded.remove(doc.getId());
                    }
                }
            });
            holder.chipGroup.addView(chip);
        }

        // Claims section - displayed when in Custom mode and expanded
        boolean showClaims = selectedMode == DocumentMode.CUSTOM && claimsExpanded.contains(doc.getId());
        holder.layoutClaims.setVisibility(showClaims ? View.VISIBLE : View.GONE);

        if (showClaims && stateProvider != null) {
            List<ClaimItem> allClaims = stateProvider.getClaimsForDoc(doc.getDocumentType());
            Set<String> checked = stateProvider.getCheckedClaims(doc.getId());

            // Select all checkbox
            holder.cbSelectAll.setOnCheckedChangeListener(null);
            holder.cbSelectAll.setChecked(checked.size() == allClaims.size() && !allClaims.isEmpty());
            holder.cbSelectAll.setOnClickListener(v -> callback.onToggleAllClaims(doc.getId()));

            // Individual claim checkboxes
            holder.layoutClaimList.removeAllViews();
            for (ClaimItem claim : allClaims) {
                CheckBox cb = new CheckBox(holder.layoutClaimList.getContext());
                cb.setText(claim.getLabel());
                cb.setChecked(checked.contains(claim.getLabel()));
                cb.setOnClickListener(v -> callback.onClaimToggled(doc.getId(), claim.getLabel()));
                holder.layoutClaimList.addView(cb);
            }
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocName;
        ChipGroup chipGroup;
        LinearLayout layoutClaims, layoutClaimList;
        CheckBox cbSelectAll;

        ViewHolder(View itemView) {
            super(itemView);
            tvDocName = itemView.findViewById(R.id.tv_doc_name);
            chipGroup = itemView.findViewById(R.id.chip_group_modes);
            layoutClaims = itemView.findViewById(R.id.layout_claims);
            layoutClaimList = itemView.findViewById(R.id.layout_claim_list);
            cbSelectAll = itemView.findViewById(R.id.cb_select_all);
        }
    }
}
