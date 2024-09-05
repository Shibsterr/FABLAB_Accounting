package com.example.fablab.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
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

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getPreferenceManager().getSharedPreferences();

        // Set up language preference
        ListPreference languagePreference = findPreference("language_preference");
        if (languagePreference != null) {
            CharSequence[] languageEntries = {"English", "Latvian"};
            CharSequence[] languageValues = {"en", "lv"};
            languagePreference.setEntries(languageEntries);
            languagePreference.setEntryValues(languageValues);

            // Set the summary of the language preference to the current language
            languagePreference.setSummaryProvider(preference -> {
                String languageValue = sharedPreferences.getString("language_preference", "en");
                return languageValue.equals("en") ? "English" : "Latvian";
            });

            languagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedLanguage = (String) newValue;
                sharedPreferences.edit().putString("language_preference", selectedLanguage).apply();
                changeLanguage(selectedLanguage);
                restartActivity();
                return true;
            });
        }

        // Set up theme preference
        ListPreference themePreference = findPreference("theme_preference");
        if (themePreference != null) {
            CharSequence[] themeEntries = {"FABLAB", "Dabasmāja"};
            CharSequence[] themeValues = {"Theme.FABLAB", "Theme.Dabasmāja"};
            themePreference.setEntries(themeEntries);
            themePreference.setEntryValues(themeValues);

            themePreference.setSummaryProvider(preference -> {
                String themeValue = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
                return themeValue.equals("Theme.FABLAB") ? "FABLAB" : "Dabasmāja";
            });

            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedTheme = (String) newValue;
                sharedPreferences.edit().putString("theme_preference", selectedTheme).apply();
                restartActivity();
                return true;
            });
        }

        // Sign out button
        findPreference("sign_out").setOnPreferenceClickListener(preference -> {
            signOut();
            return true;
        });

        // Password reset button
        findPreference("password_reset").setOnPreferenceClickListener(preference -> {
            Log.d("LoginUser", "Clicked message to reset password");
            Intent intent = new Intent(getContext(), ForgotPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        });
    }

    private void signOut() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginUser.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void changeLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        // For API 24 and above, use createConfigurationContext
        Context context = getContext();
        if (context != null) {
            context = context.createConfigurationContext(configuration);
            Resources newResources = context.getResources();
            Configuration newConfiguration = new Configuration(newResources.getConfiguration());
            newConfiguration.setLocale(locale);
            getResources().updateConfiguration(newConfiguration, newResources.getDisplayMetrics());
        }
    }

    private void restartActivity() {
        // Restart the activity to apply changes
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish(); // Finish the current activity to ensure it is not kept in the back stack
    }
}
