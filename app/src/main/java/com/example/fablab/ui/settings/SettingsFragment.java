package com.example.fablab.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.fablab.R;
import com.example.fablab.ui.authen.LoginUser;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth mAuth;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        mAuth = FirebaseAuth.getInstance();

        ListPreference themePreference = findPreference("theme_preference");

        if (themePreference != null) {
            // Set up the dropdown list with theme options
            CharSequence[] themeEntries = {"FABLAB", "Dabasmāja"};
            CharSequence[] themeValues = {"Theme.FABLAB", "Theme.Dabasmāja"};
            themePreference.setEntries(themeEntries);
            themePreference.setEntryValues(themeValues);

            // Listen for changes to the theme preference
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Get the selected theme value
                String selectedTheme = (String) newValue;
                // Apply the selected theme
                applyTheme(selectedTheme);
                return true;
            });
        }

        // Sign out button
        findPreference("sign_out").setOnPreferenceClickListener(preference -> {
            signOut();
            return true;
        });
    }

    private void applyTheme(String themeName) {
        int themeResourceId = getResources().getIdentifier(themeName, "style", requireContext().getPackageName());
        if (themeResourceId != 0) {
            getActivity().setTheme(themeResourceId);
            // Recreate the activity to apply the new theme
            getActivity().recreate();
        }
    }

    private void signOut() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        // Redirect to login activity
        Intent intent = new Intent(getActivity(), LoginUser.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}