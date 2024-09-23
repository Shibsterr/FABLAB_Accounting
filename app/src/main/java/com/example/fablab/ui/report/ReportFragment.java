package com.example.fablab.ui.report;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private EditText descriptionEditText, edittelpanr, editname;
    private Spinner stacijaSpinner;
    private EmailSender emailSender;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slideshow, container, false);

        descriptionEditText = view.findViewById(R.id.Description);
        edittelpanr = view.findViewById(R.id.telpanr);
        editname = view.findViewById(R.id.itemname);
        stacijaSpinner = view.findViewById(R.id.stacijasnr_spinner);

        Button sendButton = view.findViewById(R.id.send_it);
        sendButton.setOnClickListener(v -> fetchAdminAndWorkerEmails());

        loadStationNames();

        // Initialize EmailSender with your email and password
        emailSender = new EmailSender("fablabappnoreply@gmail.com", "xllk wqet dulg xabp");

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
                for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                    String stationName = stationSnapshot.child("Name").child(getCurrentLanguage()).getValue(String.class);
                    if (stationName != null) {
                        stationNames.add(stationName);
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
                    Toast.makeText(getContext(), "No admins or workers found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendEmailToAdmin(List<String> emails) {
        String telpanr = edittelpanr.getText().toString();
        String stacijanr = stacijaSpinner.getSelectedItem().toString();
        String name = editname.getText().toString();
        String description = descriptionEditText.getText().toString();

        String emailBody = "Telpa: " + telpanr +
                "\nStacija: " + stacijanr +
                "\nPriekšmetu nosaukums: " + name +
                "\nProblēma: \n" + description;

        emailSender.sendEmail(emails, "Problem Report", emailBody);
        Toast.makeText(getContext(), "Email sent in background.", Toast.LENGTH_SHORT).show();
    }
}
