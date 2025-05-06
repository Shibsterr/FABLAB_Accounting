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

        // Inicializē UI komponentes
        spinnerUsers = view.findViewById(R.id.spinnerUsers);
        datePicker = view.findViewById(R.id.datePicker);
        radioGroupUrgency = view.findViewById(R.id.radioGroupUrgency);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        buttonAssignTask = view.findViewById(R.id.buttonAssignTask);

        // Firebase autentifikācija un datu bāzes references
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Uzstāda šodienas datumu kā minimālo DatePicker vērtību
        Calendar calendar = Calendar.getInstance();
        datePicker.setMinDate(calendar.getTimeInMillis());

        // Ielādē lietotāju sarakstu spinera izvēlnē
        initSpinner();

        // Uzstāda pogas nospiešanas listeneri
        buttonAssignTask.setOnClickListener(v -> assignTask());

        return view;
    }

    // Inicializē lietotāju spineri
    private void initSpinner() {
        DatabaseReference usersRef = mDatabase.child("users");

        // Nolasa visus lietotājus no datu bāzes vienreiz
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> userList = new ArrayList<>();

                // Iegūst pašreizējā lietotāja statusu
                String currentUserStatus = dataSnapshot.child(mAuth.getCurrentUser().getUid()).child("Statuss").getValue(String.class);

                // Iterē caur visiem lietotājiem
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("Statuss").getValue(String.class);

                    // Atkarībā no pašreizējā lietotāja statusa pievieno attiecīgos lietotājus
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

                // Uzstāda spinerim adapteri ar lietotāju sarakstu
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerUsers.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Kļūda lietotāju ielādē
                Toast.makeText(getContext(), getString(R.string.failed_error_loadUser) + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Metode uzdevuma piešķiršanai
    private void assignTask() {
        String selectedUser = spinnerUsers.getSelectedItem().toString();

        // Saņem izvēlēto datumu
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1;
        int year = datePicker.getYear();
        String deadline = day + "/" + month + "/" + year;

        // Saņem izvēlēto steidzamības līmeni
        RadioButton radioButton = getView().findViewById(radioGroupUrgency.getCheckedRadioButtonId());
        String urgency = radioButton.getText().toString();

        // Iegūst aprakstu un pārbauda, vai tas nav tukšs
        String description = editTextDescription.getText().toString();
        if (description.trim().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_desc_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        // Iegūst pašreizējā lietotāja UID
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Nolasa pašreizējā lietotāja vārdu no datu bāzes
        DatabaseReference currentUserRef = mDatabase.child("users").child(currentUserId);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Iegūst vārdu un uzvārdu
                    String currentUserName = dataSnapshot.child("Vards un uzvards").getValue(String.class);

                    // Ģenerē datuma un laika zīmogu kā uzdevuma ID
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    // Saglabā uzdevumu izvēlētajam lietotājam datu bāzē
                    DatabaseReference userTasksRef = mDatabase.child("tasks").child(selectedUser);
                    String taskId = dateTime; // Izmanto datuma/laika zīmogu kā unikālu ID
                    DatabaseReference taskRef = userTasksRef.child(taskId);

                    // Saglabā uzdevuma detaļas
                    taskRef.child("deadline").setValue(deadline);
                    taskRef.child("urgency").setValue(urgency);
                    taskRef.child("description").setValue(description);
                    taskRef.child("status").setValue("incomplete"); // Sākotnējais statuss
                    taskRef.child("assignedBy").setValue(currentUserName); // Kas piešķīra

                    Toast.makeText(getContext(), getString(R.string.task_assign_good), Toast.LENGTH_SHORT).show();
                } else {
                    // Kļūda lietotāja datu nolasē
                    Toast.makeText(getContext(), getString(R.string.failed_retrieve_info), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Kļūda datu nolasē
                Toast.makeText(getContext(), getString(R.string.failed_retrieve_info), Toast.LENGTH_SHORT).show();
            }
        });
    }
}