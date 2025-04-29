package com.example.fablab.ui.authen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class LoginUser extends AppCompatActivity implements View.OnClickListener {

    // UI komponentes
    private EditText email, password;
    private Button loginbtn, forgorbtn, login;
    private ProgressBar progressBar;

    // Firebase autentifikācijas un datubāzes atsauces
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ielādē lietotāja izvēlēto valodu no iestatījumiem
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        // Uzstāda lietotāja izvēlēto valodu lietotnei
        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        // Uzstāda tēmu, ko lietotājs izvēlējies iestatījumos
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_login);

        // Inicializē UI komponentes
        login = findViewById(R.id.register_swap);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginbtn = findViewById(R.id.login_Button);
        forgorbtn = findViewById(R.id.forgot_pass);
        progressBar = findViewById(R.id.progress_login);

        // Inicializē Firebase autentifikāciju un datubāzi
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loginbtn.setVisibility(View.VISIBLE);

        // Uzstāda pogu klikšķu klausītājus
        loginbtn.setOnClickListener(this);
        forgorbtn.setOnClickListener(this);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.login_Button) {
            // Pieslēgšanās process
            userLogin();
        } else if (v.getId() == R.id.register_swap) {
            // Pāriet uz reģistrācijas ekrānu
            Intent intentreg = new Intent(LoginUser.this, RegisterUser.class);
            intentreg.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intentreg);
        } else if (v.getId() == R.id.forgot_pass) {
            // Atver aizmirstās paroles atgūšanas ekrānu
            Log.d("LoginUser", "Clicked message to reset password");
            Intent intent = new Intent(LoginUser.this, ForgotPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    // Lietotāja pieslēgšanās funkcija
    private void userLogin() {
        String mail = email.getText().toString();
        String pass = password.getText().toString();

        // Pārbaude vai e-pasts ir korekts
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            email.setError(getString(R.string.email_incorrect));
            email.requestFocus();
            return;
        }
        // Pārbaude vai e-pasts ir ievadīts
        if (mail.isEmpty()) {
            email.setError(getString(R.string.email_missing));
            email.requestFocus();
            return;
        }
        // Pārbaude vai parole ir ievadīta
        if (pass.isEmpty()) {
            password.setError(getString(R.string.password_missing));
            password.requestFocus();
            return;
        }

        // Rāda ielādes indikatoru, paslēpj pieslēgšanās pogu
        loginbtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Firebase pieslēgšanās
        mAuth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Pārbauda lietotāja statusu
                String userId = mAuth.getCurrentUser().getUid();
                checkUserStatus(userId);
            } else {
                // Apstrādā kļūdas gadījumus
                if (task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().contains("wrong password")) {
                    Toast.makeText(LoginUser.this, getString(R.string.incorrect_pass), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginUser.this, getString(R.string.error), Toast.LENGTH_LONG).show();
                }
                loginbtn.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Pārbauda lietotāja konta statusu Firebase datubāzē
    private void checkUserStatus(String userId) {
        mDatabase.child("users").child(userId).child("Statuss").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String status = task.getResult().getValue(String.class);
                if ("deleted".equalsIgnoreCase(status)) {
                    // Ja konts ir dzēsts, atslēdz lietotāju un parāda ziņojumu
                    mAuth.signOut();
                    Toast.makeText(LoginUser.this, getString(R.string.deleted_account), Toast.LENGTH_LONG).show();
                    loginbtn.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                } else {
                    // Ja viss kārtībā, ieiet aplikācijā
                    Intent intent = new Intent(LoginUser.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    Log.d("MainActivity", "Logging in...");
                }
            } else {
                // Ja neizdodas saņemt statusu
                Toast.makeText(LoginUser.this, getString(R.string.error_account_status), Toast.LENGTH_LONG).show();
                loginbtn.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
