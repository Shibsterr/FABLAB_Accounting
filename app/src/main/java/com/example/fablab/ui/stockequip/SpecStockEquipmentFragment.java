package com.example.fablab.ui.stockequip;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fablab.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SpecStockEquipmentFragment extends Fragment {

    private DatabaseReference databaseReference;
    private TextView titletext, desctext,
            maxstc,minstc,critstc,basestock;
    private ImageView equipimg;
    private Button addbtn,subtractbtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spec_stock_equipment, container, false);
        // Retrieve the data passed from the previous fragment
        Bundle bundle = getArguments();

        titletext = view.findViewById(R.id.equip_title);
        desctext = view.findViewById(R.id.equip_desc);
        equipimg = view.findViewById(R.id.equip_image);
        basestock = view.findViewById(R.id.stock);
        maxstc = view.findViewById(R.id.maxstock);
        minstc = view.findViewById(R.id.minstock);
        critstc = view.findViewById(R.id.critstock);

        addbtn = view.findViewById(R.id.addbutton);
        subtractbtn = view.findViewById(R.id.subtractbutton);


        addbtn.setOnClickListener(v -> onAddButtonClicked());
        subtractbtn.setOnClickListener(v -> onSubtractButtonClicked());

        if (bundle != null && bundle.containsKey("equipment_name")) {
            String equipmentName = bundle.getString("equipment_name");
            Log.d("StockSpecificEquipment", "Equipment name: " + equipmentName);

            // Query the database to find details about the clicked equipment
            DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("equipment");

            Query query = equipmentRef.orderByChild("Nosaukums").equalTo(equipmentName);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if the equipment with the given name exists in the database
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            // Get the details of the equipment
                            String imageName = snapshot.child("Attēls").getValue(String.class);
                            String description = snapshot.child("Description").getValue(String.class);
                            int minStock = snapshot.child("Min Stock").getValue(Integer.class);
                            int maxStock = snapshot.child("Max Stock").getValue(Integer.class);
                            int critStock = snapshot.child("Critical Stock").getValue(Integer.class);
                            String stk = snapshot.child("Skaits").getValue(String.class);


                            String min,max,crit,stock;
                            stock = "Stock: "+stk;
                            min = "Minimum stock: "+minStock;
                            max = "Maximum stock: "+maxStock;
                            crit = "Critical stock: "+critStock;

                            // Update UI with the retrieved details
                            equipimg.setClipToOutline(true);
                            titletext.setText(equipmentName);
                            desctext.setText(description);

                            maxstc.setText(max);
                            minstc.setText(min);
                            critstc.setText(crit);
                            basestock.setText(stock);

                            Glide.with(requireContext()).load(imageName).into(equipimg);
                        }
                    } else {
                        // Equipment with the given name does not exist
                        Log.d("StockSpecificEquipment", "Equipment with name '" + equipmentName + "' not found.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle errors
                    Log.e("StockSpecificEquipment", "Error querying equipment: " + databaseError.getMessage());
                }
            });
        }
        return view;
        }

    // Inside SpecStockEquipmentFragment class

    private void showQuantityInputDialog(boolean isAddOperation) {
        QuantityInputDialogFragment dialogFragment = new QuantityInputDialogFragment(new QuantityInputDialogFragment.QuantityInputListener() {
            @Override
            public void onQuantityInput(int quantity) {
                // Add or subtract the quantity based on user input
                // Ensure it cannot go above max and below crit
                // Update the stock accordingly
                int currentStock = Integer.parseInt(basestock.getText().toString().replace("Stock: ", ""));
                int newStock = isAddOperation ? currentStock + quantity : currentStock - quantity;

                // Ensure stock does not go above max or below crit
                int maxStock = Integer.parseInt(maxstc.getText().toString().replace("Maximum stock: ", ""));
                int critStock = Integer.parseInt(critstc.getText().toString().replace("Critical stock: ", ""));

                if (newStock > maxStock) {
                    newStock = maxStock;
                } else if (newStock < critStock) {
                    newStock = critStock;
                }

                // Update the UI with the new stock value
                basestock.setText("Stock: " + newStock);

                // Update the stock in the database
                String equipmentName = titletext.getText().toString();
                DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("equipment");

                Query query = equipmentRef.orderByChild("Nosaukums").equalTo(equipmentName);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Check if the equipment with the given name exists in the database
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                // Get the current stock from the database
                                int currentStockFromDB = Integer.parseInt(snapshot.child("Skaits").getValue(String.class));

                                // Update the stock value in the database based on the operation
                                int updatedStock = isAddOperation ? currentStockFromDB + quantity : currentStockFromDB - quantity;

                                // Ensure stock does not go above max or below crit
                                if (updatedStock > maxStock) {
                                    updatedStock = maxStock;
                                } else if (updatedStock < critStock) {
                                    updatedStock = critStock;
                                }

                                // Update the stock value in the database
                                snapshot.getRef().child("Skaits").setValue(String.valueOf(updatedStock));

                                // Add a log entry to Firestore
                                addLogEntry(equipmentName, quantity, isAddOperation);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle errors
                        Log.e("UpdateStock", "Error updating stock: " + databaseError.getMessage());
                    }
                });
            }
        }, isAddOperation);
        dialogFragment.show(getParentFragmentManager(), "quantity_input_dialog");
    }

    // Method to add a log entry to the Realtime Database
// Method to add a log entry to the Realtime Database
    private void addLogEntry(String equipmentName, int quantity, boolean isAddOperation) {
        // Get the current user's UID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        // Get a reference to the user's node in the Realtime Database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);

        // Fetch the user's full name from the Realtime Database
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the user's full name
                    String fullName = dataSnapshot.child("Vards un uzvards").getValue(String.class);

                    // Get current date and time
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    // Create a new log entry with the current date and time as the node name
                    DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference().child("Logs").child(dateTime);

                    // Create a map with the log entry data
                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("Priekšmeta nosaukums", equipmentName);
                    logEntry.put("Daudzums", quantity);
                    logEntry.put("Pievienošana?", isAddOperation);      //if false then its subtraction else its addition
                    logEntry.put("Vārds uzvārds", fullName); // Use the user's full name
                    logEntry.put("Epasts", currentUser.getEmail());

                    // Add the log entry to the Realtime Database
                    logsRef.setValue(logEntry)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("AddLogEntry", "Log entry added successfully");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("AddLogEntry", "Error adding log entry", e);
                                }
                            });
                } else {
                    Log.e("AddLogEntry", "User data not found in Realtime Database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AddLogEntry", "Database error: " + databaseError.getMessage());
            }
        });
    }

    // Call this method when the Add button is clicked
    private void onAddButtonClicked() {
        showQuantityInputDialog(true);
    }

    // Call this method when the Subtract button is clicked
    private void onSubtractButtonClicked() {
        showQuantityInputDialog(false);
    }


}