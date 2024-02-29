package com.example.fablab.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.fablab.NewEvent;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment{

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
//        View view;
//        view = inflater.inflate(R.layout.fragment_home,container,false);

        return createLayout(getContext());
    }
    private WebView mGoogleCalendarWebView;
    private DatabaseReference databaseReference;
    private Button eventPage;

    public HomeFragment() {
        // Required empty public constructor
    }
    private List<String> stationsList;
    private List<String> descriptionsList;
    private View createLayout(Context context) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        ScrollView scrollView = new ScrollView(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout);

        View topView = new View(context);

        LinearLayout properLayout = new LinearLayout(context);
        properLayout.setOrientation(LinearLayout.VERTICAL);
        properLayout.setTag("properLayout");
        properLayout.setVisibility(View.GONE);


        //Station button that hides nad unhides
        Button hidingButton = new Button(context);
        hidingButton.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.station_rounded_button));
        hidingButton.setText(getResources().getString(R.string.stations));
        hidingButton.setTypeface(null, Typeface.BOLD);
        hidingButton.setOnClickListener(v -> {
            if(properLayout.getVisibility() == View.VISIBLE){
                properLayout.setVisibility(View.GONE);
                Log.d("MainActivity", "It works (Its gone)");
            }else{
                properLayout.setVisibility(View.VISIBLE);
                Log.d("MainActivity", "It works (Its visible again)");
            }
        });

        //Calendar button

        // Initialize the Google Calendar API client
        mGoogleCalendarWebView = new WebView(getActivity());
        Button calendarbutton = new Button(context);

        calendarbutton.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.calendar_button));
        calendarbutton.setText(getResources().getString(R.string.calendar));
        calendarbutton.setTypeface(null, Typeface.BOLD);
        calendarbutton.setOnClickListener(v -> {
            loadGoogleCalendar();
        });

        // Go to a new page to create an event
        eventPage =  new Button(context);
        eventPage.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.event_page_button));
        eventPage.setText(getResources().getString(R.string.new_event));
        eventPage.setVisibility(View.GONE);
        eventPage.setOnClickListener(v -> {
            openNewEventActivity();
        });

        //Stock button
        Button stockButton = new Button(context);
        stockButton.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.stock_button));
        stockButton.setText(getResources().getString(R.string.stock));
        stockButton.setTypeface(null, Typeface.BOLD);

        // Set the icon
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.nav_home);
        // Set bounds for the icon (optional)
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        // Set the icon to the left of the text
        stockButton.setCompoundDrawablesRelative(icon, null, null, null);
        // Set padding between the icon and the text
        stockButton.setCompoundDrawablePadding(10); // Adjust as needed

        // Set padding to ensure the text stays centered
        stockButton.setPadding(0, stockButton.getPaddingTop(), 0, stockButton.getPaddingBottom());

        stockButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.equipmentFragment
                ));

        mAuth = FirebaseAuth.getInstance();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String statuss = dataSnapshot.child("Statuss").getValue(String.class);
                    if (statuss != null) {
                        switch (statuss) {
                            case "Lietotājs":
                                stockButton.setVisibility(View.GONE);
                                hidingButton.setVisibility(View.VISIBLE);
                                break;
                            case "Darbinieks":
                                stockButton.setVisibility(View.VISIBLE);
                                hidingButton.setVisibility(View.GONE);
                                break;
                            case "Admin":
                                stockButton.setVisibility(View.VISIBLE);
                                hidingButton.setVisibility(View.VISIBLE);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("MainActivity", "Error with reading statuss");
            }

        });
//------------------------------------------------------------------------------------------
        LinearLayout.LayoutParams topViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Get the WindowManager service from the Context
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // Create DisplayMetrics object to hold screen information
        DisplayMetrics displayMetrics = new DisplayMetrics();

        // Get the default display using WindowManager
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        // Calculate the desired button width based on screen width
        int screenWidth = displayMetrics.widthPixels;
        int buttonWidth = (int) (screenWidth * 0.95); // Adjust this factor as needed

        // Set the layout parameters for buttons
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                buttonWidth,
                250 // Adjust this as needed
        );

        layoutParams.setMargins(30, 20, 100, 20);

        hidingButton.setLayoutParams(layoutParams);
        stockButton.setLayoutParams(layoutParams);
        calendarbutton.setLayoutParams(layoutParams);
        eventPage.setLayoutParams(layoutParams);

        RelativeLayout.LayoutParams topLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        topLayoutParams.addRule(RelativeLayout.ABOVE, hidingButton.getId());
        topLayoutParams.setMargins(20, 100, 0, 20);
        topView.setLayoutParams(topLayoutParams);
