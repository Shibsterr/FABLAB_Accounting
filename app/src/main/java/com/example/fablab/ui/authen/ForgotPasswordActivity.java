package com.example.fablab.ui.authen;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private Button buttonPwdReset;
    private EditText editTextPwdResetEmail;
    private ProgressBar progressBar;
    private FirebaseAuth authProfile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editTextPwdResetEmail = findViewById(R.id.editText_password_reset_email);
        buttonPwdReset = findViewById(R.id.button_password_reset);
        progressBar = findViewById(R.id.progressBar);

        buttonPwdReset.setOnClickListener(v -> {
            String email = editTextPwdResetEmail.getText().toString();
            if(TextUtils.isEmpty(email)){
                editTextPwdResetEmail.setError("Lūdzu ievadiet savu epastu!");
                editTextPwdResetEmail.requestFocus();
            }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                editTextPwdResetEmail.setError("Lūdzu ievadiet pariezu epastu!");
                editTextPwdResetEmail.requestFocus();
            }else{
                progressBar.setVisibility(View.VISIBLE);
                resetPasswrod(email);
            }
        });
    }

    private void resetPasswrod(String email) {
        authProfile = FirebaseAuth.getInstance();
        authProfile.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(ForgotPasswordActivity.this, "Lūdzu apstieties savā epastā lai turpinātu tālāk!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ForgotPasswordActivity.this, LoginUser.class);
                getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            progressBar.setVisibility(View.GONE);
        });
    }

}