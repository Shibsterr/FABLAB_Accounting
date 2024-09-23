package com.example.fablab;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
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

    // Email credentials (ensure to store them securely)
    private static final String EMAIL = "fablabappnoreply@gmail.com"; // Change this to your email
    private static final String PASSWORD = "xllk wqet dulg xabp"; // Change this to your password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Language setup
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_event);

        // Initialize views
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
            Toast.makeText(NewEvent.this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Reference to the "events" node in Firebase database
        eventsRef = FirebaseDatabase.getInstance().getReference("events");

        // Date Picker
        dateText.setOnClickListener(v -> showDatePickerDialog());

        // Time Picker for Start Time
        startTimeText.setOnClickListener(v -> showTimePickerDialog(true));

        // Time Picker for End Time
        endTimeText.setOnClickListener(v -> showTimePickerDialog(false));

        // Submit Button
        submitButton.setOnClickListener(v -> {
            String title = titleText.getText().toString().trim();
            String description = descriptionText.getText().toString().trim();
            String numberOfPeople = numberOfPeopleText.getText().toString().trim();

            // Validation
            if (title.isEmpty() || description.isEmpty() || eventDate.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || numberOfPeople.isEmpty()) {
                Toast.makeText(NewEvent.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create event object with a unique ID
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

            // Save event under events/date/userId/eventId
            eventsRef.child(eventDate).child(currentUser.getUid()).child(eventId).setValue(eventMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(NewEvent.this, "Event added successfully", Toast.LENGTH_SHORT).show();
                        sendEmailToAdmins(title, description, numberOfPeople, eventDate, startTime, endTime);
                        finish(); // Close activity after successful submission
                    })
                    .addOnFailureListener(e -> Toast.makeText(NewEvent.this, "Failed to add event", Toast.LENGTH_SHORT).show());
        });
    }

    // Show DatePickerDialog for picking the date
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Set the minimum date to today
        Calendar minDate = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, (view, selectedYear, selectedMonth, selectedDay) -> {
            eventDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
            dateText.setText(eventDate);
        }, year, month, day);

        // Ensure the user cannot select a past date
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    // Show TimePickerDialog for picking start or end time
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
                    Toast.makeText(NewEvent.this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                    return;
                }
                endTime = time;
                endTimeText.setText(getString(R.string.end_time, time));
            }
        }, hour, minute, true);
        timePickerDialog.show();
    }

    // Check if the end time is before the start time
    private boolean isEndTimeBeforeStartTime(String endTime) {
        if (startTime.isEmpty()) return false;

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        // Parse the start and end times
        String[] startParts = startTime.split(":");
        String[] endParts = endTime.split(":");

        startCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startParts[0]));
        startCalendar.set(Calendar.MINUTE, Integer.parseInt(startParts[1]));

        endCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endParts[0]));
        endCalendar.set(Calendar.MINUTE, Integer.parseInt(endParts[1]));

        return endCalendar.before(startCalendar);
    }

    // Method to send email to admins
    private void sendEmailToAdmins(String title, String description, String numberOfPeople, String eventDate, String startTime, String endTime) {
        // Create a string with the email body
        String emailBody = "Ir izveidots jauns pasākums:\n" +
                "Stacija: " + title +
                "\nApraksts: " + description +
                "\nCilvēka skaits: " + numberOfPeople +
                "\nDatums: " + eventDate +
                "\nSākuma laiks: " + startTime +
                "\nBeigu laiks: " + endTime;

        // Fetch admin and worker emails and send the email
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> emails = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("Statuss").getValue(String.class);
                    String email = userSnapshot.child("epasts").getValue(String.class);

                    // Add email if user is an admin or worker
                    if (email != null && ("Admin".equalsIgnoreCase(status) || "Darbinieks".equalsIgnoreCase(status))) {
                        emails.add(email);
                    }
                }

                // Send email if there are recipients
                if (!emails.isEmpty()) {
                    EmailSender emailSender = new EmailSender(EMAIL, PASSWORD);
                    emailSender.sendEmail(emails, "Izveidots jauns pasākums", emailBody);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }
}
