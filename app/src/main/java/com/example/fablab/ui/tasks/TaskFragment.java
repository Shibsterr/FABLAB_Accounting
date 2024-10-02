package com.example.fablab.ui.tasks;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class TaskFragment extends Fragment {

    private LinearLayout tasksLayout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Button refreshButton; // Add a reference for the refresh button

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        // Update the configuration and display metrics
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        tasksLayout = view.findViewById(R.id.tasksLayout);
        refreshButton = view.findViewById(R.id.refreshButton); // Initialize the refresh button
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadTasks();

        // Set OnClickListener for the refresh button
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTasks();
            }
        });

        return view;
    }

    private void loadTasks() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            DatabaseReference currentUserRef = mDatabase.child("users").child(currentUserId);
            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String currentUsername = dataSnapshot.child("Vards un uzvards").getValue(String.class);
                    if (currentUsername != null) {
                        DatabaseReference userTasksRef = mDatabase.child("tasks").child(currentUsername);
                        userTasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                tasksLayout.removeAllViews();
                                boolean hasTasks = false;

                                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                                    String description = taskSnapshot.child("description").getValue(String.class);
                                    String deadline = taskSnapshot.child("deadline").getValue(String.class);
                                    String status = taskSnapshot.child("status").getValue(String.class);
                                    String assignedBy = taskSnapshot.child("assignedBy").getValue(String.class);
                                    String urgent = taskSnapshot.child("urgency").getValue(String.class);
                                    String taskKey = taskSnapshot.getKey();
                                    if (description != null && deadline != null && status != null && assignedBy != null) {
                                        if (!status.equals("complete")) {
                                            hasTasks = true;
                                            addTaskToLayout(description, deadline, status, assignedBy, taskKey, currentUsername, urgent);
                                        }
                                    }
                                }

                                // If no tasks are found, show the refresh button
                                if (!hasTasks) {
                                    refreshButton.setVisibility(View.VISIBLE);
                                } else {
                                    refreshButton.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle errors
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle errors
                }
            });
        }
    }
    private void addTaskToLayout(String description, String deadline, String status, String assignedBy, String taskKey, String currentUsername, String urgent) {
        View taskView = getLayoutInflater().inflate(R.layout.item_tasks, tasksLayout, false);
        TextView textViewDescription = taskView.findViewById(R.id.textViewTaskDescription);
        TextView textViewDeadline = taskView.findViewById(R.id.textViewTaskDeadline);
        TextView textViewStatus = taskView.findViewById(R.id.textViewTaskStatus);
        TextView textViewAssignedBy = taskView.findViewById(R.id.textViewAssignedBy);
        TextView textViewUrgency = taskView.findViewById(R.id.textViewImportant);
        Button buttonComplete = taskView.findViewById(R.id.buttonComplete);

        // Set task details
        textViewDescription.setText(description);
        textViewDeadline.setText(getString(R.string.deadline) + deadline);
        textViewStatus.setText(getString(R.string.status_item) + status);
        textViewAssignedBy.setText(getString(R.string.assigned_by_items) + assignedBy);

        // Display translated urgency
        if (urgent != null) {
            if (urgent.equals("Steidzami")) {
                textViewUrgency.setText(getString(R.string.important) + getString(R.string.urgent));
            } else {
                textViewUrgency.setText(getString(R.string.important) + getString(R.string.not_urgent));
            }
        } else {
            textViewUrgency.setText(getString(R.string.important) + getString(R.string.not_urgent)); // default if not provided
        }

        // Set click listener for Complete button
        buttonComplete.setOnClickListener(v -> {
            DatabaseReference taskRef = mDatabase.child("tasks").child(currentUsername).child(taskKey).child("status");
            taskRef.setValue("complete").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    tasksLayout.removeView(taskView);
                } else {
                    Toast.makeText(getContext(), "Failed to mark task as complete", Toast.LENGTH_SHORT).show();
                }
            });
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out);
            taskView.startAnimation(animation);
        });

        tasksLayout.addView(taskView);
    }


}
