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
package org.omnione.did.mdoc.reader.di.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.omnione.did.mdoc.reader.R;
import org.omnione.did.mdoc.reader.MdocReaderApplication;
import org.omnione.did.mdoc.reader.databinding.FragmentMenuBinding;

public class MenuFragment extends Fragment {
    private FragmentMenuBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);

        binding.tvTitle.setText(R.string.menu_title);
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        // Menu items
        binding.itemHome.setOnClickListener(v ->
            navController.popBackStack(R.id.homeFragment, false));

        binding.itemSettings.setOnClickListener(v ->
            navController.navigate(R.id.action_menu_to_settings));

        // Display app version
        String version = MdocReaderApplication.getPlatformController().getAppVersion();
        binding.tvAppVersion.setText("v" + version);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
