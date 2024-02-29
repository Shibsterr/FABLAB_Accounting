package com.example.fablab.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class SpecificEquipmentFragment extends Fragment {
    private DatabaseReference databaseReference;

    private TextView titletext, desctext;
    private ImageView equipimg;
    private Button instbtn, stockbtn;
    private String test;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("stations");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_specific_equipment, container, false);
        titletext = view.findViewById(R.id.equip_title);
        desctext = view.findViewById(R.id.equip_desc);
        equipimg = view.findViewById(R.id.equip_image);

        instbtn = view.findViewById(R.id.instructions);
        stockbtn = view.findViewById(R.id.stock);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String statuss = dataSnapshot.child("Statuss").getValue(String.class);
                    if (statuss != null) {
                        switch (statuss) {
                            case "Lietotājs":
                                stockbtn.setVisibility(View.GONE);
                                break;
                            case "Darbinieks":
                            case "Admin":
                                stockbtn.setVisibility(View.VISIBLE);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("MainActivity", "Error with reading statuss");
            }

        });

        // Retrieve the data passed from the previous fragment
        Bundle bundle = getArguments(); //MANUALLY SELECTED EQUIPMENT or QR Code scanned

        if (bundle != null) {
            if(bundle.containsKey("equipment_name")){   //Happens if the user does the slow way of finding item or just doesnt have a camera :skull:
                String equipmentName = bundle.getString("equipment_name");
                String stationNodeName = EquipmentListFragment.statNodeName;
                Log.d("SpecificEquipmentFragment", "Equipment name: " + equipmentName);
                Log.d("SpecificEquipmentFragment", "Station node name: " + stationNodeName);

                // Query the database to find details about the clicked equipment
                DatabaseReference stationRef = databaseReference.child(stationNodeName).child("Equipment");
                stationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String name = snapshot.child("Nosaukums").getValue(String.class);
                            if (name != null && name.equals(equipmentName)) {

                                String description = snapshot.child("Description").getValue(String.class);
                                String imageUrl = snapshot.child("Attēls").getValue(String.class);
                                test = snapshot.child("Kods").getValue(String.class);
                                Log.d("SpecificEquipmentFragment", "Description: " + description);

                                // Update UI with the retrieved details
                                equipimg.setClipToOutline(true);
                                titletext.setText(name);
                                desctext.setText(description);
                                Glide.with(requireContext()).load(imageUrl).into(equipimg);

                                break; // Exit loop once the equipment is found
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle database error
                    }
                });
                instbtn.setOnClickListener(v -> {
                    // Retrieve the equipment code from the bundle
                    String equipmentCode = test;
                    Log.d("SpecificEquipmentFragment", "This is the code: "+test);
                    // Construct the file name for the PDF (assuming it has the same name as the equipment code)
                    String fileName = equipmentCode + ".pdf";

                    // Get a reference to the PDF file in Firebase Storage
                    String filePath = "Equipment_instructions/" + fileName;
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference(filePath);

                    // Create a local file to store the downloaded PDF
                    File localFile  ;
                    try {
                        localFile = File.createTempFile("temp_pdf", "pdf");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error: Unable to create local file", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Download the PDF file from Firebase Storage to the local file
                    File finalLocalFile = localFile;
                    storageRef.getFile(localFile)
                            .addOnSuccessListener(taskSnapshot -> {
                                // File downloaded successfully, open the PDF using an Intent
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                Uri pdfUri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".provider", finalLocalFile);
                                intent.setDataAndType(pdfUri, "application/pdf");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(getContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(exception -> {
                                // Handle any errors
                                Toast.makeText(getContext(), "Error: PDF not found", Toast.LENGTH_SHORT).show();
                                Log.e("SpecificEquipmentFragment", "Error downloading PDF: " + exception.getMessage());
                            });
                });
            }else if(bundle.containsKey("code")){
                String equipmentCode = bundle.getString("code");
                // Get a reference to the database node containing stations
                DatabaseReference stationsRef = FirebaseDatabase.getInstance().getReference().child("stations");

                instbtn.setOnClickListener(v -> {

                    Log.d("SpecificEquipmentFragment", "This is the code: "+equipmentCode);
                    // Construct the file name for the PDF (assuming it has the same name as the equipment code)
                    String fileName = equipmentCode + ".pdf";

                    // Get a reference to the PDF file in Firebase Storage
                    String filePath = "Equipment_instructions/" + fileName;
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference(filePath);

                    // Create a local file to store the downloaded PDF
                    File localFile  ;
                    try {
                        localFile = File.createTempFile("temp_pdf", "pdf");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error: Unable to create local file", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Download the PDF file from Firebase Storage to the local file
                    File finalLocalFile = localFile;
                    storageRef.getFile(localFile)
                            .addOnSuccessListener(taskSnapshot -> {
                                // File downloaded successfully, open the PDF using an Intent
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                Uri pdfUri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".provider", finalLocalFile);
                                intent.setDataAndType(pdfUri, "application/pdf");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(getContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(exception -> {
                                // Handle any errors
                                Toast.makeText(getContext(), "Error: Unable to download PDF", Toast.LENGTH_SHORT).show();
                                Log.e("SpecificEquipmentFragment", "Error downloading PDF: " + exception.getMessage());
                            });
                });

                // Query the database to find the station node containing the equipment with the specified code
                stationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Check if the dataSnapshot contains any stations
                        if (dataSnapshot.exists()) {
                            // Iterate over each station
                            for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                                // Get a reference to the equipment node within the current station
                                DatabaseReference equipmentRef = stationSnapshot.child("Equipment").getRef();
                                // Query the equipment node to find the equipment with the specified code
                                equipmentRef.orderByChild("Kods").equalTo(equipmentCode).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        // Check if the dataSnapshot contains the equipment with the specified code
                                        if (dataSnapshot.exists()) {
                                            // Loop through the dataSnapshot to find the specific equipment
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                // Retrieve the equipment details
                                                String equipmentName = snapshot.child("Nosaukums").getValue(String.class);
                                                String description = snapshot.child("Description").getValue(String.class);
                                                String imageUrl = snapshot.child("Attēls").getValue(String.class);

                                                // Update UI with the retrieved details
                                                equipimg.setClipToOutline(true);
                                                titletext.setText(equipmentName);
                                                desctext.setText(description);
                                                Glide.with(requireContext()).load(imageUrl).into(equipimg);

                                                // Log equipment details for debugging
                                                Log.d("SpecificEquipmentFragment", "Equipment name from QR code: " + equipmentName);
                                                Log.d("SpecificEquipmentFragment", "Description: " + description);
                                                Log.d("SpecificEquipmentFragment", "Image URL: " + imageUrl);
                                            }
                                        } else {
                                            // Handle the case where the equipment data is not found in this station
                                            Log.d("SpecificEquipmentFragment", "Equipment with code " + equipmentCode + " not found in station " + stationSnapshot.getKey());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Handle database error
                                        Log.e("SpecificEquipmentFragment", "Database Error: " + databaseError.getMessage());
                                    }
                                });
                            }
                        } else {
                            // Handle the case where there are no stations in the database
                            Log.d("SpecificEquipmentFragment", "No stations found in the database");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle database error
                        Log.e("SpecificEquipmentFragment", "Database Error: " + databaseError.getMessage());
                    }
                });
            }
        }else{
            Log.d("yippe", "lmao its trying to do manual request");
        }
        return view;
    }
}