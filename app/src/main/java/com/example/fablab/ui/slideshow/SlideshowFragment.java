package com.example.fablab.ui.slideshow;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.fablab.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private EditText descriptionEditText;
    private TextView selectedPictureTextView;

    private String currentPhotoPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slideshow, container, false);

        descriptionEditText = view.findViewById(R.id.Description);
        selectedPictureTextView = view.findViewById(R.id.selected_picture);

        Button attachButton = view.findViewById(R.id.attach);
        attachButton.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });

        Button sendButton = view.findViewById(R.id.send_it);
        sendButton.setOnClickListener(v -> sendEmail());

        return view;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK) {
            // Update the TextView with the name of the selected picture
            selectedPictureTextView.setText("Picture has been taken and attached");
        }
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
        // Construct your email and send it to the list of emails
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");

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

        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailList});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Problem Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, descriptionEditText.getText().toString());

        if (currentPhotoPath != null) {
            File photoFile = new File(currentPhotoPath);
            if (photoFile.exists()) {
                Uri photoUri = FileProvider.getUriForFile(requireContext(),
                        "com.example.fablab.provider", photoFile);
                ArrayList<Uri> uris = new ArrayList<>();
                uris.add(photoUri);
                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                emailIntent.setType("image/png");
            }
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

}
