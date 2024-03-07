package com.example.fablab.ui.slideshow;

import android.content.ActivityNotFoundException;
import android.content.Intent;
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

import com.example.fablab.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment {

    private EditText descriptionEditText, edittelpanr, editname;
    private Spinner stacijaSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slideshow, container, false);

        descriptionEditText = view.findViewById(R.id.Description);
        edittelpanr = view.findViewById(R.id.telpanr);
        editname = view.findViewById(R.id.itemname);
        stacijaSpinner = view.findViewById(R.id.stacijasnr_spinner);

        Button sendButton = view.findViewById(R.id.send_it);
        sendButton.setOnClickListener(v -> sendEmail());

        loadStationNames(); // Load station names into the spinner

        return view;
    }

    private void loadStationNames() {
        DatabaseReference stationsRef = FirebaseDatabase.getInstance().getReference().child("stations");
        stationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> stationNames = new ArrayList<>();
                for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                    String stationName = stationSnapshot.child("Name").getValue(String.class);
                    if (stationName != null) {
                        stationNames.add(stationName);
                    }
                }
                // Populate spinner with station names
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

    private void sendEmail() {
        // Retrieve email list from Firebase Realtime Database based on user status
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> emails = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("Statuss").getValue(String.class);
                    String email = userSnapshot.child("epasts").getValue(String.class);
                    if (email != null && ("Admin".equals(status) || "Darbinieks".equals(status))) {
                        emails.add(email);
                    }
                }
                // Now you have the list of emails, you can use it to send the email
                sendEmailToAdmin(emails);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void sendEmailToAdmin(List<String> emails) {
        String text;
        String telpanr, stacijanr, name;

        telpanr = String.valueOf(edittelpanr.getText());
        stacijanr = String.valueOf(stacijaSpinner.getSelectedItem());
        name = String.valueOf(editname.getText());

        // Construct your email and send it to the list of emails
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("message/rfc822");

        // Convert the list of emails to a single string with comma-separated values
        StringBuilder emailStringBuilder = new StringBuilder();
        for (String email : emails) {
            emailStringBuilder.append(email).append(",");
        }
        // Remove the trailing comma
        String emailList = emailStringBuilder.toString();
        if (emailList.endsWith(",")) {
            emailList = emailList.substring(0, emailList.length() - 1);
        }
        text = "Telpa: " + telpanr +
                "\nStacija: " + stacijanr +
                "\nPriekšmetu nosaukums: " + name +
                "\nProblēma: \n"
                + descriptionEditText.getText().toString();
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailList});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Problem Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, text);

        // Set the package name of the Gmail app to force usage
        emailIntent.setPackage("com.google.android.gm");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }


}