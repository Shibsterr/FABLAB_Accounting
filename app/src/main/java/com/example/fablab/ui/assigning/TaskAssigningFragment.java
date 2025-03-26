package com.example.fablab.ui.assigning;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAssigningFragment extends Fragment {

    private Spinner spinnerUsers;
    private DatePicker datePicker;
    private RadioGroup radioGroupUrgency;
    private EditText editTextDescription;
    private Button buttonAssignTask;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_assigning, container, false);

        spinnerUsers = view.findViewById(R.id.spinnerUsers);
        datePicker = view.findViewById(R.id.datePicker);
        radioGroupUrgency = view.findViewById(R.id.radioGroupUrgency);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        buttonAssignTask = view.findViewById(R.id.buttonAssignTask);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Set the minimum date for the DatePicker to today's date
        Calendar calendar = Calendar.getInstance();
        datePicker.setMinDate(calendar.getTimeInMillis());

        // Initialize spinner with users
        initSpinner();

        buttonAssignTask.setOnClickListener(v -> assignTask());

        return view;
    }

    private void initSpinner() {
        // Get a reference to the users node in the database
        DatabaseReference usersRef = mDatabase.child("users");

        // Read data from the users node
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> userList = new ArrayList<>();

                // Get the status of the current user
                String currentUserStatus = dataSnapshot.child(mAuth.getCurrentUser().getUid()).child("Statuss").getValue(String.class);

                // Iterate through the users
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("Statuss").getValue(String.class);

                    if ("Admin".equals(currentUserStatus)) {
                        if ("Admin".equals(status) || "Darbinieks".equals(status)) {
                            String fullName = userSnapshot.child("Vards un uzvards").getValue(String.class);
                            if (fullName != null) {
                                userList.add(fullName);
                            }
                        }
                    } else if ("Darbinieks".equals(currentUserStatus)) {
                        if ("Darbinieks".equals(status)) {
                            String fullName = userSnapshot.child("Vards un uzvards").getValue(String.class);
                            if (fullName != null) {
                                userList.add(fullName);
                            }
                        }
                    }
                }

                // Populate the spinner with the user list
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerUsers.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), getString(R.string.failed_error_loadUser) + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignTask() {
        String selectedUser = spinnerUsers.getSelectedItem().toString();
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1; // Months are 0-based, so add 1
        int year = datePicker.getYear();
        String deadline = day + "/" + month + "/" + year;

        RadioButton radioButton = getView().findViewById(radioGroupUrgency.getCheckedRadioButtonId());
        String urgency = radioButton.getText().toString();

        String description = editTextDescription.getText().toString();

        // Check if the description is empty
        if (description.trim().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_desc_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user's UID
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Get current user's username from the Realtime Database
        DatabaseReference currentUserRef = mDatabase.child("users").child(currentUserId);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String currentUserName = dataSnapshot.child("Vards un uzvards").getValue(String.class);

                    // Get current date and time
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    // Assign task to user in the database
                    DatabaseReference userTasksRef = mDatabase.child("tasks").child(selectedUser);

                    // Generate unique task ID using date and time
                    String taskId = dateTime;
                    DatabaseReference taskRef = userTasksRef.child(taskId);

                    taskRef.child("deadline").setValue(deadline);
                    taskRef.child("urgency").setValue(urgency);
                    taskRef.child("description").setValue(description);
                    taskRef.child("status").setValue("incomplete"); // Initial status is incomplete
                    taskRef.child("assignedBy").setValue(currentUserName); // Include the name of the user who assigned the task
                    Toast.makeText(getContext(), getString(R.string.task_assign_good), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.failed_retrieve_info), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), getString(R.string.failed_retrieve_info), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
