package com.example.fablab;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.example.fablab.databinding.ActivityMainBinding;
import com.example.fablab.ui.authen.RegisterUser;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private TextView fullname, email;
    private View loadingScreen;
    private DatabaseReference userRef;
    private ValueEventListener userListener;
    private FirebaseUser currentUser;
    private ActivityMainBinding binding;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    String scannedCode = result.getContents(); //Saņem noskanētu rezultātu
                    if (isValidScan(scannedCode)) { // Pārbauda vai dati atbilst
                        searchEquipmentInFirebase(scannedCode); // Ja jā tad padod tos datus uz meklēšanu un atver to iekārti
                    } else {
                        Toast.makeText(this, getString(R.string.invalid_qr_code_scanned), Toast.LENGTH_LONG).show();
                        scanCode(null);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ielādē iepriekš izvēlēto motīvu (theme) un to uzstāda
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

        // Ielādē iepriekš izvēlēto valodu un to uzstāda
        String languageCode = sharedPreferences.getString("language_preference", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = getResources().getConfiguration();
        configuration.setLocale(locale);
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());

        // Uzstāda sākotnējo skatu ar ielādes ekrānu
        setContentView(R.layout.activity_loading);
        loadingScreen = findViewById(R.id.progressBar);

        // Pārbauda, vai ir pieejams interneta savienojums (WiFi vai mobilie dati)
        if (!isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        // Pārbauda, vai ir pieslēgts lietotājs
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Ja nav lietotāja, pārvirza uz reģistrācijas aktivitāti
            startActivity(new Intent(MainActivity.this, RegisterUser.class));
            finish();
        } else {
            // Ja ir pieslēgts lietotājs, iegūst lietotāja datus no Firebase
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.keepSynced(true); // Nodrošina, ka dati vienmēr ir sinhronizēti

            userListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(MainActivity.this, getString(R.string.no_data_user), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Datu apstrāde tiek veikta UI pavedienā
                    runOnUiThread(() -> {
                        if (binding == null) {
                            setupMainContent(snapshot);
                        } else {
                            updateSidebarUI(snapshot);
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, getString(R.string.database_error), Toast.LENGTH_SHORT).show();
                }
            };
            userRef.addValueEventListener(userListener);
        }
    }


    private void setupMainContent(DataSnapshot dataSnapshot) {
        // Inicializē galveno aktivitātes saturu ar DrawerLayout un NavigationView
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Konfigurē navigācijas izvēlni
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.new_equip, R.id.nav_task, R.id.nav_assign,
                R.id.nav_logs, R.id.nav_report, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        updateSidebarUI(dataSnapshot);
    }


    private void updateSidebarUI(DataSnapshot snapshot) {
        // Atjaunina sānu izvēlni ar lietotāja vārdu un e-pastu
        View headerView = binding.navView.getHeaderView(0);
        fullname = headerView.findViewById(R.id.nameSurname);
        email = headerView.findViewById(R.id.epasts);

        String name = snapshot.child("Vards un uzvards").getValue(String.class);
        String userEmail = snapshot.child("epasts").getValue(String.class);
        String statuss = snapshot.child("Statuss").getValue(String.class);

        fullname.setText(name != null ? name : "No name");
        email.setText(userEmail != null ? userEmail : "No email");

        // Pēc lietotāja statusa nosaka, kuras izvēlnes opcijas būs redzamas
        Menu navMenu = binding.navView.getMenu();
        navMenu.findItem(R.id.new_equip).setVisible("Darbinieks".equals(statuss) || "Admin".equals(statuss));
        navMenu.findItem(R.id.nav_task).setVisible("Darbinieks".equals(statuss) || "Admin".equals(statuss));
        navMenu.findItem(R.id.nav_logs).setVisible("Admin".equals(statuss));
        navMenu.findItem(R.id.nav_assign).setVisible("Darbinieks".equals(statuss) || "Admin".equals(statuss));
        navMenu.findItem(R.id.nav_updateuser).setVisible("Admin".equals(statuss));
    }


    private boolean isNetworkAvailable() {
        // Pārbauda, vai ierīcei ir aktīvs interneta savienojums
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }


    private boolean isValidScan(String data) {
        // Validē skenēto datu formātu (8 cipari vai specifiska struktūra)
        return data.length() == 8 || data.matches("^[1-9][0-9]?_[1-9]_[1-2]?_[a-zA-Z0-9\\s]+$")
                || data.matches("^[1-9][0-9]?_[1-9][0-9]?_[1-2]_[a-zA-Z0-9\\s]+$")
                || data.matches("^[1-9]_[1-9][0-9]?_[1-2]_[a-zA-Z0-9\\s]+$")
                || data.matches("^[1-9]_[1-9]_[1-2]_[a-zA-Z0-9\\s]+$");
    }


    private void searchEquipmentInFirebase(String scannedCode) {
        // Meklē iekārtu Firebase datubāzē pēc skenētā koda
        FirebaseDatabase.getInstance().getReference("equipment").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean found = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String kods = snapshot.child("Kods").getValue(String.class);
                            String izgKods = snapshot.child("IzgKods").getValue(String.class);
                            if (scannedCode.equals(kods) || scannedCode.equals(izgKods)) {
                                openSpecificEquipmentFragment(snapshot.getKey());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            Toast.makeText(MainActivity.this, "Item not found, please try again.", Toast.LENGTH_LONG).show();
                            scanCode(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("MainActivity", "Firebase error", error.toException());
                    }
                });
    }


    private void openSpecificEquipmentFragment(String equipmentId) {
        // Atver specifisko iekārtas fragmentu, nododot tā ID
        Bundle bundle = new Bundle();
        bundle.putString("code", equipmentId);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.specificEquipmentFragment, bundle);
        Toast.makeText(this, "Scanned: " + equipmentId, Toast.LENGTH_LONG).show();
        addLogEntry(equipmentId);
    }


    public void scanCode(MenuItem item) {
        // Palaiž QR koda skeneri ar noteiktiem iestatījumiem
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.volume_prompt_qr));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }


    private void addLogEntry(String code) {
        // Pievieno jaunu ierakstu žurnālā par skenēto iekārtu
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

                // Formatē pašreizējo datumu un laiku uz (DD-MM-YYYY)
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                String dateTime = sdf.format(new Date());

                String title = "Noskanēja kodu " + dateTime;

                String summary = fullName + " noskanēja kodu '" + code + "'.";

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }


    //pievieno pogu qr augšējā joslā (NEPIECIEŠAMS)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // nepieciešama, lai trīs līniju (hamburger) poga darbotos (NEPIECIEŠAMS)
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}
