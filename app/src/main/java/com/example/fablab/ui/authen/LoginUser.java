package com.example.fablab.ui.authen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginUser extends AppCompatActivity implements View.OnClickListener{
    private TextView login;
    private EditText email,password;
    private Button loginbtn;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_login);

        login = (TextView) findViewById(R.id.register_swap);
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        loginbtn = (Button) findViewById(R.id.login_Button);
        progressBar = (ProgressBar) findViewById(R.id.progress_login);
        mAuth = FirebaseAuth.getInstance();

        loginbtn.setVisibility(View.VISIBLE);

        loginbtn.setOnClickListener((View.OnClickListener) this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.login_Button){
            userLogin();
        }else if(v.getId() == R.id.register_swap){
            startActivity(new Intent(this, RegisterUser.class));
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
                startActivity(new Intent(LoginUser.this, MainActivity.class));
                finish();
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
