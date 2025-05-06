package com.example.fablab.ui.authen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class ForgotPasswordActivity extends AppCompatActivity {

    private Button buttonPwdReset;
    private EditText editTextPwdResetEmail;
    private ProgressBar progressBar;
    private FirebaseAuth authProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Pielieto tēmu un valodu pirms izkārtojuma uzstādīšanas
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

        // Uzstāda lokalizāciju pēc izvēlētās valodas
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        // Atjauno konfigurāciju un displeja parametrus
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        // Uzstāda izkārtojumu un inicializē Firebase autentifikāciju
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authProfile = FirebaseAuth.getInstance();
        authProfile.signOut(); // Izrakstās no jebkura aktīvā lietotāja

        // Inicializē UI komponentes
        editTextPwdResetEmail = findViewById(R.id.editText_password_reset_email);
        buttonPwdReset = findViewById(R.id.button_password_reset);
        progressBar = findViewById(R.id.progressBar);

        // Uzstāda noklikšķināšanas notikumu pogai
        buttonPwdReset.setOnClickListener(v -> {
            String email = editTextPwdResetEmail.getText().toString();

            // Pārbauda, vai e-pasts ir ievadīts un derīgs
            if (TextUtils.isEmpty(email)) {
                editTextPwdResetEmail.setError(getString(R.string.email_missing));
                editTextPwdResetEmail.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextPwdResetEmail.setError(getString(R.string.email_incorrect));
                editTextPwdResetEmail.requestFocus();
            } else {
                // Rāda ielādes indikatoru un sāk paroles atiestatīšanas procesu
                progressBar.setVisibility(View.VISIBLE);
                resetPasswrod(email);
            }
        });
    }

    // Sūta paroles atiestatīšanas e-pastu, ja lietotājs eksistē
    private void resetPasswrod(String email) {
        authProfile.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ForgotPasswordActivity.this, getString(R.string.checkemail), Toast.LENGTH_SHORT).show();

                // Pāriet atpakaļ uz pieslēgšanās ekrānu un notīra steku
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginUser.class);
                getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            // Paslēpj ielādes indikatoru pēc darbības beigām
            progressBar.setVisibility(View.GONE);
        });
    }
}
