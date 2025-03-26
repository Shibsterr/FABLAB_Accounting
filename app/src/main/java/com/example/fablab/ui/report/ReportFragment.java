package com.example.fablab.ui.report;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.fablab.EmailSender;
import com.example.fablab.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReportFragment extends Fragment {

    private EditText descriptionEditText, edittelpanr;
    private Spinner stacijaSpinner, equipmentSpinner;
    private EmailSender emailSender;
    private List<String> stationNodeNames = new ArrayList<>();  // To store station node names

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slideshow, container, false);

        descriptionEditText = view.findViewById(R.id.Description);
        edittelpanr = view.findViewById(R.id.telpanr);
        stacijaSpinner = view.findViewById(R.id.stacijasnr_spinner);
        equipmentSpinner = view.findViewById(R.id.equipment_spinner);

        Button sendButton = view.findViewById(R.id.send_it);
        sendButton.setOnClickListener(v -> fetchAdminAndWorkerEmails());

        loadStationNames();

        // Initialize EmailSender with your email and password
        emailSender = new EmailSender("fablabappnoreply@gmail.com", "xllk wqet dulg xabp");

        // Set listener for station selection to load equipment list
        stacijaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStationNode = stationNodeNames.get(position);  // Get the node name for the selected station
                loadEquipmentForStation(selectedStationNode);  // Pass the node name
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        return view;
    }

    private String getCurrentLanguage() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPref.getString("language_preference", "en"); // Default is "en"
    }

    private void loadStationNames() {
        DatabaseReference stationsRef = FirebaseDatabase.getInstance().getReference().child("stations");
        stationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> stationNames = new ArrayList<>();
                stationNodeNames.clear();  // Clear previous station nodes

                for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                    String stationName = stationSnapshot.child("Name").child(getCurrentLanguage()).getValue(String.class);
                    String stationNodeName = stationSnapshot.getKey();  // Get the node name (e.g., "Heatpress_station")

                    if (stationName != null && stationNodeName != null) {
                        stationNames.add(stationName);  // Add station name to display
                        stationNodeNames.add(stationNodeName);  // Store node name for later use
                    }
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, stationNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                stacijaSpinner.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    // Method to load equipment for the selected station node
    private void loadEquipmentForStation(String stationNodeName) {
        DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("stations").child(stationNodeName).child("Equipment");
        equipmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> equipmentList = new ArrayList<>();
                for (DataSnapshot equipmentSnapshot : dataSnapshot.getChildren()) {
                    String equipmentName = equipmentSnapshot.child("Nosaukums").getValue(String.class);  // Get equipment name
                    if (equipmentName != null) {
                        equipmentList.add(equipmentName);
                    }
                }
                ArrayAdapter<String> equipmentAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, equipmentList);
                equipmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                equipmentSpinner.setAdapter(equipmentAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void fetchAdminAndWorkerEmails() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> emails = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("Statuss").getValue(String.class);
                    String email = userSnapshot.child("epasts").getValue(String.class);

                    if (email != null && ("Admin".equalsIgnoreCase(status) || "Darbinieks".equalsIgnoreCase(status))) {
                        emails.add(email);
                    }
                }

                if (!emails.isEmpty()) {
                    sendEmailToAdmin(emails);
                } else {
                    Toast.makeText(getContext(), getString(R.string.no_account_found), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), getString(R.string.failed_retrieve_info), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendEmailToAdmin(List<String> emails) {
        String telpanr = edittelpanr.getText().toString();
        String stacijanr = stacijaSpinner.getSelectedItem().toString();
        String equipmentName = equipmentSpinner.getSelectedItem().toString(); // Equipment name from Spinner
        String description = descriptionEditText.getText().toString();

        String emailBody = "Telpa: " + telpanr +
                "\nStacija: " + stacijanr +
                "\nPriekšmetu nosaukums: " + equipmentName +
                "\nProblēma: \n" + description;

        emailSender.sendEmail(emails, "Problēmas ziņojums", emailBody);
        Toast.makeText(getContext(), getString(R.string.email_sent), Toast.LENGTH_SHORT).show();
    }
}
