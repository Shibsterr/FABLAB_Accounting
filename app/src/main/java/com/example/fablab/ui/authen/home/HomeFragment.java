package com.example.fablab.ui.authen.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private DatabaseReference databaseReference;
    private Button eventPage;
    private Button hidingButton, calbtn,stockbtn;
    private LinearLayout properLayout;
    private MaterialCalendarView calendarView;
    private List<String> stationsList;
    private List<String> descriptionsList;
    private DatabaseReference eventsDatabaseRef;
    private Map<String, List<Event>> eventsMap = new HashMap<>();
    private String userStatus;

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

        // Set up button click listeners
        calbtn.setOnClickListener(v -> toggleCalView());
        hidingButton.setOnClickListener(v -> toggleViews()); // Updated to toggle views including calendar
        eventPage.setOnClickListener(v -> openNewEventActivity());
        stockbtn.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.equipmentFragment));

        // Set up calendar
        setupCalendar();

        // Load stations from Firebase
        loadStations();

        // Check user status and set up the UI
        checkUserStatus();
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                List<Event> eventsForDay = getEventsForDay(date);

                if (eventsForDay != null && !eventsForDay.isEmpty()) {
                    // Show dialog with events
                    EventsDialogFragment eventsDialogFragment = EventsDialogFragment.newInstance(eventsForDay);
                    eventsDialogFragment.show(getChildFragmentManager(), "eventsDialog");
                }
            }
        });

        return view;
    }

    // Method to toggle both properLayout and calendarView visibility
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

    // Collapse and expand layout functions remain the same

    private void setupCalendar() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // Display events for the selected date
            getEventsForDay(date);
        });

        // Load events and mark calendar
        loadEvents();
    }

    private void openNewEventActivity() {
        Intent intent = new Intent(getContext(), NewEvent.class);
        startActivity(intent);
    }

    private void loadStations() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("stations");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                properLayout.removeAllViews(); // Clear previous views

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String station = snapshot.child("Name").getValue(String.class);
                    String description = snapshot.child("Description").getValue(String.class);
                    Integer ID = snapshot.child("ID").getValue(Integer.class);

                    if (station != null && description != null && ID != null) {
                        stationsList.add(station);
                        descriptionsList.add(description);

                        createCardView(getContext(), properLayout, station, description, ID);
                        createSpace(getContext(), properLayout);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("HomeFragment", "Failed to load stations: " + databaseError.getMessage());
            }
        });
    }

    private void createCardView(Context context, LinearLayout parent, String title, String description, int ID) {
        CardView cardView = new CardView(context);

        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(50, 50, 50, 50);
        innerLayout.setBackgroundResource(R.drawable.my_custom_background);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));

        // Load image from Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child("Station_Icons/" + ID + ".png");

        storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imageView.setImageBitmap(bitmap);
        }).addOnFailureListener(exception -> {
            Log.e("FirebaseStorageError", "Failed to load image: " + exception.getMessage());
            imageView.setImageResource(R.drawable.placeholder_image);
        });

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setTypeface(null, Typeface.BOLD);

        TextView descriptionView = new TextView(context);
        descriptionView.setText(description);
        descriptionView.setTextSize(12);

        innerLayout.addView(imageView);
        innerLayout.addView(titleView);
        innerLayout.addView(descriptionView);

        // Add to parent layout
        cardView.addView(innerLayout);

        cardView.setOnClickListener(v -> {
            // Retrieve the NavController associated with the activity
            NavController navController = Navigation.findNavController(v);

            // Check if the title is not null or empty
            if (title != null && !title.isEmpty()) {
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Check if the dataSnapshot is not empty
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                                // Retrieve the name of the station
                                String stationName = stationSnapshot.child("Name").getValue(String.class);

                                // Check if the name of the station matches the title of the clicked card view
                                if (stationName != null && stationName.equals(title)) {
                                    // If a match is found, retrieve the name of the node (station) where the data is located
                                    String stationNodeName = stationSnapshot.getKey();

                                    // Navigate to the EquipmentListFragment with station node name as argument
                                    Bundle bundle = new Bundle();
                                    bundle.putString("stationNodeName", stationNodeName);
                                    navController.navigate(R.id.equipmentListFragment, bundle);
                                    return; // Exit the loop once a match is found
                                }
                            }
                            // Handle the case where no matching station is found
                            Log.d("StationNotFound", "No matching station found for title: " + title);
                        } else {
                            // Handle the case where no data is available in the dataSnapshot
                            Log.d("EmptyDataSnapshot", "No data available in the dataSnapshot");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle database error
                        Log.e("DatabaseError", "Error: " + error.getMessage());
                    }
                });
            } else {
                // Handle the case where title is null or empty
                Log.d("InvalidTitle", "Invalid title: " + title);
            }
        });
        parent.addView(cardView);
    }

    private void createSpace(Context context, LinearLayout parent) {
        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(30, 30));
        parent.addView(space);
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
                    calbtn.setVisibility(View.VISIBLE);
                    stockbtn.setVisibility(View.VISIBLE);
                    hidingButton.setVisibility(View.VISIBLE);
                    break;
                case "Darbinieks":
                    calbtn.setVisibility(View.VISIBLE);
                    stockbtn.setVisibility(View.VISIBLE);
                    hidingButton.setVisibility(View.GONE);
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



    private void loadEvents() {
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");
        eventsDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventsMap.clear(); // Clear existing events before loading new ones

                // Iterate through each date node
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey(); // Date node key

                    // Iterate through each user UID node under the date node
                    for (DataSnapshot userSnapshot : dateSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey(); // User UID

                        // Iterate through each event under the user UID node
                        for (DataSnapshot eventSnapshot : userSnapshot.getChildren()) {
                            String eventId = eventSnapshot.getKey(); // Event ID
                            String title = eventSnapshot.child("title").getValue(String.class);
                            String description = eventSnapshot.child("description").getValue(String.class);
                            String startTime = eventSnapshot.child("startTime").getValue(String.class);
                            String endTime = eventSnapshot.child("endTime").getValue(String.class);
                            String numberOfPeople = eventSnapshot.child("numberOfPeople").getValue(String.class);
                            String status = eventSnapshot.child("status").getValue(String.class);

                            if (title != null && description != null && date != null && startTime != null && endTime != null && numberOfPeople != null && status != null && userId != null) {
                                // Create the Event object with the new structure
                                Event event = new Event(eventId, title, description, date, startTime, endTime, numberOfPeople, status, userId);

                                // Add the event to the list of events for this date
                                if (!eventsMap.containsKey(date)) {
                                    eventsMap.put(date, new ArrayList<>());
                                }
                                eventsMap.get(date).add(event);
                            }
                        }
                    }
                }

                markEventsOnCalendar(); // Mark the events on the calendar
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
        // Format date as "yyyy-MM-dd"
        String dateKey = date.getYear() + "-" + (date.getMonth() + 1) + "-" + date.getDay();

        // Return the list of events for the selected day, or an empty list if none
        return eventsMap.getOrDefault(dateKey, new ArrayList<>());
    }


}
