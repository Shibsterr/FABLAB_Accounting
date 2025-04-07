package com.example.fablab.ui.authen.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.example.fablab.NewEvent;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private DatabaseReference databaseReference;
    private DatabaseReference userDatabaseReference;
    private Button eventPage;
    private Button hidingButton, calbtn, stockbtn;
    private LinearLayout properLayout;
    private MaterialCalendarView calendarView;
    private List<String> stationsList;
    private List<String> descriptionsList;
    private DatabaseReference eventsDatabaseRef;
    private final Map<String, List<Event>> eventsMap = new HashMap<>();
    private String userStatus;
    private String currentUserUid;
    private Set<Integer> addedCardViewIds = new HashSet<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        hidingButton = view.findViewById(R.id.hiding_button);
        eventPage = view.findViewById(R.id.event_page_button);
        properLayout = view.findViewById(R.id.properLayout);
        calbtn = view.findViewById(R.id.calendar_button);
        calendarView = view.findViewById(R.id.calendarView); // CalendarView reference
        stockbtn = view.findViewById(R.id.stock_button);

        // Initialize lists
        stationsList = new ArrayList<>();
        descriptionsList = new ArrayList<>();

        // Initialize Firebase Database reference
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");
        currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserUid).child("recentlyUsedStations");

        // Set up button click listeners
        calbtn.setOnClickListener(v -> toggleCalView());
        hidingButton.setOnClickListener(v -> toggleViews()); // Updated to toggle views including calendar
        eventPage.setOnClickListener(v -> openNewEventActivity());
        stockbtn.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.equipmentFragment));

        // Set up calendar
        setupCalendar();

        // Load stations from Firebase, including recently used order
        loadStations();

        // Check user status and set up the UI
        checkUserStatus();

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            List<Event> eventsForDay = getEventsForDay(date);

            if (eventsForDay != null && !eventsForDay.isEmpty()) {
                // Show dialog with events
                EventsDialogFragment eventsDialogFragment = EventsDialogFragment.newInstance(eventsForDay);
                eventsDialogFragment.show(getChildFragmentManager(), "eventsDialog");
            }
        });

        return view;
    }
    private void toggleViews() {
        // Toggle properLayout
        if (properLayout.getVisibility() == View.VISIBLE) {
            collapseLayout(properLayout);
        } else {
            properLayout.setVisibility(View.VISIBLE);
            expandLayout(properLayout);
        }
    }
    private void toggleCalView() {
        // Toggle calendarView
        if (calendarView.getVisibility() == View.VISIBLE) {
            calendarView.setVisibility(View.GONE);
            collapseLayout(calendarView); // Collapse calendarView
            eventPage.setVisibility(View.GONE);
        } else {
            calendarView.setVisibility(View.VISIBLE);
            expandLayout(calendarView); // Expand calendarView
            eventPage.setVisibility(View.VISIBLE);
        }
    }
    private void openNewEventActivity() {
        Intent intent = new Intent(getContext(), NewEvent.class);
        startActivity(intent);
    }
    private void loadStations() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("stations");

        String language = getCurrentLanguage();

        // Get recently used stations for the current user
        userDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                Map<String, Integer> recentlyUsedMap = new HashMap<>();

                if (userSnapshot.exists()) {
                    for (DataSnapshot stationSnapshot : userSnapshot.getChildren()) {
                        String stationId = stationSnapshot.getKey();
                        Integer usageCount = stationSnapshot.getValue(Integer.class);
                        recentlyUsedMap.put(stationId, usageCount);
                    }
                }

                // Now load stations from the main database
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        properLayout.removeAllViews(); // Clear previous views
                        addedCardViewIds.clear(); // Clear the set of added CardView IDs

                        if (dataSnapshot.exists()) {
                            executorService.execute(() -> {
                                List<DataSnapshot> stationSnapshots = new ArrayList<>();
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    stationSnapshots.add(snapshot);
                                }

                                // Sort stations by recently used count
                                stationSnapshots.sort((s1, s2) -> {
                                    Long id1 = s1.child("ID").getValue(Long.class);
                                    Long id2 = s2.child("ID").getValue(Long.class);
                                    Integer count1 = recentlyUsedMap.getOrDefault(id1.toString(), 0);
                                    Integer count2 = recentlyUsedMap.getOrDefault(id2.toString(), 0);
                                    return count2.compareTo(count1);
                                });

                                // Build UI on the main thread
                                requireActivity().runOnUiThread(() -> {
                                    properLayout.removeAllViews();
                                    addedCardViewIds.clear();

                                    for (DataSnapshot snapshot : stationSnapshots) {
                                        String station = snapshot.child("Name").child(language).getValue(String.class);
                                        String description = snapshot.child("Description").child(language).getValue(String.class);
                                        Integer ID = snapshot.child("ID").getValue(Integer.class);

                                        if (station != null && description != null && ID != null && !addedCardViewIds.contains(ID)) {
                                            createCardView(getContext(), properLayout, station, description, ID);
                                            addedCardViewIds.add(ID);
                                        }
                                    }
                                });
                            });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d("HomeFragment", "Failed to load stations: " + databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("HomeFragment", "Failed to load recently used stations: " + databaseError.getMessage());
            }
        });
    }
    private String getCurrentLanguage() {
        // Retrieve from shared preferences or app settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPref.getString("language_preference", "en"); // Default is "en"
    }
    private void createCardView(Context context, LinearLayout parent, String title, String description, int ID) {
        CardView cardView = new CardView(context);

        // Set layout parameters for CardView with fixed margin
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(24, 24, 24, 24); // Adding consistent margins
        cardView.setLayoutParams(cardParams);
        cardView.setBackgroundResource(R.drawable.my_custom_background); // Custom background for the CardView

        // Inner layout for card content (vertical orientation and padding)
        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(30, 30, 30, 30);
        innerLayout.setBackgroundResource(R.drawable.my_custom_background);

        // ImageView for station icon (fixed size)
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(150, 150);
        imageView.setLayoutParams(imageParams);

        // Load image from Firebase Storage based on station ID
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child("Station_Icons/" + ID + ".png");

        // Loading image from Firebase with success/failure handling
        storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imageView.setImageBitmap(bitmap);
        }).addOnFailureListener(exception -> {
            Log.e("FirebaseStorageError", "Failed to load image: " + exception.getMessage());
            imageView.setImageResource(R.drawable.placeholder_image); // Placeholder image in case of failure
        });

        // TextView for the title of the station
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setTypeface(null, Typeface.BOLD);

        // TextView for station description
        TextView descriptionView = new TextView(context);
        descriptionView.setText(description);
        descriptionView.setTextSize(12);

        // TextView for displaying station ID
        TextView idView = new TextView(context);
        idView.setText("ID: " + ID);
        idView.setTextColor(Color.parseColor("#FF5722")); // Using color for emphasis on ID
        idView.setTextSize(14);
        idView.setTypeface(null, Typeface.BOLD);

        // Add views to the inner layout
        innerLayout.addView(imageView);
        innerLayout.addView(titleView);
        innerLayout.addView(descriptionView);
        innerLayout.addView(idView);

        cardView.addView(innerLayout); // Adding inner layout to CardView
        parent.addView(cardView); // Adding CardView to the parent layout

        // Setting up click listener for each CardView
        cardView.setOnClickListener(v -> {
            // Update recently used station count by station ID
            updateRecentlyUsedStation(ID);

            NavController navController = Navigation.findNavController(v);

            if (title != null && !title.isEmpty()) {
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                                String stationName = stationSnapshot.child("Name").child(getCurrentLanguage()).getValue(String.class);
                                if (stationName != null && stationName.equals(title)) {
                                    String stationNodeName = stationSnapshot.getKey();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("stationNodeName", stationNodeName);
                                    navController.navigate(R.id.equipmentListFragment, bundle); // Navigate to equipment list
                                    return;
                                }
                            }
                            Log.d("StationNotFound", "No matching station found for title: " + title);
                        } else {
                            Log.d("EmptyDataSnapshot", "No data available in the dataSnapshot");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DatabaseError", "Error: " + error.getMessage());
                    }
                });
            } else {
                Log.d("InvalidTitle", "Invalid title: " + title);
            }
        });
    }
    private void updateRecentlyUsedStation(int stationId) {
        userDatabaseReference.child(String.valueOf(stationId)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int usageCount = 1;
                if (dataSnapshot.exists()) {
                    usageCount = dataSnapshot.getValue(Integer.class) + 1;
                }
                userDatabaseReference.child(String.valueOf(stationId)).setValue(usageCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("HomeFragment", "Failed to update recently used station count: " + databaseError.getMessage());
            }
        });
    }
    private void collapseLayout(final View view) {
        final int initialHeight = view.getMeasuredHeight();
        ValueAnimator heightAnimator = ValueAnimator.ofInt(initialHeight, 0);
        heightAnimator.setDuration(300);
        heightAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = animatedValue;
            view.setLayoutParams(layoutParams);
        });
        heightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        });
        heightAnimator.start();
    }
    private void expandLayout(final View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(((ViewGroup) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int targetHeight = view.getMeasuredHeight();
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = 0;
        view.setLayoutParams(layoutParams);
        view.setVisibility(View.VISIBLE);
        ValueAnimator heightAnimator = ValueAnimator.ofInt(0, targetHeight);
        heightAnimator.setDuration(300);
        heightAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            layoutParams.height = animatedValue;
            view.setLayoutParams(layoutParams);
        });
        heightAnimator.start();
    }
    //---------------------------------------------------------------------------------------
    private void checkUserStatus() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userStatus = dataSnapshot.child("Statuss").getValue(String.class);
                updateButtonVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("HomeFragment", "ERROR WITH USER STATUS");
            }
        });
    }
    private void updateButtonVisibility() {
        if (userStatus != null) {
            switch (userStatus) {
                case "Admin":
                case "Darbinieks":
                    calbtn.setVisibility(View.VISIBLE);
                    stockbtn.setVisibility(View.VISIBLE);
                    hidingButton.setVisibility(View.VISIBLE);
                    break;
                case "Lietotājs":
                    calbtn.setVisibility(View.VISIBLE);
                    stockbtn.setVisibility(View.GONE);
                    hidingButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    calbtn.setVisibility(View.GONE);
                    stockbtn.setVisibility(View.GONE);
                    hidingButton.setVisibility(View.GONE);
                    break;
            }
        }
    }
    private void setupCalendar() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // Display events for the selected date
            getEventsForDay(date);
        });

        // Load events and mark calendar
        loadEvents();
    }
    private void loadEvents() {
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");
        eventsDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executorService.execute(() -> {
                    Map<String, List<Event>> tempMap = new HashMap<>();

                    for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                        String date = dateSnapshot.getKey();

                        for (DataSnapshot userSnapshot : dateSnapshot.getChildren()) {
                            String userId = userSnapshot.getKey();

                            for (DataSnapshot eventSnapshot : userSnapshot.getChildren()) {
                                String eventId = eventSnapshot.getKey();
                                String title = eventSnapshot.child("title").getValue(String.class);
                                String description = eventSnapshot.child("description").getValue(String.class);
                                String startTime = eventSnapshot.child("startTime").getValue(String.class);
                                String endTime = eventSnapshot.child("endTime").getValue(String.class);
                                String numberOfPeople = eventSnapshot.child("numberOfPeople").getValue(String.class);
                                String status = eventSnapshot.child("status").getValue(String.class);

                                if (title != null && description != null && date != null && startTime != null &&
                                        endTime != null && numberOfPeople != null && status != null && userId != null) {

                                    Event event = new Event(eventId, title, description, date, startTime, endTime, numberOfPeople, status, userId);

                                    tempMap.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
                                }
                            }
                        }
                    }

                    // Once processing is complete, update the UI on the main thread
                    requireActivity().runOnUiThread(() -> {
                        eventsMap.clear();
                        eventsMap.putAll(tempMap);
                        markEventsOnCalendar(); // safe to call on UI thread
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HomeFragment", "Failed to load events: " + error.getMessage());
            }
        });
    }

    private void markEventsOnCalendar() {
        calendarView.removeDecorators(); // Clear old decorators

        // Iterate over the map
        for (Map.Entry<String, List<Event>> entry : eventsMap.entrySet()) {
            String dateString = entry.getKey();
            List<Event> events = entry.getValue();

            // Parse the date
            CalendarDay calendarDay = parseDate(dateString);
            if (calendarDay != null) {
                // Add a decorator for each event
                for (Event event : events) {
                    calendarView.addDecorator(new EventDecorator(calendarDay, event.getStatus()));
                }
            }
        }
    }
    private CalendarDay parseDate(String date) {
        try {
            String[] parts = date.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // Calendar months are 0-based
            int day = Integer.parseInt(parts[2]);
            return CalendarDay.from(year, month, day);
        } catch (Exception e) {
            Log.e("HomeFragment", "Date parsing error: " + e.getMessage());
            return null;
        }
    }
    private List<Event> getEventsForDay(CalendarDay date) {
        // Format date as "yyyy-MM-dd" with leading zeros
        String dateKey = String.format("%04d-%02d-%02d", date.getYear(), date.getMonth() + 1, date.getDay());

        // Return the list of events for the selected day, or an empty list if none
        return eventsMap.getOrDefault(dateKey, new ArrayList<>());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdownNow();
    }

}