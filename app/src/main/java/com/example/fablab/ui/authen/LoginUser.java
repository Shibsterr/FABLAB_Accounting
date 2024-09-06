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

import java.util.Locale;

public class LoginUser extends AppCompatActivity implements View.OnClickListener{
    private EditText email,password;
    private Button loginbtn,forgorbtn,login;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_login);

        login = (Button) findViewById(R.id.register_swap);
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        loginbtn = (Button) findViewById(R.id.login_Button);
        forgorbtn = (Button) findViewById(R.id.forgot_pass);
        progressBar = (ProgressBar) findViewById(R.id.progress_login);
        mAuth = FirebaseAuth.getInstance();

        loginbtn.setVisibility(View.VISIBLE);

        loginbtn.setOnClickListener((View.OnClickListener) this);
        forgorbtn.setOnClickListener((View.OnClickListener) this);
        login.setOnClickListener((View.OnClickListener) this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.login_Button){ //log the user in
            userLogin();
        }else if(v.getId() == R.id.register_swap){  //swap to registration
            Intent intentreg = new Intent(LoginUser.this, RegisterUser.class);
            intentreg.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intentreg);
        }else if(v.getId() == R.id.forgot_pass){    //password reset
            Log.d("LoginUser", "Clicked message to reset password");
            Intent intent = new Intent(LoginUser.this, ForgotPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void userLogin() {
        String mail = email.getText().toString();
        String pass = password.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            email.setError("Lūdzu ievadiet pareizu e-pastu!");
            email.requestFocus();
            return;
        }

        if (mail.isEmpty()) {
            email.setError("E-pasts nav ievadīts!");
            email.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            password.setError("Nav ievadīta parole!");
            password.requestFocus();
            return;
        }

        loginbtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent intent = new Intent(LoginUser.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                Log.d("MainActivity", "Logging in...");
            } else {
                // Check if the error is due to an incorrect password
                if (task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().contains("wrong password")) {
                    Toast.makeText(LoginUser.this, "Nepareiza parole!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginUser.this, "Notikusi kļūda!", Toast.LENGTH_LONG).show();
                }
            }
            loginbtn.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        });
    }
}
