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

    // Firebase datu bāzes references un UI komponentes
    private DatabaseReference databaseReference;
    private TextView titletext, desctext, maxstc, minstc, critstc, basestock, roomtxt;
    private ImageView equipimg;
    private Button addbtn, subtractbtn, maxStockButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Iegūst valodas iestatījumus no SharedPreferences un iestata lokalizāciju
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        Log.d("MainActivity", "Language Code: " + languageCode);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Uzpūš fragmenta izkārtojumu un inicializē UI komponentes
        View view = inflater.inflate(R.layout.fragment_spec_stock_equipment, container, false);
        Bundle bundle = getArguments();

        // Saista UI elementus ar XML komponentēm
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

        // Pievieno notikumu klausītājus pogām
        maxStockButton.setOnClickListener(v -> showEditMaxStockDialog());
        addbtn.setOnClickListener(v -> onAddButtonClicked());
        subtractbtn.setOnClickListener(v -> onSubtractButtonClicked());

        // Ja ir saņemts iekārtas nosaukums no iepriekšējā fragmenta, tad pieprasa datus no Firebase
        if (bundle != null && bundle.containsKey("equipment_name")) {
            String equipmentName = bundle.getString("equipment_name");
            Log.d("StockSpecificEquipment", "Equipment name: " + equipmentName);

            // Iegūst datus no Firebase par konkrēto iekārtu
            DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("equipment");
            Query query = equipmentRef.orderByChild("Nosaukums").equalTo(equipmentName);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Ja atrasta atbilstoša iekārta
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            // Izgūst iekārtas detaļas
                            String imageName = snapshot.child("Attēls").getValue(String.class);
                            String description = snapshot.child("Description").getValue(String.class);
                            int minStock = snapshot.child("Min Stock").getValue(Integer.class);
                            int maxStock = snapshot.child("Max Stock").getValue(Integer.class);
                            int critStock = snapshot.child("Critical Stock").getValue(Integer.class);
                            int stk = snapshot.child("Skaits").getValue(Integer.class);

                            // Formatē tekstus un iestata tos UI komponentēs
                            String min = getString(R.string.minimum_stock_0, minStock);
                            String max = getString(R.string.maximum_stock_0, maxStock);
                            String crit = getString(R.string.critical_stock_0, critStock);
                            String stock = getString(R.string.stock_0, stk);

                            equipimg.setClipToOutline(true);
                            titletext.setText(equipmentName);
                            desctext.setText(description);
                            maxstc.setText(max);
                            minstc.setText(min);
                            critstc.setText(crit);
                            basestock.setText(stock);

                            // Ielādē attēlu ar Glide
                            Glide.with(requireContext()).load(imageName).into(equipimg);

                            // Atjauno pogu statusus
                            updateBtn();
                        }
                    } else {
                        // Ja nav atrasta atbilstoša iekārta
                        Log.d("StockSpecificEquipment", "Equipment with name '" + equipmentName + "' not found.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Apstrādā Firebase vaicājuma kļūdas
                    Log.e("StockSpecificEquipment", "Error querying equipment: " + databaseError.getMessage());
                }
            });
        }
        return view;
    }

    private void showQuantityInputDialog(boolean isAddOperation) {
        // Rāda dialogu, kur ievadīt pievienojamo vai atņemamo daudzumu
        QuantityInputDialogFragment dialogFragment = new QuantityInputDialogFragment(quantity -> {
            int currentStock = parseStockFromText(basestock.getText().toString(), R.string.stock_0);

            // Aprēķina jauno krājumu, ņemot vērā robežvērtības
            int adjustedQuantity = isAddOperation ? quantity : Math.min(quantity, currentStock);
            int newStock = isAddOperation ? currentStock + adjustedQuantity : currentStock - adjustedQuantity;

            // Iegūst robežvērtības no UI
            int maxStock = extractStockValue(maxstc.getText().toString(), 0);
            int critStock = extractStockValue(critstc.getText().toString(), 0);
            int minStock = extractStockValue(minstc.getText().toString(), 0);

            // Sūta e-pastu, ja krājums zem minimuma vai kritiska
            if (newStock < minStock) {
                sendEmailToAdmin();
            } else if (newStock > maxStock) {
                newStock = maxStock;
            } else if (newStock < critStock) {
                sendEmailToAdmin();
            }

            // Atjauno UI un Firebase
            basestock.setText(getString(R.string.stock_0, newStock));
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

                            // Aizsargā pret pārsniegšanu
                            if (updatedStock > maxStock) {
                                updatedStock = maxStock;
                            }

                            // Atjauno Firebase un pievieno žurnāla ierakstu
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

    // Izvelk skaitli no teksta virknes (piemēram "Krājums: 15" -> 15)
    private int extractStockValue(String text, int fallback) {
        try {
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
        // Sūta e-pastu visiem administratoriem, ja krājums zem normas
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
                        // Veido ziņojuma tekstu un sūta to
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
                    // Firebase kļūdu apstrāde
                }
            });
        }
    }
    // Pievieno žurnāla ierakstu Firebase, kas norāda, vai lietotājs pievienoja vai noņēma vienības
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
                    Log.e("AddLogEntry", "Lietotāja dati nav atrasti.");
                    return;
                }

                // Iegūst lietotāja vārdu no Firebase
                String fullName = dataSnapshot.child("Vards un uzvards").getValue(String.class);

                // Formatē pašreizējo datumu un laiku
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                String dateTime = sdf.format(new Date());

                // Veido kopsavilkumu, pamatojoties uz darbību
                String summary = isAddOperation
                        ? fullName + " pievienoja " + quantity + " vienības priekšmetam '" + equipmentName + "'."
                        : fullName + " noņēma " + quantity + " vienības no priekšmeta '" + equipmentName + "'.";

                // Veido virsrakstu žurnāla ierakstam
                String title = isAddOperation
                        ? "Iekārtes vienību pievienošana " + dateTime
                        : "Iekārtes vienību noņemšana " + dateTime;

                DatabaseReference logRef = FirebaseDatabase.getInstance().getReference()
                        .child("Logs").child(dateTime);

                // Izveido ierakstu kā HashMap
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("user", fullName);
                logEntry.put("email", email);
                logEntry.put("title", title);
                logEntry.put("summary", summary);

                // Saglabā žurnāla ierakstu Firebase
                logRef.setValue(logEntry)
                        .addOnSuccessListener(aVoid -> Log.d("AddLogEntry", "Žurnāla ieraksts veiksmīgi pievienots"))
                        .addOnFailureListener(e -> Log.e("AddLogEntry", "Kļūda pievienojot žurnāla ierakstu", e));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AddLogEntry", "Datu bāzes kļūda: " + databaseError.getMessage());
            }
        });
    }

    // Apstrādā pievienošanas pogas klikšķi – atver ievades dialogu ar pievienošanas loģiku
    private void onAddButtonClicked() {
        showQuantityInputDialog(true); // true = pievienošana
    }

    // Apstrādā noņemšanas pogas klikšķi – atver ievades dialogu ar noņemšanas loģiku
    private void onSubtractButtonClicked() {
        showQuantityInputDialog(false); // false = noņemšana
    }

    // Parāda dialogu, kas ļauj rediģēt maksimālo krājumu daudzumu konkrētai iekārtai
    private void showEditMaxStockDialog() {
        final EditText input = new EditText(getContext()); // Teksta lauks ievadei
        input.setInputType(InputType.TYPE_CLASS_NUMBER); // Atļauj tikai skaitļus

        // Izveido un parāda ievades dialogu
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.max_changetitle))
                .setMessage(getString(R.string.max_change))
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String inputText = input.getText().toString();

                    if (!inputText.isEmpty()) {
                        try {
                            int newMaxStock = Integer.parseInt(inputText); // Parsē skaitli
                            updateMaxStock(newMaxStock); // Atjaunina Firebase un UI
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

    // Atjaunina Firebase un UI ar jauno maksimālā krājuma daudzumu izvēlētajai iekārtai
    private void updateMaxStock(int newMaxStock) {
        String equipmentName = titletext.getText().toString(); // Iegūst iekārtas nosaukumu

        maxstc.setText(getString(R.string.maximum_stock_0, newMaxStock)); // Atjaunina UI

        // Atrod attiecīgo iekārtu Firebase datubāzē
        DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference().child("equipment");
        Query query = equipmentRef.orderByChild("Nosaukums").equalTo(equipmentName);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            // Atjaunina "Max Stock" Firebase datubāzē
                            snapshot.getRef().child("Max Stock").setValue(newMaxStock)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), getString(R.string.max_stock_good), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), getString(R.string.max_stock_bad), Toast.LENGTH_SHORT).show();
                                    });

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Kļūda lasot krājuma vērtības", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.equip_notfound), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UpdateMaxStock", "Kļūda atjauninot max krājumu: " + databaseError.getMessage());
            }
        });

        updateBtn(); // Atjaunina pogu stāvokļus
    }

    // Atjaunina pogu stāvokļus (aktīva/neaktīva) atkarībā no pašreizējā krājumu daudzuma
    private void updateBtn() {
        int currentStock = parseStockFromText(basestock.getText().toString(), R.string.stock_0);
        int minStock = extractStockValue(minstc.getText().toString(), 0);
        int maxStock = extractStockValue(maxstc.getText().toString(), 0);

        // Poga “noņemt” pieejama tikai, ja ir vairāk nekā min
        subtractbtn.setEnabled(currentStock > minStock);

        // Poga “pievienot” pieejama tikai, ja ir mazāk nekā max
        addbtn.setEnabled(currentStock < maxStock);
    }

    // Parsē krājumu vērtību no tekstā norādītās informācijas
    private int parseStockFromText(String fullText, int stringId) {
        String prefix = getString(stringId).replace("%1$d", "").trim(); // Iegūst prefiksu bez skaitļa

        try {
            return Integer.parseInt(fullText.replace(prefix, "").trim()); // Izņem skaitli no teksta
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1; // Gadījumā, ja neizdodas parsēt
        }
    }


}