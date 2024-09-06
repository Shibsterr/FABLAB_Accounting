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
import android.webkit.WebView;
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

import org.naishadhparmar.zcustomcalendar.ZCustomCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public class HomeFragment extends Fragment {

    private ZCustomCalendar customCalendar;
    private WebView mGoogleCalendarWebView;
    private DatabaseReference databaseReference;
    private Button eventPage;
    private Button hidingButton;
    private LinearLayout properLayout;
    private LinearLayout eventDetailsLayout;
    private List<String> stationsList;
    private List<String> descriptionsList;
    private DatabaseReference eventsDatabaseRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        hidingButton = view.findViewById(R.id.hiding_button);
//        mGoogleCalendarWebView = view.findViewById(R.id.google_calendar_webview);
        eventPage = view.findViewById(R.id.event_page_button);
        properLayout = view.findViewById(R.id.properLayout);
        eventDetailsLayout = view.findViewById(R.id.event_details_layout);

        // Initialize lists
        stationsList = new ArrayList<>();
        descriptionsList = new ArrayList<>();

        // Initialize Firebase Database reference
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");

        // Initialize ZCustomCalendar
        customCalendar = view.findViewById(R.id.custom_calendar);
        setupCalendar();

        // Set up button click listeners
        hidingButton.setOnClickListener(v -> toggleProperLayout());
        eventPage.setOnClickListener(v -> openNewEventActivity());
        view.findViewById(R.id.calendar_button).setOnClickListener(v -> loadGoogleCalendar());
        view.findViewById(R.id.stock_button).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.equipmentFragment));

        // Load stations from Firebase
        loadStations();

        return view;
    }

    private void setupCalendar() {
        customCalendar.setOnDateSelectedListener(date -> {
            // Display events for the selected date
            displayEventsForDate(date);
        });

        customCalendar.setOnDateClickListener(date -> {
            // Show event details if clicking on an absent day
            if (isDateAbsent(date)) {
                showEventDetails(date);
            }
        });
    }

    private boolean isDateAbsent(String date) {
        // Implement logic to check if the date is marked as absent
        return true; // Placeholder; adjust according to your data
    }

    private void displayEventsForDate(String date) {
        eventDetailsLayout.removeAllViews(); // Clear previous details

        eventsDatabaseRef.child(date).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Map<String, Object> events = (Map<String, Object>) task.getResult().getValue();
                if (events != null) {
                    for (Map.Entry<String, Object> entry : events.entrySet()) {
                        String eventDetails = entry.getValue().toString();
                        TextView eventView = new TextView(getContext());
                        eventView.setText(eventDetails);
                        eventDetailsLayout.addView(eventView);
                    }
                }
            }
        });

        // Show event details layout
        eventDetailsLayout.setVisibility(View.VISIBLE);
    }

    private void showEventDetails(String date) {
        // Implement logic to show event details for the absent date
        eventDetailsLayout.setVisibility(View.VISIBLE);
    }

    private void openNewEventActivity() {
        Intent intent = new Intent(getContext(), NewEvent.class);
        startActivity(intent);
    }

    private void loadGoogleCalendar() {
        if (mGoogleCalendarWebView.getVisibility() == View.VISIBLE) {
            Log.d("HomeFragment", "Calendar should be hidden");
            eventPage.setVisibility(View.GONE);
            mGoogleCalendarWebView.setVisibility(View.GONE);
        } else {
            Log.d("HomeFragment", "Calendar should be visible");
            mGoogleCalendarWebView.setVisibility(View.VISIBLE);
            eventPage.setVisibility(View.VISIBLE);

            // Load the public calendar URL
            String calendarUrl = "https://calendar.google.com/calendar/u/0?cid=ZmUxMTMwMDhhYzQyYjgyYTRmMzU3MzIyOGU3ODdkNzVjYzU1MjBmZGE5OGMyMDQ0OGRjMzUzMDc1Nzg0NzhjNkBncm91cC5jYWxlbmRhci5nb29nbGUuY29t";
            mGoogleCalendarWebView.loadUrl(calendarUrl);
        }
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
                Log.d("Homefragment", "Evil database not working");
            }
        });
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

    private void toggleProperLayout() {
        if (properLayout.getVisibility() == View.VISIBLE) {
            collapseLayout(properLayout);
        } else {
            properLayout.setVisibility(View.VISIBLE);
            expandLayout(properLayout);
        }
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
}
