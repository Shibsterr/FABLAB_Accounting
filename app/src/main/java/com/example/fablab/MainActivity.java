package com.example.fablab;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private TextView fullname, email;
    private Button refreshbtn;
    private ProgressBar progbar;
    private final ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(),
            result -> {

                //If you scanned something it shows result VVVV
                if (result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Log.d("MainActivity", "Cancelled scan");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Log.d("MainActivity", "Cancelled scan due to missing camera permission");
                        Toast.makeText(MainActivity.this, "Cancelled due to missing camera permission", Toast.LENGTH_LONG).show();
                    }
                } else { //if scanned correctly then display the page
                    Log.d("MainActivity", "Scanned");
                    String data = result.getContents();
                    Bundle bundle = new Bundle();
                    bundle.putString("code", data);

                    // Create a new instance of SpecificEquipmentFragment and set the arguments
                    SpecificEquipmentFragment specificEquipmentFragment = new SpecificEquipmentFragment();
                    specificEquipmentFragment.setArguments(bundle);

                    // Get the NavController
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

                    // Navigate to the SpecificEquipmentFragment
                    navController.navigate(R.id.specificEquipmentFragment, bundle);

                    Log.d("MainActivity", "Scanned data: " + data);
                    Toast.makeText(MainActivity.this, "Scanned: " + data, Toast.LENGTH_LONG).show();

                    addLogEntry(data);
                }
            });

    private void addLogEntry(String data) {
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
                    String apraksts = "Noskanēja objektu";
                    // Get current date and time
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    // Create a new log entry with the current date and time as the node name
                    DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference().child("Logs").child(dateTime);

                    // Create a map with the log entry data
                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("Priekšmeta kods", data);
                    logEntry.put("Vārds uzvārds", fullName); // Use the user's full name
                    logEntry.put("Epasts", currentUser.getEmail());
                    logEntry.put("Laiks", dateTime);
                    logEntry.put("Apraksts", apraksts);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Retrieve the selected theme from shared preferences and set it
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        super.onCreate(savedInstanceState);

        // Rest of your onCreate code
        if (isNetworkAvailable()) {       //if connection is true then there is a connection
            Log.d("MainActivity", "Its true you have net");
            setContentView(R.layout.fragment_home);
        } else { //else
            Log.d("MainActivity", "Its false no net");
            // Inflate layout without internet connection
            setContentView(R.layout.activity_main_no_internet); //no

            refreshbtn = findViewById(R.id.try_again_button);
            progbar = findViewById(R.id.progressBar);

            progbar.setVisibility(View.GONE);
            refreshbtn.setVisibility(View.VISIBLE);

            refreshbtn.setOnClickListener(v -> {
                progbar.setVisibility(View.VISIBLE);
                refreshbtn.setVisibility(View.GONE);
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                finish();
            });
        }

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, RegisterUser.class));
        } else {
            ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setSupportActionBar(binding.appBarMain.toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);        //hides the app name (at the toolbar next to the logo)

            DrawerLayout drawer = binding.drawerLayout;
            NavigationView navigationView = binding.navView;
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.new_equip, R.id.nav_task,R.id.nav_assign,R.id.nav_logs,R.id.nav_report, R.id.nav_settings)  //Assigning tasks would be a sidebar navigation// (Admin only or both worker and admin)
                    .setOpenableLayout(drawer)
                    .build();

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            View headerView = navigationView.getHeaderView(0);

            fullname = headerView.findViewById(R.id.nameSurname);
            email = headerView.findViewById(R.id.epasts);

            mAuth = FirebaseAuth.getInstance();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("Vards un uzvards").getValue(String.class);
                        String userEmail = dataSnapshot.child("epasts").getValue(String.class);

                        if (name != null && userEmail != null) {
                            fullname.setText(name);
                            email.setText(userEmail);
                        } else {
                            fullname.setText("Not working");
                            email.setText("Not working");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("MainActivity", "ERROR WITH USER");
                }
            });

            // Hide specific items in the navigation drawer based on user's status
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String statuss = dataSnapshot.child("Statuss").getValue(String.class);
                        Menu navMenu = navigationView.getMenu();

                        // Hide or show specific menu items based on user status
                        if ("Lietotājs".equals(statuss)) { // Use .equals() for string comparison
                            navMenu.findItem(R.id.new_equip).setVisible(false); // Hide new equipment item
                            navMenu.findItem(R.id.nav_task).setVisible(false); // Hide tasks item
                            navMenu.findItem(R.id.nav_logs).setVisible(false);
                            navMenu.findItem(R.id.nav_assign).setVisible(false);
                        } else if("Darbinieks".equals(statuss)){
                            navMenu.findItem(R.id.new_equip).setVisible(true);
                            navMenu.findItem(R.id.nav_task).setVisible(true);
                            navMenu.findItem(R.id.nav_logs).setVisible(false);
                            navMenu.findItem(R.id.nav_assign).setVisible(true);
                        }else if("Admin".equals(statuss)){
                            navMenu.findItem(R.id.new_equip).setVisible(true);
                            navMenu.findItem(R.id.nav_task).setVisible(true);
                            navMenu.findItem(R.id.nav_logs).setVisible(true);
                            navMenu.findItem(R.id.nav_assign).setVisible(true);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d("MainActivity", "ERROR WITH USER");
                }
            });

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    public void scanCode(MenuItem item) {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to turn on flash");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
