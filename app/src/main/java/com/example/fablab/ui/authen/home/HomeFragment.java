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
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
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
import java.util.TreeMap;

public class HomeFragment extends Fragment {

    private DatabaseReference databaseReference;
    private Button eventPage;
    private Button hidingButton, calbtn,stockbtn;
    private LinearLayout properLayout;
    private MaterialCalendarView calendarView;
    private List<String> stationsList;
    private List<String> descriptionsList;
    private DatabaseReference eventsDatabaseRef;
    private final Map<String, List<Event>> eventsMap = new HashMap<>();
    private String userStatus;
    private Set<Integer> addedCardViewIds = new HashSet<>();
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
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            List<Event> eventsForDay = getEventsForDay(date);

            if (eventsForDay != null && !eventsForDay.isEmpty()) {
                // Show dialog with events
                EventsDialogFragment eventsDialogFragment = EventsDialogFragment.newInstance(eventsForDay);
                eventsDialogFragment.show(getChildFragmentManager(), "eventsDialog");
            }
        });
        restoreCardViewPositions(properLayout,getContext());
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Restore positions when the fragment resumes
        restoreCardViewPositions(properLayout, getContext());
        Log.d("onResume", "Restoring CardView positions in onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save positions when the fragment pauses (user navigates away)
        saveCardViewPositions(properLayout, getContext());
        Log.d("onPause", "Saving CardView positions in onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        // Also save positions when the fragment is stopped
        saveCardViewPositions(properLayout, getContext());
        Log.d("onStop", "Saving CardView positions in onStop");
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
        Log.d("LanguageCheck", "Current language: " + language);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("DataSnapshot", "DataSnapshot content: " + dataSnapshot.toString());
                properLayout.removeAllViews(); // Clear previous views

                addedCardViewIds.clear(); // Clear the set of added CardView IDs

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String station = snapshot.child("Name").child(language).getValue(String.class);
                        String description = snapshot.child("Description").child(language).getValue(String.class);
                        Integer ID = snapshot.child("ID").getValue(Integer.class);

                        Log.d("DataSnapshot", "Station Name: " + station + ", Description: " + description + ", ID: " + ID);

                        if (station != null && description != null && ID != null && !addedCardViewIds.contains(ID)) {
                            // Create CardViews for stations
                            createCardView(getContext(), properLayout, station, description, ID);
                            addedCardViewIds.add(ID); // Add the ID to the set of added CardView IDs
                        } else {
                            Log.d("DataCheck", "Incomplete station data: " + snapshot.getKey());
                        }
                    }
                } else {
                    Log.d("EmptyDataSnapshot", "No stations found in the dataSnapshot");
                }
                // Restore CardView positions AFTER all stations are loaded
                restoreCardViewPositions(properLayout, getContext());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("HomeFragment", "Failed to load stations: " + databaseError.getMessage());
            }
        });
    }
    private String getCurrentLanguage() {
        // You can retrieve this from shared preferences or app settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPref.getString("language_preference", "en"); // Default is "en"
    }
    private void createCardView(Context context, LinearLayout parent, String title, String description, int ID) {
        if (context == null) {
            Log.e("CreateCardView", "Context is null");
            return;
        }

        Log.d("CreateCardView",
                "Creating CardView with Title: "
                        + title +
                        ", Description: " + description +
                        "" + ", ID: " + ID);

        CardView cardView = new CardView(context);

        // Set layout parameters for CardView
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 16, 16, 16); // Adjust margins as needed
        cardView.setLayoutParams(cardParams);

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

        TextView idView = new TextView(context);
        idView.setText("ID: " + ID);
        idView.setTextColor(Color.parseColor("#FF5722"));
        idView.setTextSize(14);
        idView.setTypeface(null, Typeface.BOLD);

        innerLayout.addView(imageView);
        innerLayout.addView(titleView);
        innerLayout.addView(descriptionView);
        innerLayout.addView(idView);

        cardView.addView(innerLayout);
        int uniqueId = View.generateViewId(); // Generates a unique ID
        cardView.setId(uniqueId);

        // Click listener for card view
        cardView.setOnClickListener(v -> {
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
                                    navController.navigate(R.id.equipmentListFragment, bundle);
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

        // Long click listener to start drag
        cardView.setOnLongClickListener(v -> {
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(cardView);
            ViewCompat.startDragAndDrop(cardView, null, shadowBuilder, cardView, 0);
            return true;
        });

        parent.addView(cardView);
        Log.d("CreateCardView", "CardView added to parent layout.");

        // Drag listener on the parent layout to handle drop events
        parent.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_LOCATION:
                case DragEvent.ACTION_DRAG_ENTERED:
                case DragEvent.ACTION_DRAG_ENDED:
                    return true;

                case DragEvent.ACTION_DROP:
                    View draggedView = (View) event.getLocalState();
                    int dropY = (int) event.getY();
                    View targetView = findViewUnderY(parent, dropY);

                    if (targetView != null && targetView != draggedView) {
                        swapCardViews(parent, draggedView, targetView);
                        saveCardViewPositions(parent, context);
                    }
                    return true;

                default:
                    return false;
            }
        });

        // Restore CardView positions when the layout is initialized
