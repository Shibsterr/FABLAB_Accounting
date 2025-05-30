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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Ielādē iestatījumu XML failu
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getPreferenceManager().getSharedPreferences();

        // Valodas iestatīšana
        ListPreference languagePreference = findPreference("language_preference");

        if (languagePreference != null) {
            // Definē pieejamās valodas un to vērtības
            CharSequence[] languageEntries = {"English", "Latvian"};
            CharSequence[] languageValues = {"en", "lv"};
            languagePreference.setEntries(languageEntries);
            languagePreference.setEntryValues(languageValues);

            // Uzstāda valodas izvēles kopsavilkumu
            languagePreference.setSummaryProvider(preference -> {
                String languageValue = sharedPreferences.getString("language_preference", "en");
                Log.d("LanguagePreference", "saved language preference: " + languageValue);
                return languageValue.equals("en") ? "English" : "Latvian";
            });

            // Apstrādā valodas maiņu
            languagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedLanguage = (String) newValue;
                sharedPreferences.edit().putString("language_preference", selectedLanguage).apply();
                changeLanguage(selectedLanguage); // Maina valodu
                restartActivity(); // Pārstartē aktivitāti, lai atjaunotu izmaiņas
                return true;
            });
        }

        // Tēmas iestatīšana
        ListPreference themePreference = findPreference("theme_preference");
        if (themePreference != null) {
            // Definē pieejamās tēmas un to vērtības
            CharSequence[] themeEntries = {"FABLAB", "Dabasmāja"};
            CharSequence[] themeValues = {"Theme.FABLAB", "Theme.Dabasmāja"};
            themePreference.setEntries(themeEntries);
            themePreference.setEntryValues(themeValues);

            // Uzstāda tēmas izvēles kopsavilkumu
            themePreference.setSummaryProvider(preference -> {
                String themeValue = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
                return themeValue.equals("Theme.FABLAB") ? "FABLAB" : "Dabasmāja";
            });

            // Apstrādā motīva maiņu
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedTheme = (String) newValue;
                sharedPreferences.edit().putString("theme_preference", selectedTheme).apply();
                restartActivity(); // Pārstartē aktivitāti, lai piemērotu jauno tēmu
                return true;
            });
        }

        // Izrakstīšanās poga
        findPreference("sign_out").setOnPreferenceClickListener(preference -> {
            signOut();
            return true;
        });

        // Paroles atiestatīšanas poga
        findPreference("password_reset").setOnPreferenceClickListener(preference -> {
            Log.d("LoginUser", "Clicked message to reset password");
            Intent intent = new Intent(getContext(), ForgotPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        });


        findPreference("recent_stations").setOnPreferenceClickListener(preference -> {
            recentstat();
            return true;
        });

    }

    private void recentstat() {
        Log.d("SettingsFragment", "Clicked to reset recently used stations");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(uid)
                    .child("recentlyUsedStations");

            ref.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("SettingsFragment", "Successfully deleted recently used stations");
                    Toast.makeText(getContext(), "Recently used stations reset", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("SettingsFragment", "Failed to delete recently used stations", task.getException());
                    Toast.makeText(getContext(), "Failed to reset recently used stations", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("SettingsFragment", "User not logged in");
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    // Lietotāja izrakstīšanās no Firebase un pāriešana uz pieslēgšanās ekrānu
    private void signOut() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginUser.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // Maina lietotnes valodu
    private void changeLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        // Android API 24+ nepieciešams izmantot createConfigurationContext
        Context context = getContext();
        if (context != null) {
            context = context.createConfigurationContext(configuration);
            Resources newResources = context.getResources();
            Configuration newConfiguration = new Configuration(newResources.getConfiguration());
            newConfiguration.setLocale(locale);
            resources.updateConfiguration(newConfiguration, newResources.getDisplayMetrics());
        }
    }

    // Pārstartē aktivitāti, lai piemērotu valodas vai tēmas izmaiņas
    private void restartActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish(); // Aizver pašreizējo aktivitāti
    }
}