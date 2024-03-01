package com.example.fablab.ui.authen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUser extends AppCompatActivity implements View.OnClickListener{

    private EditText name_surname,email,password,rep_password;
    private Button register,login;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_register);

        login = (Button) findViewById(R.id.register);
        login.setOnClickListener(this);

        register = (Button) findViewById(R.id.register_button);
        register.setOnClickListener(this);

        name_surname = (EditText) findViewById(R.id.name_surname);
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        rep_password = (EditText) findViewById(R.id.password_repeat);
        progressBar = (ProgressBar) findViewById(R.id.progress_register);

        register.setVisibility(View.VISIBLE);

        mAuth = FirebaseAuth.getInstance();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.register) {
            Intent intent = new Intent(RegisterUser.this, LoginUser.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }else if(v.getId() == R.id.register_button){
            userRegister();
        }
    }

    private void userRegister() {
        String names = name_surname.getText().toString();
        String mail = email.getText().toString();
        String pass = password.getText().toString();
        String reppass = rep_password.getText().toString();
        String statuss = "Lietotājs";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();

         if(names.isEmpty() && mail.isEmpty() && pass.isEmpty() && reppass.isEmpty()){
             name_surname.setError("Vārds un uzvārds ir vajadzīgs!");
             name_surname.requestFocus();

             if(!Patterns.EMAIL_ADDRESS.matcher(mail).matches()){
                 email.setError("Lūdzu ievadiet pareizu e-pastu!");
                 email.requestFocus();
             }

             email.setError("E-pasts nav ievadīts!");
             email.requestFocus();

             password.setError("Nav ievadīta parole!");
             password.requestFocus();

         }else{
             register.setVisibility(View.GONE);
             progressBar.setVisibility(View.VISIBLE);

             mAuth.createUserWithEmailAndPassword(mail,pass).addOnCompleteListener(task -> {
                 if(task.isSuccessful()){
                     // Get a reference to your Firebase database
                     DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

                    // Assuming 'db' is an instance of DatabaseReference pointing to your database root
                     DatabaseReference userRef = databaseRef.child("users").child(mAuth.getUid());

                     user.put("Statuss", statuss);
                     user.put("Vards un uzvards", names);
                     user.put("epasts", mail);

                     userRef.setValue(user);
                     Intent intent = new Intent(RegisterUser.this, MainActivity.class);
                     intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                     startActivity(intent);
                     finish();
                     Log.d("MainActivity","Registering account...");
                 }else{
                     Toast.makeText(RegisterUser.this, "Notikusi kļūda!", Toast.LENGTH_LONG).show();
                 }
             });
         }
    }
}
