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

import org.omnione.did.mdoc.reader.config.ConfigProvider;
import org.omnione.did.mdoc.reader.settings.PreferencesManager;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.AttestationType;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.common.enums.DocumentMode;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.ClaimItem;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.RequestedDocument;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.SupportedDocumentUi;

import java.util.*;

public class DocumentSelectionManager implements HomeDocumentAdapter.StateProvider {

    public interface SelectionChangeListener {
        void onSelectionChanged();
    }

    private final ConfigProvider configProvider;
    private final PreferencesManager preferencesManager;
    private SelectionChangeListener listener;

    private final List<SupportedDocumentUi> supportedDocs = new ArrayList<>();
    private final Map<String, DocumentMode> selectedModes = new LinkedHashMap<>();
    private final Map<String, Set<String>> checkedClaims = new LinkedHashMap<>();

    public DocumentSelectionManager(ConfigProvider configProvider, PreferencesManager preferencesManager) {
        this.configProvider = configProvider;
        this.preferencesManager = preferencesManager;
    }

    public void setSelectionChangeListener(SelectionChangeListener listener) {
        this.listener = listener;
    }

    public List<SupportedDocumentUi> getSupportedDocs() {
        return supportedDocs;
    }

    public void loadDocuments() {
        supportedDocs.clear();
        for (AttestationType type : AttestationType.values()) {
            List<DocumentMode> modes = configProvider.getDocumentModes(type);
            String id = type.name();
            supportedDocs.add(new SupportedDocumentUi(id, type, modes));
        }

        // Restore saved selection; if none exists, default to mDL Full
        if (!restoreSelection()) {
            for (SupportedDocumentUi doc : supportedDocs) {
                if (doc.getDocumentType() == AttestationType.MDL) {
                    selectMode(doc, DocumentMode.FULL);
                    break;
                }
            }
        }
    }

    public void selectMode(SupportedDocumentUi doc, DocumentMode mode) {
        selectedModes.put(doc.getId(), mode);
        if (mode == DocumentMode.FULL) {
            Set<String> all = new LinkedHashSet<>();
            for (ClaimItem ci : configProvider.getDocumentClaims(doc.getDocumentType())) {
                all.add(ci.getLabel());
            }
            checkedClaims.put(doc.getId(), all);
        } else {
            if (!checkedClaims.containsKey(doc.getId())) {
                checkedClaims.put(doc.getId(), new LinkedHashSet<>());
            }
        }
        notifyChanged();
        persistSelection();
    }

    public boolean deselectDocument(String docId) {
        if (selectedModes.size() <= 1 && selectedModes.containsKey(docId)) {
            return false;
        }
        selectedModes.remove(docId);
        checkedClaims.remove(docId);
        preferencesManager.saveCheckedClaims(docId, Collections.emptySet());
        notifyChanged();
        persistSelection();
        return true;
    }

    public void toggleClaim(String docId, String claimLabel) {
        Set<String> claims = checkedClaims.get(docId);
        if (claims == null) {
            claims = new LinkedHashSet<>();
            checkedClaims.put(docId, claims);
        }
        if (claims.contains(claimLabel)) {
            claims.remove(claimLabel);
        } else {
            claims.add(claimLabel);
        }
        notifyChanged();
        persistSelection();
    }

    public void toggleAllClaims(String docId, AttestationType type) {
        List<ClaimItem> allItems = configProvider.getDocumentClaims(type);
        Set<String> claims = checkedClaims.get(docId);
        if (claims == null) claims = new LinkedHashSet<>();

        if (claims.size() == allItems.size()) {
            claims.clear();
        } else {
            claims.clear();
            for (ClaimItem ci : allItems) claims.add(ci.getLabel());
        }
        checkedClaims.put(docId, claims);
        notifyChanged();
        persistSelection();
    }

    public List<RequestedDocument> buildRequestedDocuments() {
        List<RequestedDocument> result = new ArrayList<>();
        for (SupportedDocumentUi doc : supportedDocs) {
            DocumentMode mode = selectedModes.get(doc.getId());
            if (mode == null) continue;

            Set<String> claims = checkedClaims.get(doc.getId());
            if (claims == null || claims.isEmpty()) continue;

            result.add(new RequestedDocument(
                doc.getId(), doc.getDocumentType(), mode, new ArrayList<>(claims)));
        }
        return result;
    }

    public boolean hasValidSelection() {
        for (SupportedDocumentUi doc : supportedDocs) {
            DocumentMode mode = selectedModes.get(doc.getId());
            if (mode != null) {
                Set<String> claims = checkedClaims.get(doc.getId());
                if (claims != null && !claims.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // StateProvider implementation

    @Override
    public DocumentMode getSelectedMode(String docId) {
        return selectedModes.get(docId);
    }

    @Override
    public Set<String> getCheckedClaims(String docId) {
        Set<String> c = checkedClaims.get(docId);
        return c != null ? c : Collections.emptySet();
    }

    @Override
    public List<ClaimItem> getClaimsForDoc(AttestationType type) {
        return configProvider.getDocumentClaims(type);
    }

    // Private

    private boolean restoreSelection() {
        Map<String, String> savedModes = preferencesManager.loadSelectedModes();
        if (savedModes.isEmpty()) return false;

        for (Map.Entry<String, String> entry : savedModes.entrySet()) {
            String docId = entry.getKey();
            try {
                DocumentMode mode = DocumentMode.valueOf(entry.getValue());
                selectedModes.put(docId, mode);

                Set<String> claims = preferencesManager.loadCheckedClaims(docId);
                if (claims != null) {
                    checkedClaims.put(docId, claims);
                } else if (mode == DocumentMode.FULL) {
                    for (SupportedDocumentUi doc : supportedDocs) {
                        if (doc.getId().equals(docId)) {
                            Set<String> all = new LinkedHashSet<>();
                            for (ClaimItem ci : configProvider.getDocumentClaims(doc.getDocumentType())) {
                                all.add(ci.getLabel());
                            }
                            checkedClaims.put(docId, all);
                            break;
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return !selectedModes.isEmpty();
    }

    private void persistSelection() {
        Map<String, String> modes = new LinkedHashMap<>();
        for (Map.Entry<String, DocumentMode> e : selectedModes.entrySet()) {
            modes.put(e.getKey(), e.getValue().name());
        }
        preferencesManager.saveSelectedModes(modes);

        for (Map.Entry<String, Set<String>> e : checkedClaims.entrySet()) {
            preferencesManager.saveCheckedClaims(e.getKey(), e.getValue());
        }
    }

    private void notifyChanged() {
        if (listener != null) listener.onSelectionChanged();
    }
}
