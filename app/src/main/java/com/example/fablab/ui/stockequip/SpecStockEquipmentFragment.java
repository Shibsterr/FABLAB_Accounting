package com.example.fablab.ui.stockequip;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.example.fablab.EmailSender;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecStockEquipmentFragment extends Fragment {

    private DatabaseReference databaseReference;
    private TextView titletext, desctext,
            maxstc, minstc, critstc, basestock, roomtxt;
    private ImageView equipimg;
    private Button addbtn, subtractbtn, maxStockButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        // Set locale based on saved language preference
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        // Update the configuration and display metrics
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        Log.d("MainActivity", "Language Code: " + languageCode);
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
        maxStockButton = view.findViewById(R.id.maxstock_button);

        maxStockButton.setOnClickListener(v -> showEditMaxStockDialog());
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
                            int stk = snapshot.child("Skaits").getValue(Integer.class);


                            String min, max, crit, stock;
                            stock = getString(R.string.stock_0, stk);
                            min = getString(R.string.minimum_stock_0, minStock);
                            max = getString(R.string.maximum_stock_0, maxStock);
                            crit = getString(R.string.critical_stock_0, critStock);

                            // Update UI with the retrieved details
                            equipimg.setClipToOutline(true);
                            titletext.setText(equipmentName);
                            desctext.setText(description);

                            maxstc.setText(max);
                            minstc.setText(min);
                            critstc.setText(crit);
                            basestock.setText(stock);

                            Glide.with(requireContext()).load(imageName).into(equipimg);
                            String stockText = basestock.getText().toString();
                            String stockPrefix = getString(R.string.stock); // e.g., "Stock: " or "Krājums: "

                            updateBtn();


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

    private void showQuantityInputDialog(boolean isAddOperation) {
        QuantityInputDialogFragment dialogFragment = new QuantityInputDialogFragment(quantity -> {
            int currentStock = parseStockFromText(basestock.getText().toString(), R.string.stock_0);

            // Adjust quantity to prevent negative stock
            int adjustedQuantity = isAddOperation ? quantity : Math.min(quantity, currentStock);
            int newStock = isAddOperation ? currentStock + adjustedQuantity : currentStock - adjustedQuantity;

            // Fetch limits
            int maxStock = extractStockValue(maxstc.getText().toString(), 0);
            int critStock = extractStockValue(critstc.getText().toString(), 0);
            int minStock = extractStockValue(minstc.getText().toString(), 0);

            // Email alerts and capping
            if (newStock < minStock) {
                sendEmailToAdmin(); // Alert when below min
            } else if (newStock > maxStock) {
                newStock = maxStock; // Cap to max
            } else if (newStock < critStock) {
                sendEmailToAdmin(); // Alert when below critical
            }

            // Update UI
            basestock.setText(getString(R.string.stock_0, newStock));

            // Update Firebase
            String equipmentName = titletext.getText().toString();
            DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("equipment");

            Query query = equipmentRef.orderByChild("Nosaukums").equalTo(equipmentName);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Integer currentStockFromDB = snapshot.child("Skaits").getValue(Integer.class);
                            if (currentStockFromDB == null) currentStockFromDB = 0;

                            int adjustedDbQuantity = isAddOperation ? quantity : Math.min(quantity, currentStockFromDB);
                            int updatedStock = isAddOperation ? currentStockFromDB + adjustedDbQuantity : currentStockFromDB - adjustedDbQuantity;

                            // Cap to max
                            if (updatedStock > maxStock) {
                                updatedStock = maxStock;
                            }

                            snapshot.getRef().child("Skaits").setValue(updatedStock);
                            addLogEntry(equipmentName, adjustedDbQuantity, isAddOperation);
                            updateBtn();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("UpdateStock", "Error updating stock: " + databaseError.getMessage());
                }
            });
        }, isAddOperation);

        dialogFragment.show(getParentFragmentManager(), "quantity_input_dialog");
    }

    // Utility method to extract digits from a string and parse to int
    private int extractStockValue(String text, int fallback) {
        try {
            // This will extract the first number found in the string
            Matcher matcher = Pattern.compile("\\d+").matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return fallback;
    }

    private void sendEmailToAdmin() {
        int maxStock = extractStockValue(maxstc.getText().toString(), 0);
        int minStock = extractStockValue(minstc.getText().toString(), 0);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<String> adminEmails = new ArrayList<>();
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userStatus = userSnapshot.child("Statuss").getValue(String.class);
                        if ("Admin".equals(userStatus)) {
                            String adminEmail = userSnapshot.child("epasts").getValue(String.class);
                            if (adminEmail != null) {
                                adminEmails.add(adminEmail);
                            }
                        }
                    }
                    if (!adminEmails.isEmpty()) {
                        String subject = "Paziņojums par zemu krājumu";
                        String message = "Sveiki!\n\n" +
                                "Uzmanību! Preces '" + titletext.getText().toString() + "' krājums ir zemā līmenī.\n" +
                                "Detalizēta informācija:\n" +
                                "Nosaukums: " + titletext.getText().toString() + "\n" +
                                "Pašreizējais krājums: " + basestock.getText().toString() + "\n" +
                                "Minimālais krājums: " + minStock + "\n" +
                                "Maksimālais krājums: " + maxStock + "\n" +
                                "Paldies!";

                        EmailSender emailSender = new EmailSender("fablabappnoreply@gmail.com", "xllk wqet dulg xabp");
                        emailSender.sendEmail(adminEmails, subject, message);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("SendEmailToAdmin", "Database error: " + databaseError.getMessage());
                }
            });
        }
    }

    private void addLogEntry(String equipmentName, int quantity, boolean isAddOperation) {
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

                // Summary based on action
                String summary = isAddOperation
                        ? fullName + " pievienoja " + quantity + " vienības priekšmetam '" + equipmentName + "'."
                        : fullName + " noņēma " + quantity + " vienības no priekšmeta '" + equipmentName + "'.";

                String title = isAddOperation
                        ? "Iekārtes vienību pievienošana " + dateTime
                        : "Iekārtes vienību noņemšana " + dateTime;
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

    private void onAddButtonClicked() {
        showQuantityInputDialog(true);
    }

    private void onSubtractButtonClicked() {
        showQuantityInputDialog(false);
    }

    private void showEditMaxStockDialog() {
        // Create an EditText view for user input
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER); // Only allow integer input

        // Create the dialog
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.max_changetitle))
                .setMessage(getString(R.string.max_change))
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String inputText = input.getText().toString();

                    if (!inputText.isEmpty()) {
                        try {
                            int newMaxStock = Integer.parseInt(inputText);
                            updateMaxStock(newMaxStock); // Update the maxStock in the UI and Firebase
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                .show();
    }

    private void updateMaxStock(int newMaxStock) {
        String equipmentName = titletext.getText().toString();

        // Update the UI with localized string
        maxstc.setText(getString(R.string.maximum_stock_0, newMaxStock));

        // Update the value in Firebase
        DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("equipment");
        Query query = equipmentRef.orderByChild("Nosaukums").equalTo(equipmentName);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            // Update the "Max Stock" value in the database
                            snapshot.getRef().child("Max Stock").setValue(newMaxStock)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), getString(R.string.max_stock_good), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), getString(R.string.max_stock_bad), Toast.LENGTH_SHORT).show();
                                    });

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error reading stock values", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.equip_notfound), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UpdateMaxStock", "Error updating max stock: " + databaseError.getMessage());
            }
        });

        updateBtn(); // Update the button states
    }

    private void updateBtn() {
        int currentStock = parseStockFromText(basestock.getText().toString(), R.string.stock_0);
        int minStock = extractStockValue(minstc.getText().toString(), 0);
        int maxStock = extractStockValue(maxstc.getText().toString(), 0);

        subtractbtn.setEnabled(currentStock > minStock);
        addbtn.setEnabled(currentStock < maxStock);
    }

    private int parseStockFromText(String fullText, int stringId) {
        String prefix = getString(stringId).replace("%1$d", "").trim();
        try {
            return Integer.parseInt(fullText.replace(prefix, "").trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1; // or throw, or return 0 as fallback
        }
    }

}