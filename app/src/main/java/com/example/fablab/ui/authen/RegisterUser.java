package com.example.fablab.ui.authen;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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

        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);
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

        // Apply the ASCII input filter to the name_surname and email fields
        name_surname.setFilters(new InputFilter[]{new NameInputFilter()});
        email.setFilters(new InputFilter[]{new EmailInputFilter()});


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
                    if (isValidDate(selectedYear, selectedMonth, selectedDay)) {
                        String dateString = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        date_of_birth.setText(dateString);
                    } else {
                        Toast.makeText(RegisterUser.this, "Please select a valid date of birth.", Toast.LENGTH_SHORT).show();
                    }
                },
                year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private boolean isValidDate(int year, int month, int day) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);

        Calendar currentDate = Calendar.getInstance();

        return !selectedDate.after(currentDate);
    }

    private void userRegister() {
        String names = name_surname.getText().toString().trim();
        String mail = email.getText().toString().trim();
        String pass = password.getText().toString();
        String reppass = rep_password.getText().toString();
        String dob = date_of_birth.getText().toString();
        String statuss = "Lietotājs";

        if (names.isEmpty()) {
            name_surname.setError(getString(R.string.error_name_required));
            name_surname.requestFocus();
            return;
        }

        if (mail.isEmpty()) {
            email.setError(getString(R.string.error_email_required));
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            email.setError(getString(R.string.error_invalid_email));
            email.requestFocus();
            return;
        }

        if (!isValidEmailDomain(mail)) {
            email.setError(getString(R.string.error_invalid_email_domain));
            email.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            date_of_birth.setError(getString(R.string.error_dob_required));
            date_of_birth.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            password.setError(getString(R.string.error_password_required));
            password.requestFocus();
            return;
        }

        if (!pass.equals(reppass)) {
            rep_password.setError(getString(R.string.error_password_mismatch));
            rep_password.requestFocus();
            return;
        }

        if (!isValidPassword(pass)) {
            password.setError(getString(R.string.error_invalid_password));
            password.requestFocus();
            return;
        }

        if (!isValidName(names)) {
            name_surname.setError(getString(R.string.validname_error));
            name_surname.requestFocus();
            return;
        }


        register.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.fetchSignInMethodsForEmail(mail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean emailExists = task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty();
                if (emailExists) {
                    email.setError(getString(R.string.error_email_exists));
                    email.requestFocus();
                    register.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                } else {
                    createNewUser(mail, pass, names, dob, statuss);
                }
            } else {
                Toast.makeText(RegisterUser.this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                register.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void createNewUser(String email, String password, String name, String dob, String status) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                DatabaseReference userRef = databaseRef.child("users").child(mAuth.getUid());

                Map<String, Object> user = new HashMap<>();
                user.put("Statuss", status);
                user.put("Vards un uzvards", name);
                user.put("epasts", email);
                user.put("Dzimšanas datums", dob);

                userRef.setValue(user).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        Intent intent = new Intent(RegisterUser.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterUser.this, getString(R.string.error_database_error), Toast.LENGTH_LONG).show();
                        register.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            } else {
                String errorMsg = getString(R.string.error_generic);
                try {
                    throw task.getException();
                } catch (Exception e) {
                    errorMsg = getString(R.string.error_generic);
                }
                Toast.makeText(RegisterUser.this, errorMsg, Toast.LENGTH_LONG).show();
                register.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private boolean isValidEmailDomain(String email) {
        String[] validDomains = {"gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com", "liepaja.edu.lv"};
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        for (String validDomain : validDomains) {
            if (domain.equals(validDomain)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#\\$%^&*])[A-Za-z\\d!@#\\$%^&*]{8,}$";
        return password.matches(regex);
    }

    private boolean isValidName(String name) {
        String regex = "^[A-Za-zĀČĒĢĪĶĻŅŠŪŽāčēģīķļņšūž ]+$";
        return name.matches(regex);
    }

    //*********************************************************************
    // input filter to restrict special characters
    private static class NameInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null && !source.toString().matches("^[A-Za-zĀČĒĢĪĶĻŅŠŪŽāčēģīķļņšūž ]*$")) {
                return "";
            }
            return null;
        }
    }

    private static class EmailInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null && !source.toString().matches("^[A-Za-z0-9@._-]*$")) {
                return "";
            }
            return null;
        }
    }

}
