package com.example.fablab.ui.updateuser;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpdateUserFragment extends Fragment {

    private Spinner spinnerUsers, spinnerStatuss;
    private Button confirm, deleteUserButton;
    private DatabaseReference mDatabase;
    private Map<String, String> userIdMap;
    private FirebaseAuth mAuth;

    public UpdateUserFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_user, container, false);
        spinnerUsers = view.findViewById(R.id.spinnerUsers);
        spinnerStatuss = view.findViewById(R.id.spinneStatuss);
        confirm = view.findViewById(R.id.buttonConfirm);
        deleteUserButton = view.findViewById(R.id.buttonDelete);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        userIdMap = new HashMap<>();

        deleteUserButton.setEnabled(false); // Disable delete button initially

        initUserSpinner();
        initStatusSpinner();

        spinnerUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUser = parent.getItemAtPosition(position).toString();
                loadUserStatus(selectedUser);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        confirm.setOnClickListener(v -> UpdateUser());
        deleteUserButton.setOnClickListener(v -> confirmDeleteUser());

        return view;
    }

    private void initUserSpinner() {
        DatabaseReference usersRef = mDatabase.child("users");
        String currentUserId = mAuth.getCurrentUser().getUid();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> userList = new ArrayList<>();
                userIdMap.clear();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId.equals(currentUserId)) continue;

                    String fullName = userSnapshot.child("Vards un uzvards").getValue(String.class);
                    String notDeleted = userSnapshot.child("Statuss").getValue(String.class);

                    if (fullName != null && notDeleted != null && !notDeleted.equals("deleted")) {
                        userList.add(fullName);
                        userIdMap.put(fullName, userId);
                    }
                }

                if (getContext() != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUsers.setAdapter(adapter);
                    deleteUserButton.setEnabled(!userList.isEmpty()); // Enable delete button if users exist
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), getString(R.string.failed_error_loadUser) + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initStatusSpinner() {
        List<String> statuses = new ArrayList<>();
        statuses.add("Darbinieks");
        statuses.add("Lietotājs");
        statuses.add("Admin");

        if (getContext() != null) {
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statuses);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStatuss.setAdapter(statusAdapter);
        }
    }

    private void loadUserStatus(String userName) {
        String userId = userIdMap.get(userName);
        if (userId == null) return;

        DatabaseReference userRef = mDatabase.child("users").child(userId).child("Statuss");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String userStatus = dataSnapshot.getValue(String.class);
                if (userStatus != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStatuss.getAdapter();
                    int position = adapter.getPosition(userStatus);
                    if (position != -1) {
                        spinnerStatuss.setSelection(position);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), getString(R.string.error_user_status), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteUser() {
        if (spinnerUsers.getSelectedItem() == null) {
            Toast.makeText(getContext(), getString(R.string.invalid_user_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedUser = spinnerUsers.getSelectedItem().toString();
        String userId = userIdMap.get(selectedUser);

        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.invalid_user_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("UPDATE USER", "Selected user ID: " + userId);

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.deletetitle))
                .setMessage(getString(R.string.deletetext1) + " " + selectedUser + " " + getString(R.string.deletetext2))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteUser(userId))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteUser(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("Statuss", "deleted");

        mDatabase.child("users").child(userId).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.userdeleted), Toast.LENGTH_SHORT).show();
                initUserSpinner(); // Refresh the user list
                addLogEntry(spinnerUsers.getSelectedItem().toString(), "deleted", true);

            } else {
                Toast.makeText(getContext(), getString(R.string.userstatuserror), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void UpdateUser() {
        if (spinnerUsers.getSelectedItem() == null || spinnerStatuss.getSelectedItem() == null) {
            Toast.makeText(getContext(), getString(R.string.invalid_user_id), Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedUser = spinnerUsers.getSelectedItem().toString();
        String newStatus = spinnerStatuss.getSelectedItem().toString();
        String userId = userIdMap.get(selectedUser);

        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.invalid_user_id), Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = mDatabase.child("users").child(userId);
        userRef.child("Statuss").setValue(newStatus).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.toastUpdateMsg), Toast.LENGTH_SHORT).show();
                initUserSpinner(); // Refresh the user list
                addLogEntry(selectedUser, newStatus, false);

            } else {
                Toast.makeText(getContext(), getString(R.string.toastUpdateMsgError), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addLogEntry(String targetUserName, String newStatus, boolean isDeletion) {
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

                String title = isDeletion
                        ? "Lietotāja dzēšana " + dateTime
                        : "Lietotāja statusa maiņa " + dateTime;

                String summary = isDeletion
                        ? fullName + " izdzēsa lietotāju: " + targetUserName + "."
                        : fullName + " mainīja lietotāja " + targetUserName + " statusu uz: " + newStatus + ".";

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
