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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> userList = new ArrayList<>();
                userIdMap.clear();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String fullName = userSnapshot.child("Vards un uzvards").getValue(String.class);
                    if (fullName != null) {
                        userList.add(fullName);
                        userIdMap.put(fullName, userId);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerUsers.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initStatusSpinner() {
        List<String> statuses = new ArrayList<>();
        statuses.add("Darbinieks");
        statuses.add("Lietotājs");
        statuses.add("Admin");

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatuss.setAdapter(statusAdapter);
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
                Toast.makeText(getContext(), "Failed to load user status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteUser() {
        String selectedUser = spinnerUsers.getSelectedItem().toString();
        String userId = userIdMap.get(selectedUser);
        Log.d("UPDATE USER", userId);
        if (userId == null) {
            Toast.makeText(getContext(), "Invalid user selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + selectedUser + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(String userId) {
        // Delete user from Firebase Realtime Database
        mDatabase.child("users").child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                deleteFromAuth(userId);
            } else {
                Toast.makeText(getContext(), "Failed to delete user from database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFromAuth(String userId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid().equals(userId)) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "User deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(getContext(), "Re-authentication required to delete user", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete user from authentication", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Cannot delete: Authentication issue", Toast.LENGTH_SHORT).show();
        }
    }

    private void UpdateUser() {
        // Your existing update user method
    }
}
