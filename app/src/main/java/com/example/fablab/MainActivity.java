package com.example.fablab;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.example.fablab.ui.SpecificEquipmentFragment;
import com.example.fablab.ui.authen.RegisterUser;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private TextView fullname, email;
    private Button refreshbtn;
    private ProgressBar progbar;
    private View loadingScreen;
    private NetworkChangeReceiver networkChangeReceiver;

    private static final String PATTERN_STRING = "^[1-9][0-9]?_[1-9]_[1-2]?_[a-zA-Z0-9\\s]+$";
    private static final String PATTERN_LONGER = "^[1-9][0-9]?_[1-9][0-9]?_[1-2]_[a-zA-Z0-9\\s]+$";
    private static final String PATTERN = "^[1-9]_[1-9][0-9]?_[1-2]_[a-zA-Z0-9\\s]+$";
    private static final String SMALL_PATTERN = "^[1-9]_[1-9]_[1-2]_[a-zA-Z0-9\\s]+$";
    private boolean isReceiverRegistered = false; // Flag to track receiver registration
    private final ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Log.d("MainActivity", "Cancelled scan");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Log.d("MainActivity", "Cancelled scan due to missing camera permission");
                        Toast.makeText(MainActivity.this, "Cancelled due to missing camera permission", Toast.LENGTH_LONG).show();
                    }
                } else {
                    String scannedCode = result.getContents();
                    Log.d("MainActivity", "Scanned data: " + scannedCode);

                    if (isValidScan(scannedCode)) {
                        searchEquipmentInFirebase(scannedCode);
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid scan. Please try again.", Toast.LENGTH_LONG).show();
                        scanCode(null); // Restart scanning
                    }
                }
            });

    private boolean isValidScan(String data) {
        return data.length() == 8 || Pattern.matches(PATTERN_STRING, data) ||
                Pattern.matches(PATTERN_LONGER, data) ||
                Pattern.matches(PATTERN, data) ||
                Pattern.matches(SMALL_PATTERN, data);
    }
    private void searchEquipmentInFirebase(String scannedCode) {
        DatabaseReference equipmentRef = FirebaseDatabase.getInstance().getReference("equipment");
        equipmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean found = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String kods = snapshot.child("Kods").getValue(String.class);
                    String izgKods = snapshot.child("IzgKods").getValue(String.class);

                    if (scannedCode.equals(kods) || scannedCode.equals(izgKods)) {
                        String equipmentId = snapshot.getKey(); // Get the key of the equipment item
                        openSpecificEquipmentFragment(equipmentId);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    Toast.makeText(MainActivity.this, "Item not found, please try again.", Toast.LENGTH_LONG).show();
                    scanCode(null); // Restart scanning
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("MainActivity", "Failed to read value.", databaseError.toException());
            }
        });
    }
    private void openSpecificEquipmentFragment(String equipmentId) {
        Bundle bundle = new Bundle();
        bundle.putString("code", equipmentId);

        SpecificEquipmentFragment specificEquipmentFragment = new SpecificEquipmentFragment();
        specificEquipmentFragment.setArguments(bundle);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.specificEquipmentFragment, bundle);

        Toast.makeText(MainActivity.this, "Scanned: " + equipmentId, Toast.LENGTH_LONG).show();
        addLogEntry(equipmentId);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme and language before setting content view
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

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

        // Set the content view to the loading screen first
        setContentView(R.layout.activity_loading);
        loadingScreen = findViewById(R.id.progressBar);

        // Load the main content after a delay to simulate loading
        if (isNetworkAvailable()) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                startActivity(new Intent(MainActivity.this, RegisterUser.class));
                finish(); // Close the current activity
            } else {
                // Fetch Firebase data and show the main content after loading
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(Objects.requireNonNull(currentUser.getUid()));
                userRef.keepSynced(true);

                // Fetch the user's profile information
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Set up the main activity content after data is successfully loaded
                            setupMainContent(dataSnapshot);
                        } else {
                            Toast.makeText(MainActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                            loadingScreen.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d("MainActivity", "ERROR WITH USER");
                        loadingScreen.setVisibility(View.GONE); // Hide the loading screen if there's an error
                    }
                });
            }
        } else {
            Toast.makeText(MainActivity.this, "No network connection", Toast.LENGTH_SHORT).show();
            loadingScreen.setVisibility(View.GONE);
        }
    }

    private void setupMainContent(DataSnapshot dataSnapshot) {
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.new_equip, R.id.nav_task, R.id.nav_assign, R.id.nav_logs, R.id.nav_report, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        View headerView = navigationView.getHeaderView(0);
        fullname = headerView.findViewById(R.id.nameSurname);
        email = headerView.findViewById(R.id.epasts);

        // Update UI with user details
        String name = dataSnapshot.child("Vards un uzvards").getValue(String.class);
        String userEmail = dataSnapshot.child("epasts").getValue(String.class);

        if (name != null && userEmail != null) {
            fullname.setText(name);
            email.setText(userEmail);
        } else {
            fullname.setText("Not working");
            email.setText("Not working");
        }

        // Handle visibility based on status
        String statuss = dataSnapshot.child("Statuss").getValue(String.class);
        Menu navMenu = navigationView.getMenu();

        if ("Lietotājs".equals(statuss)) {
            navMenu.findItem(R.id.new_equip).setVisible(false);
            navMenu.findItem(R.id.nav_task).setVisible(false);
            navMenu.findItem(R.id.nav_logs).setVisible(false);
            navMenu.findItem(R.id.nav_assign).setVisible(false);
            navMenu.findItem(R.id.nav_updateuser).setVisible(false);
        } else if ("Darbinieks".equals(statuss)) {
            navMenu.findItem(R.id.new_equip).setVisible(true);
            navMenu.findItem(R.id.nav_task).setVisible(true);
            navMenu.findItem(R.id.nav_logs).setVisible(false);
            navMenu.findItem(R.id.nav_assign).setVisible(true);
            navMenu.findItem(R.id.nav_updateuser).setVisible(false);
        } else if ("Admin".equals(statuss)) {
            navMenu.findItem(R.id.new_equip).setVisible(true);
            navMenu.findItem(R.id.nav_task).setVisible(true);
            navMenu.findItem(R.id.nav_logs).setVisible(true);
            navMenu.findItem(R.id.nav_assign).setVisible(true);
            navMenu.findItem(R.id.nav_updateuser).setVisible(true);
        }

        // Hide the loading screen after content is ready
        loadingScreen.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            networkChangeReceiver = new NetworkChangeReceiver();
            registerReceiver(networkChangeReceiver, filter);
            isReceiverRegistered = true;
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (isReceiverRegistered) {
            unregisterReceiver(networkChangeReceiver);
            isReceiverRegistered = false;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
    @Override
    protected void onDestroy() {
        if (isReceiverRegistered) {  // Make sure it's only unregistered if registered
            unregisterReceiver(networkChangeReceiver);
            isReceiverRegistered = false;
        }
        super.onDestroy();
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public void scanCode(MenuItem item) {
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.volume_prompt_qr));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }
    private void addLogEntry(String data) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String fullName = dataSnapshot.child("Vards un uzvards").getValue(String.class);
                    String apraksts = "Noskanēja objektu";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference().child("Logs").child(dateTime);

                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("Priekšmeta kods", data);
                    logEntry.put("Vārds uzvārds", fullName);
                    logEntry.put("Epasts", currentUser.getEmail());
                    logEntry.put("Laiks", dateTime);
                    logEntry.put("Apraksts", apraksts);

                    logsRef.setValue(logEntry)
                            .addOnSuccessListener(aVoid -> Log.d("AddLogEntry", "Log entry added successfully"))
                            .addOnFailureListener(e -> Log.e("AddLogEntry", "Error adding log entry", e));
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
}