//------------------------------------------------------------------------------------------
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.connect(hidingButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(hidingButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        constraintSet.connect(hidingButton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        constraintSet.connect(calendarbutton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(calendarbutton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        constraintSet.connect(calendarbutton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        constraintSet.connect(stockButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(stockButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        constraintSet.connect(stockButton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        constraintSet.connect(eventPage.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(eventPage.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        constraintSet.connect(eventPage.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        //------------------------------------------------------------------------------------------

        // Set the WebView's layout parameters to increase its size
        LinearLayout.LayoutParams weblayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        weblayout.weight = 1.0f;
        weblayout.height = 1500;
        mGoogleCalendarWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mGoogleCalendarWebView.setLayoutParams(weblayout);
        mGoogleCalendarWebView.setVisibility(View.GONE);
        WebSettings webSettings = mGoogleCalendarWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //------------------------------------------------------------------------------------------
        // adds everything to the layout
        linearLayout.addView(topView,topViewParams);

        linearLayout.addView(calendarbutton);
        linearLayout.addView(mGoogleCalendarWebView);
        linearLayout.addView(eventPage);

        linearLayout.addView(hidingButton);
        linearLayout.addView(properLayout);

        linearLayout.addView(stockButton);

        //String[] stations = getResources().getStringArray(R.array.station_titles);
        //String[] descriptions = getResources().getStringArray(R.array.station_descriptions);

        stationsList = new ArrayList<>();
        descriptionsList = new ArrayList<>();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("stations");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String station = snapshot.child("Name").getValue(String.class);
                    String description = snapshot.child("Description").getValue(String.class);

                    stationsList.add(station);
                    descriptionsList.add(description);

                    // Create card view for each station
                    createCardView(context, properLayout, station, description, R.drawable.nav_home);
                    createSpace(context, properLayout);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
            }
        });

        return scrollView;
    }

    public void openNewEventActivity(){
        Intent intent = new Intent(getContext(), NewEvent.class);
        startActivity(intent);
    }

    private void loadGoogleCalendar() {
        if(mGoogleCalendarWebView.getVisibility() == View.VISIBLE){
            Log.d("MainActivity", "Calendar should begone");
            eventPage.setVisibility(View.GONE);
            mGoogleCalendarWebView.setVisibility(View.GONE);
        }else{
            Log.d("MainActivity", "Calendar should show");
            mGoogleCalendarWebView.setVisibility(View.VISIBLE);
            eventPage.setVisibility(View.VISIBLE);
            // Load the Google Calendar URL
            String calendarUrl = "https://calendar.google.com/calendar/embed?src=fe113008ac42b82a4f3573228e787d75cc5520fda98c20448dc35307578478c6%40group.calendar.google.com&ctz=UTC";
            mGoogleCalendarWebView.loadUrl(calendarUrl);
        }
    }
    // my calendar (squidhoss)
    //https://calendar.google.com/calendar/u/0?cid=ZmUxMTMwMDhhYzQyYjgyYTRmMzU3MzIyOGU3ODdkNzVjYzU1MjBmZGE5OGMyMDQ0OGRjMzUzMDc1Nzg0NzhjNkBncm91cC5jYWxlbmRhci5nb29nbGUuY29t

    //fablab calendar
    //https://calendar.google.com/calendar/embed?src=285df84310f927e2cd943985a590d6726dcade6efe9aad1980389ad74832e2e2%40group.calendar.google.com&ctz=Europe%2FRiga

    //Stations
    private void createCardView(Context context, LinearLayout parent, String title, String description, int drawableResId) {
        CardView cardView = new CardView(context);
        cardView.setCardElevation(4);

        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(50,50,50,50);
        innerLayout.setBackgroundResource(R.drawable.my_custom_background);

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(drawableResId);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setGravity(Gravity.TOP);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView descriptionView = new TextView(context);
        descriptionView.setText(description);
        descriptionView.setTextSize(12);

        innerLayout.addView(imageView);
        innerLayout.addView(titleView);
        innerLayout.addView(descriptionView);

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
        space.setLayoutParams(new LinearLayout.LayoutParams(10, 10, 1));
        parent.addView(space);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}