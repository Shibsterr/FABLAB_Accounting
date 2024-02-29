package com.example.fablab.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fablab.R;
import com.example.fablab.databinding.FragmentSettingsBinding;
import com.example.fablab.ui.authen.RegisterUser;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private Button sign_out;
    @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_settings,container,false);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        sign_out = view.findViewById(R.id.signout);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        sign_out.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), RegisterUser.class));
            mAuth.signOut();
            getActivity().finish();
            Log.d("MainActivity","Signing out");
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}