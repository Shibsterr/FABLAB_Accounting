package com.example.fablab.ui.gallery;

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

import com.example.fablab.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GalleryFragment extends Fragment {

    private LinearLayout tasksLayout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        tasksLayout = view.findViewById(R.id.tasksLayout);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadTasks();

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
                                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                                    String description = taskSnapshot.child("description").getValue(String.class);
                                    String deadline = taskSnapshot.child("deadline").getValue(String.class);
                                    String status = taskSnapshot.child("status").getValue(String.class);
                                    String assignedBy = taskSnapshot.child("assignedBy").getValue(String.class);
                                    String taskKey = taskSnapshot.getKey();
                                    if (description != null && deadline != null && status != null && assignedBy != null) {
                                        if (!status.equals("complete")) {
                                            addTaskToLayout(description, deadline, status, assignedBy, taskKey, currentUsername);
                                        }
                                    }
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

    private void addTaskToLayout(String description, String deadline, String status, String assignedBy, String taskKey, String currentUsername) {
        View taskView = getLayoutInflater().inflate(R.layout.item_tasks, tasksLayout, false);
        TextView textViewDescription = taskView.findViewById(R.id.textViewTaskDescription);
        TextView textViewDeadline = taskView.findViewById(R.id.textViewTaskDeadline);
        TextView textViewStatus = taskView.findViewById(R.id.textViewTaskStatus);
        TextView textViewAssignedBy = taskView.findViewById(R.id.textViewAssignedBy);
        Button buttonComplete = taskView.findViewById(R.id.buttonComplete); // Add Complete button

        textViewDescription.setText(description);
        textViewDeadline.setText("Deadline: " + deadline);
        textViewStatus.setText("Status: " + status);
        textViewAssignedBy.setText("Assigned by: " + assignedBy);

        // Set click listener for Complete button
        buttonComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update task status to "complete" in the database
                DatabaseReference taskRef = mDatabase.child("tasks").child(currentUsername).child(taskKey).child("status");
                taskRef.setValue("complete").addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Remove the task from the layout
                            tasksLayout.removeView(taskView);

                        } else {
                            // Handle the failure to update the task status
                            Toast.makeText(getContext(), "Failed to mark task as complete", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out);
                taskView.startAnimation(animation);
            }
        });

        tasksLayout.addView(taskView);
    }
}
