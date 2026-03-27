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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.mdoc.reader.di.DependencyProvider;
import org.omnione.did.sdk.mdoc.proximity.reader.core.TransferController;
import org.omnione.did.mdoc.reader.databinding.FragmentShowDocumentsBinding;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.ReceivedDocument;

import java.util.List;

public class ShowDocumentsFragment extends Fragment {
    private FragmentShowDocumentsBinding binding;
    private TransferController transferController;

    private DependencyProvider deps() {
        return (DependencyProvider) requireActivity().getApplication();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShowDocumentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);
        transferController = deps().transferController();

        binding.tvTitle.setText(R.string.show_docs_title);
        Runnable goHome = () -> {
            transferController.stopConnection();
            navController.navigate(R.id.action_global_pop_to_home);
        };

        binding.btnClose.setOnClickListener(v -> goHome.run());

        requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    goHome.run();
                }
            });

        // RecyclerView setup
        ReceivedDocumentsAdapter adapter = new ReceivedDocumentsAdapter();
        binding.rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDocuments.setAdapter(adapter);

        // Process received document data
        Bundle args = getArguments();
        if (args != null) {
            List<ReceivedDocument> docs = (List<ReceivedDocument>) args.getSerializable("receivedDocs");
            if (docs != null) {
                binding.tvDocCount.setText(getString(R.string.show_docs_num_docs) + ": " + docs.size());
                adapter.submitList(docs);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        transferController.stopConnection();
        binding = null;
    }
}