//        restoreCardViewPositions(parent, context);
        // Ensure uniform size and spacing
        checkCardViewSize(parent);
    }
    private void restoreCardViewPositions(LinearLayout parent, Context context) {
        if (context == null) {
            Log.e("RestoreCardViewPositions", "Context is null");
            return;
        }

        SharedPreferences sharedPref = context.getSharedPreferences("CardViewPositions", Context.MODE_PRIVATE);
        Map<Integer, Integer> cardViewPositions = new HashMap<>();

        // Collect existing CardViews and their saved positions
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof CardView) {
                CardView cardView = (CardView) child;
                int cardId = cardView.getId();
                int savedPosition = sharedPref.getInt("CardViewPosition_" + cardId, -1);
                if (savedPosition != -1) {
                    cardViewPositions.put(savedPosition, i);
                }
            }
        }

        // Sort and reorder CardViews based on saved positions
        for (Map.Entry<Integer, Integer> entry : new TreeMap<>(cardViewPositions).entrySet()) {
            int index = entry.getValue();
            View cardView = parent.getChildAt(index);
            parent.removeViewAt(index);
            parent.addView(cardView, entry.getKey());
        }

        Log.d("RestoreCardViewPositions", "CardView positions restored.");
    }
    private void saveCardViewPositions(LinearLayout parent, Context context) {
        if (context == null) {
            Log.e("SaveCardViewPositions", "Context is null");
            return;
        }

        SharedPreferences sharedPref = context.getSharedPreferences("CardViewPositions", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear(); // Clear the existing saved positions

        // Save the positions of the CardViews based on their order in the parent layout
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof CardView) {
                CardView cardView = (CardView) child;
                int cardId = cardView.getId();
                editor.putInt("CardViewPosition_" + cardId, i); // Store position by card ID
                Log.d("saveCardViewPositions", "Saving CardView position for ID: " + cardId + " at position: " + i);
            }
        }

        editor.apply(); // Apply the changes after looping through the positions
    }
    private void swapCardViews(LinearLayout parent, View view1, View view2) {
        int index1 = parent.indexOfChild(view1);
        int index2 = parent.indexOfChild(view2);

        Log.d("SwapCardViews", "Current Index: index1=" + index1 + ", index2=" + index2);

        // Ensure indices are valid
        if (index1 >= 0 && index1 < parent.getChildCount() && index2 >= 0 && index2 < parent.getChildCount()) {
            // Remove both views
            parent.removeViewAt(index1);

            // Adjust index2 if index1 < index2
            if (index1 < index2) {
                // If index1 is less than index2, the index2 has moved one position left after removal
                index2--;
            }

            // Remove the second view
            parent.removeViewAt(index2);

            // Add both views back in the swapped order
            parent.addView(view1, index2);
            parent.addView(view2, index1);

            Log.d("SwapCardViews", "Views swapped at index1=" + index1 + ", index2=" + index2);
        } else {
            Log.e("SwapCardViews", "Invalid indices: index1=" + index1 + ", index2=" + index2);
        }
    }
    private void checkCardViewSize(LinearLayout parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof CardView) {
                CardView cardView = (CardView) child;
//                Log.d("CardViewSize", "CardView " + i + " size: Width=" + cardView.getWidth() + ", Height=" + cardView.getHeight());
            }
        }
    }
    private View findViewUnderY(LinearLayout parent, int y) {
        for (int i = 0; i < parent.getChildCount()+1; i++) {
            View child = parent.getChildAt(i);
            int top = child.getTop();
            int bottom = child.getBottom();
            if (y > top && y < bottom) {
                return child;
            }
        }
        return null;
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