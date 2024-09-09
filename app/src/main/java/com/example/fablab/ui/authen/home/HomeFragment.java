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
import androidx.navigation.Navigation;

import com.example.fablab.NewEvent;
import com.example.fablab.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private DatabaseReference databaseReference;
    private Button eventPage;
    private Button hidingButton,calbtn;
    private LinearLayout properLayout;
    private MaterialCalendarView calendarView;
    private List<String> stationsList;
    private List<String> descriptionsList;
    private DatabaseReference eventsDatabaseRef;
    private Map<String, Event> eventsMap = new HashMap<>();

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

        // Initialize lists
        stationsList = new ArrayList<>();
        descriptionsList = new ArrayList<>();

        // Initialize Firebase Database reference
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");

        // Set up button click listeners
        calbtn.setOnClickListener(v -> toggleCalView());
        hidingButton.setOnClickListener(v -> toggleViews()); // Updated to toggle views including calendar
        eventPage.setOnClickListener(v -> openNewEventActivity());
        view.findViewById(R.id.stock_button).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.equipmentFragment));

        // Set up calendar
        setupCalendar();

        // Load stations from Firebase
        loadStations();

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

    private void toggleCalView(){
        // Toggle calendarView
        if (calendarView.getVisibility() == View.VISIBLE) {
            calendarView.setVisibility(View.GONE);
            collapseLayout(calendarView); // Collapse calendarView
            eventPage.setVisibility(View.GONE);
        } else {
            calendarView.setVisibility(View.VISIBLE);
            expandLayout(calendarView); // Expand calendarViewe
            eventPage.setVisibility(View.VISIBLE);
        }
    }

// Collapse and expand layout functions remain the same

    private void setupCalendar() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // Display events for the selected date
            displayEventsForDate(date);
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
                    int ID = snapshot.child("ID").getValue(Integer.class);

                    stationsList.add(station);
                    descriptionsList.add(description);

                    createCardView(getContext(), properLayout, station, description, ID);
                    createSpace(getContext(), properLayout);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("HomeFragment", "Failed to load stations: " + databaseError.getMessage());
            }
        });
    }

    private void loadEvents() {
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");

        eventsDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventsMap.clear();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    String date = eventSnapshot.child("date").getValue(String.class);
                    String description = eventSnapshot.child("description").getValue(String.class);
                    String status = eventSnapshot.child("status").getValue(String.class);
                    Event event = new Event(date, description, status);
                    eventsMap.put(date, event);
                }

                markEventsOnCalendar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HomeFragment", "Failed to load events: " + error.getMessage());
            }
        });
    }

    private void markEventsOnCalendar() {
        calendarView.removeDecorators();
        for (Event event : eventsMap.values()) {
            CalendarDay calendarDay = parseDate(event.getDate());
            calendarView.addDecorator(new EventDecorator(calendarDay, event.getStatus()));
        }
    }

    private CalendarDay parseDate(String date) {
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Calendar months are 0-based
        int day = Integer.parseInt(parts[2]);
        return CalendarDay.from(year, month, day);
    }

    private void displayEventsForDate(CalendarDay date) {
        String key = date.toString();
        Event event = eventsMap.get(key);
        if (event != null) {
            // Display event details
            properLayout.removeAllViews();
            TextView eventDetails = new TextView(getContext());
            eventDetails.setText(String.format("Description: %s\nStatus: %s", event.getDescription(), event.getStatus()));
            properLayout.addView(eventDetails);
            properLayout.setVisibility(View.VISIBLE);
        } else {
            properLayout.setVisibility(View.GONE);
        }
    }

    private void createCardView(Context context, LinearLayout parent, String title, String description, int ID) {
        CardView cardView = new CardView(context);
        cardView.setCardElevation(4);

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

    private class EventDecorator implements DayViewDecorator {
        private final CalendarDay date;
        private final String status;

        public EventDecorator(CalendarDay date, String status) {
            this.date = date;
            this.status = status;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.equals(date);
        }

        @Override
        public void decorate(DayViewFacade view) {
            int color;
            switch (status) {
                case "Accepted":
                    color = 0xFF00FF00; // Green
                    break;
                case "Declined":
                    color = 0xFFFF0000; // Red
                    break;
                default:
                    color = 0xFFFFA500; // Orange for Pending
                    break;
            }
            view.addSpan(new DotSpan(10, color));
        }
    }

    private static class Event {
        private final String date;
        private final String description;
        private final String status;

        public Event(String date, String description, String status) {
            this.date = date;
            this.description = description;
            this.status = status;
        }

        public String getDate() {
            return date;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }
    }
}
