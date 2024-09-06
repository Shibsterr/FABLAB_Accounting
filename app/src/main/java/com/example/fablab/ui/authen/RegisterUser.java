package com.example.fablab.ui.authen;

import android.app.DatePickerDialog;
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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterUser extends AppCompatActivity implements View.OnClickListener {

    private EditText name_surname, email, password, rep_password;
    private TextView date_of_birth;
    private Button register, login;
    private ImageButton datePickerButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_register);



        login = findViewById(R.id.register);
        login.setOnClickListener(this);

        register = findViewById(R.id.register_button);
        register.setOnClickListener(this);

        name_surname = findViewById(R.id.name_surname);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        rep_password = findViewById(R.id.password_repeat);
        date_of_birth = findViewById(R.id.date_of_birth);
        datePickerButton = findViewById(R.id.date_picker_button);
        progressBar = findViewById(R.id.progress_register);

        mAuth = FirebaseAuth.getInstance();

        datePickerButton.setOnClickListener(view -> showDatePickerDialog());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.register) {
            Intent intent = new Intent(RegisterUser.this, LoginUser.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (v.getId() == R.id.register_button) {
            userRegister();
        }
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                RegisterUser.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dateString = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    date_of_birth.setText(dateString);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void userRegister() {
        String names = name_surname.getText().toString();
        String mail = email.getText().toString();
        String pass = password.getText().toString();
        String reppass = rep_password.getText().toString();
        String dob = date_of_birth.getText().toString();
        String statuss = "Lietotājs";

        Map<String, Object> user = new HashMap<>();

        if (names.isEmpty()) {
            name_surname.setError("Vārds un uzvārds ir vajadzīgs!");
            name_surname.requestFocus();
            return;
        }

        if (mail.isEmpty()) {
            email.setError("E-pasts nav ievadīts!");
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            email.setError("Lūdzu ievadiet pareizu e-pastu!");
            email.requestFocus();
            return;
        }

        if (!isValidEmailDomain(mail)) {
            email.setError("E-pasta domēns nav atbalstīts! Izmantojiet piemēram, Gmail, Yahoo, Outlook utt.");
            email.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            date_of_birth.setError("Lūdzu izvēlieties dzimšanas datumu!");
            date_of_birth.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            password.setError("Nav ievadīta parole!");
            password.requestFocus();
            return;
        }

        if (!pass.equals(reppass)) {
            rep_password.setError("Paroles nesakrīt!");
            rep_password.requestFocus();
            return;
        }

        if (!isValidPassword(pass)) {
            password.setError("Parolei jābūt vismaz 8 rakstzīmēm, ar lielajiem burtiem, mazajiem burtiem, cipariem un speciālām rakstzīmēm.");
            password.requestFocus();
            return;
        }

        register.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(mail, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                DatabaseReference userRef = databaseRef.child("users").child(mAuth.getUid());

                user.put("Statuss", statuss);
                user.put("Vards un uzvards", names);
                user.put("epasts", mail);
                user.put("Dzimšanas datums", dob);  // Save the date of birth in the database

                userRef.setValue(user);
                Intent intent = new Intent(RegisterUser.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                Log.d("MainActivity", "Registering account...");
            } else {
                Toast.makeText(RegisterUser.this, "Notikusi kļūda!", Toast.LENGTH_LONG).show();
                register.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private boolean isValidEmailDomain(String email) {
        String[] validDomains = {"gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com"};
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        for (String validDomain : validDomains) {
            if (domain.equals(validDomain)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#\\$%^&*].*");
    }
}

