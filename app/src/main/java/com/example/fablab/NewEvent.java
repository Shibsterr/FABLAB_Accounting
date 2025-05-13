package com.example.fablab;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class NewEvent extends AppCompatActivity {
    private EditText titleText, descriptionText, numberOfPeopleText;
    private TextView dateText, startTimeText, endTimeText;
    private Button submitButton;
    private FirebaseAuth mAuth;
    private DatabaseReference eventsRef;

    private String eventDate = "";
    private String startTime = "";
    private String endTime = "";

    private static final String EMAIL = "fablabappnoreply@gmail.com";
    private static final String PASSWORD = "xllk wqet dulg xabp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Pirms satura skata iestatīšanas lietojiet motīvu un valodu
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

        // Iestatiet lokalizāciju, pamatojoties uz izvēlēto valodu
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        // Atjauniniet konfigurāciju un displeja metriku
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_event);

        // Inicializēt skatus
        titleText = findViewById(R.id.nodarbiba_text);
        descriptionText = findViewById(R.id.stacija_text);
        dateText = findViewById(R.id.date_text);
        startTimeText = findViewById(R.id.start_time_text);
        endTimeText = findViewById(R.id.end_time_text);
        numberOfPeopleText = findViewById(R.id.number_of_people);
        submitButton = findViewById(R.id.pieteikties);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(NewEvent.this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Atsauce uz Firebase datu bāzes mezglu "notikumi".
        eventsRef = FirebaseDatabase.getInstance().getReference("events");

        // Datuma atlasītājs
        dateText.setOnClickListener(v -> showDatePickerDialog());

        // Laika atlasītājs sākuma laikam
        startTimeText.setOnClickListener(v -> showTimePickerDialog(true));

        // Laika atlasītājs beigu laikam
        endTimeText.setOnClickListener(v -> showTimePickerDialog(false));

        // Iesniegšanas poga
        submitButton.setOnClickListener(v -> {
            String title = titleText.getText().toString().trim();
            String description = descriptionText.getText().toString().trim();
            String numberOfPeople = numberOfPeopleText.getText().toString().trim();

            // Validācija
            if (title.isEmpty() || description.isEmpty() || eventDate.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || numberOfPeople.isEmpty()) {
                Toast.makeText(NewEvent.this, getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show();
                return;
            }

            // Izveidojiet notikuma objektu ar unikālu ID
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("eventId", eventId);
            eventMap.put("title", title);
            eventMap.put("description", description);
            eventMap.put("eventDate", eventDate);
            eventMap.put("startTime", startTime);
            eventMap.put("endTime", endTime);
            eventMap.put("numberOfPeople", numberOfPeople);
            eventMap.put("status", "Pending");
            eventMap.put("userId", currentUser.getUid());

            // Saglabājiet notikumu sadaļā Events/date/userId/eventId
            eventsRef.child(eventDate).child(currentUser.getUid()).child(eventId).setValue(eventMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(NewEvent.this, getString(R.string.eventadded_good), Toast.LENGTH_SHORT).show();
                        sendEmailToAdmins(title, description, numberOfPeople, eventDate, startTime, endTime);
                        addLogEntry(title, eventDate);
                        finish(); // Aizvērt darbību pēc veiksmīgas iesniegšanas
                    })
                    .addOnFailureListener(e -> Toast.makeText(NewEvent.this, getString(R.string.eventadded_Bad), Toast.LENGTH_SHORT).show());
        });
    }

    // Rādīt DatePicker Dialog datuma izvēlei
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Iestatiet minimālo datumu uz šodienu
        Calendar minDate = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, (view, selectedYear, selectedMonth, selectedDay) -> {
            eventDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
            dateText.setText(eventDate);
        }, year, month, day);

        // Pārliecinieties, ka lietotājs nevar atlasīt pagātnes datumu
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    // Rādīt TimePickerDialog, lai izvēlētos sākuma vai beigu laiku
    private void showTimePickerDialog(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this, (view, selectedHour, selectedMinute) -> {
            String time = String.format("%02d:%02d", selectedHour, selectedMinute);
            if (isStartTime) {
                startTime = time;
                startTimeText.setText(getString(R.string.start_time, time));
            } else {
                // Check if end time is before start time
                if (isEndTimeBeforeStartTime(time)) {
                    Toast.makeText(NewEvent.this, getString(R.string.endtime_starttime_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                endTime = time;
                endTimeText.setText(getString(R.string.end_time, time));
            }
        }, hour, minute, true);
        timePickerDialog.show();
    }

    // Pārbaudiet, vai beigu laiks ir pirms sākuma laika
    private boolean isEndTimeBeforeStartTime(String endTime) {
        if (startTime.isEmpty()) return false;

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        // Parsējiet sākuma un beigu laiku
        String[] startParts = startTime.split(":");
        String[] endParts = endTime.split(":");

        startCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startParts[0]));
        startCalendar.set(Calendar.MINUTE, Integer.parseInt(startParts[1]));

        endCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endParts[0]));
        endCalendar.set(Calendar.MINUTE, Integer.parseInt(endParts[1]));

        return endCalendar.before(startCalendar);
    }

    // Sūta e-pastu visiem administratoriem un darbiniekiem
    private void sendEmailToAdmins(String title, String description, String numberOfPeople, String eventDate, String startTime, String endTime) {
        // Izveidojiet virkni ar e-pasta pamattekstu
        String emailBody = "Ir izveidots jauns pasākums:\n" +
                "Stacija: " + title +
                "\nApraksts: " + description +
                "\nCilvēka skaits: " + numberOfPeople +
                "\nDatums: " + eventDate +
                "\nSākuma laiks: " + startTime +
                "\nBeigu laiks: " + endTime;

        // Iegūstiet administratora un darbinieku e-pasta ziņojumus un nosūtiet e-pastu
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> emails = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("Statuss").getValue(String.class);
                    String email = userSnapshot.child("epasts").getValue(String.class);

                    // Pievienojiet e-pasta adresi, ja lietotājs ir administrators vai darbinieks
                    if (email != null && ("Admin".equalsIgnoreCase(status) || "Darbinieks".equalsIgnoreCase(status))) {
                        emails.add(email);
                    }
                }

                // Nosūtiet e-pastu, ja ir adresāti
                if (!emails.isEmpty()) {
                    EmailSender emailSender = new EmailSender(EMAIL, PASSWORD);
                    emailSender.sendEmail(emails, "Izveidots jauns pasākums", emailBody);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    // Pievieno ierakstu "Logs" sadaļā
    private void addLogEntry(String name, String time) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.e("AddLogEntry", "User data not found.");
                    return;
                }

                String fullName = dataSnapshot.child("Vards un uzvards").getValue(String.class);

                // Format current date and time
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                String dateTime = sdf.format(new Date());

                String title = "Izveidots jauns notikums " + dateTime;

                String summary = fullName + " izveidoja jaunu notikumu '" + name + "' kurš notiek '" + time + "' datumā.";

                DatabaseReference logRef = FirebaseDatabase.getInstance().getReference()
                        .child("Logs").child(dateTime);

                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("user", fullName);
                logEntry.put("email", email);
                logEntry.put("title", title);
                logEntry.put("summary", summary);

                logRef.setValue(logEntry)
                        .addOnSuccessListener(aVoid -> Log.d("AddLogEntry", "Log entry added successfully"))
                        .addOnFailureListener(e -> Log.e("AddLogEntry", "Error adding log entry", e));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AddLogEntry", "Database error: " + databaseError.getMessage());
            }
        });
    }

}
