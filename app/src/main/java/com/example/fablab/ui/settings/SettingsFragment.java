package com.example.fablab.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.example.fablab.ui.authen.ForgotPasswordActivity;
import com.example.fablab.ui.authen.LoginUser;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getPreferenceManager().getSharedPreferences();

        ListPreference themePreference = findPreference("theme_preference");

        if (themePreference != null) {
            // Set up the dropdown list with theme options
            CharSequence[] themeEntries = {"FABLAB", "Dabasmāja"};
            CharSequence[] themeValues = {"Theme.FABLAB", "Theme.Dabasmāja"};
            themePreference.setEntries(themeEntries);
            themePreference.setEntryValues(themeValues);

            // Set the summary of the theme preference to the current theme
            themePreference.setSummaryProvider(preference -> {
                String themeValue = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
                if (themeValue.equals("Theme.FABLAB")) {
                    return "FABLAB";
                } else if (themeValue.equals("Theme.Dabasmāja")) {
                    return "Dabasmāja";
                }
                return "";
            });

            // Listen for changes to the theme preference
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Get the selected theme value
                String selectedTheme = (String) newValue;
                // Save the selected theme to shared preferences
                sharedPreferences.edit().putString("theme_preference", selectedTheme).apply();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            });
        }

        // Sign out button
        findPreference("sign_out").setOnPreferenceClickListener(preference -> {
            signOut();
            return true;
        });
        findPreference("password_reset").setOnPreferenceClickListener(preference -> {
            Log.d("LoginUser", "Clicked message to reset password");
            Intent intent = new Intent(getContext(), ForgotPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            mAuth.signOut();
            return true;
        });
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
