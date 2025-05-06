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
        // Uzpūšam fragmenta skatu no XML
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializējam skatus (pogas, kalendāru, izkārtojumu)
        hidingButton = view.findViewById(R.id.hiding_button);
        eventPage = view.findViewById(R.id.event_page_button);
        properLayout = view.findViewById(R.id.properLayout);
        calbtn = view.findViewById(R.id.calendar_button);
        calendarView = view.findViewById(R.id.calendarView);
        stockbtn = view.findViewById(R.id.stock_button);

        // Inicializējam sarakstus staciju nosaukumiem un aprakstiem
        stationsList = new ArrayList<>();
        descriptionsList = new ArrayList<>();

        // Inicializējam Firebase atsauces
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");
        currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserUid).child("recentlyUsedStations");

        // Uzstādām pogu nospiešanas klausītājus
        calbtn.setOnClickListener(v -> toggleCalView()); // Rāda/paslēpj kalendāru
        hidingButton.setOnClickListener(v -> toggleViews()); // Rāda/paslēpj properLayout
        eventPage.setOnClickListener(v -> openNewEventActivity()); // Atver jauna notikuma aktivitāti
        stockbtn.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.equipmentFragment)); // Pārvieto uz aprīkojuma fragmentu

        // Inicializējam kalendāra funkcionalitāti
        setupCalendar();

        // Ielādējam stacijas no Firebase un kārtojam pēc nesenās lietošanas
        loadStations();

        // Pārbaudām lietotāja statusu, lai pielāgotu lietotāja interfeisu
        checkUserStatus();

        // Kalendāra klikšķu klausītājs: atver dialogu ar dienas notikumiem
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            List<Event> eventsForDay = getEventsForDay(date);
            if (eventsForDay != null && !eventsForDay.isEmpty()) {
                EventsDialogFragment eventsDialogFragment = EventsDialogFragment.newInstance(eventsForDay);
                eventsDialogFragment.show(getChildFragmentManager(), "eventsDialog");
            }
        });

        return view;
    }


    private void toggleViews() {
        // Pārslēgt properlayout
        if (properLayout.getVisibility() == View.VISIBLE) {
            collapseLayout(properLayout);
        } else {
            properLayout.setVisibility(View.VISIBLE);
            expandLayout(properLayout);
        }
    }

    private void toggleCalView() {
        // Pārslēgt calendarView
        if (calendarView.getVisibility() == View.VISIBLE) {
            calendarView.setVisibility(View.GONE);
            collapseLayout(calendarView); // Sakļaut calendarView
            eventPage.setVisibility(View.GONE);
        } else {
            calendarView.setVisibility(View.VISIBLE);
            expandLayout(calendarView); // Izvērst calendarView
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

        // Iegūstiet nesen izmantotās stacijas pašreizējam lietotājam
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

                // Tagad ielādējiet stacijas no galvenās datu bāzes
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        properLayout.removeAllViews(); // Notīrīt iepriekšējos skatus
                        addedCardViewIds.clear(); // Notīriet pievienoto CardView ID kopu

                        if (dataSnapshot.exists()) {
                            List<DataSnapshot> stationSnapshots = new ArrayList<>();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                stationSnapshots.add(snapshot);
                            }

                            // Kārtot stacijas pēc nesen izmantoto skaita
                            stationSnapshots.sort((s1, s2) -> {
                                Long id1 = s1.child("ID").getValue(Long.class); // Saņemt kā Long datu tipu
                                Long id2 = s2.child("ID").getValue(Long.class);
                                Integer count1 = recentlyUsedMap.getOrDefault(id1.toString(), 0); // Pārveidojiet Long par virkni kartes atslēgai
                                Integer count2 = recentlyUsedMap.getOrDefault(id2.toString(), 0);
                                return count2.compareTo(count1); // Kārtot pēc lietojuma dilstošā secībā
                            });

                            // Pievienojiet stacijas kā CardViews sakārtotā secībā
                            for (DataSnapshot snapshot : stationSnapshots) {
                                String station = snapshot.child("Name").child(language).getValue(String.class);
                                String description = snapshot.child("Description").child(language).getValue(String.class);
                                Integer ID = snapshot.child("ID").getValue(Integer.class);

                                if (station != null && description != null && ID != null && !addedCardViewIds.contains(ID)) {
                                    createCardView(getContext(), properLayout, station, description, ID);
                                    addedCardViewIds.add(ID); // Pievienojiet ID pievienoto CardView ID kopai
                                }
                            }
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
        // Izgūt no koplietotajām preferencēm vai lietotņu iestatījumiem
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPref.getString("language_preference", "en"); // Default is "en"
    }

    private void createCardView(Context context, LinearLayout parent, String title, String description, int ID) {
        CardView cardView = new CardView(context);

        // Iestatiet CardView izkārtojuma parametrus ar fiksētu piemali
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(24, 24, 24, 24); // Pievienot cardView margins
        cardView.setLayoutParams(cardParams);
        cardView.setBackgroundResource(R.drawable.my_custom_background); // Pielāgots CardView fons (drawable)

        // Kartes satura iekšējais izkārtojums (vertikālā orientācija un polsterējums)
        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(30, 30, 30, 30);
        innerLayout.setBackgroundResource(R.drawable.my_custom_background);

        // ImageView stacijas ikonai (fiksēts izmērs)
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(150, 150);
        imageView.setLayoutParams(imageParams);

        // Ielādējiet attēlu no Firebase krātuves, pamatojoties uz stacijas ID
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child("Station_Icons/" + ID + ".png");

        // Notiek attēla ielāde no Firebase ar veiksmīgu/neveiksmīgu apstrādi
        storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imageView.setImageBitmap(bitmap);
        }).addOnFailureListener(exception -> {
            Log.e("FirebaseStorageError", "Failed to load image: " + exception.getMessage());
            imageView.setImageResource(R.drawable.placeholder_image); // Placeholder image in case of failure
        });

        // TextView stacijas nosaukumam
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setTypeface(null, Typeface.BOLD);

        // TextView stacijas aprakstam
        TextView descriptionView = new TextView(context);
        descriptionView.setText(description);
        descriptionView.setTextSize(12);

        // TextView stacijas ID parādīšanai
        TextView idView = new TextView(context);
        idView.setText("ID: " + ID);
        idView.setTextColor(Color.parseColor("#FF5722")); // Using color for emphasis on ID
        idView.setTextSize(14);
        idView.setTypeface(null, Typeface.BOLD);

        // Pievienojiet skatus iekšējam izkārtojumam
        innerLayout.addView(imageView);
        innerLayout.addView(titleView);
        innerLayout.addView(descriptionView);
        innerLayout.addView(idView);

        cardView.addView(innerLayout); // Iekšējā izkārtojuma pievienošana CardView
        parent.addView(cardView); // CardView pievienošana vecāku izkārtojumam

        // Klikšķu uztvērēja iestatīšana katram CardView
        cardView.setOnClickListener(v -> {
            // Atjauniniet nesen izmantoto staciju skaitu pēc stacijas ID
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
                                    navController.navigate(R.id.equipmentListFragment, bundle); // Pārejiet uz aprīkojuma sarakstu
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

    // Atjaunina lietotāja izmantotās stacijas lietošanas skaitu Firebase datubāzē
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

    // Samazina (sakļauj) norādīto izkārtojumu ar animāciju
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

    // Paplašina (atver) norādīto izkārtojumu ar animāciju
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

    // Pārbauda pašreizējā lietotāja statusu un atjauno pogu redzamību atbilstoši lomai
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

    // Atjauno redzamo pogu stāvokli atkarībā no lietotāja statusa
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

    // Inicializē kalendāra uzvedību: kad tiek izvēlēts datums, tiek parādīti tā notikumi
    private void setupCalendar() {
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // Parādīt notikumus atlasītajam datumam
            getEventsForDay(date);
        });

        // Ielādējiet notikumus un atzīmējiet kalendāru
        loadEvents();
    }

    // Ielādē visus notikumus no Firebase un saglabā tos atbilstoši datumiem mapē
    private void loadEvents() {
        eventsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("events");
        eventsDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventsMap.clear(); // Notīra iepriekšējos notikumus

                // Iziet cauri katram datuma mezglam
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

                            if (title != null && description != null && date != null && startTime != null && endTime != null && numberOfPeople != null && status != null && userId != null) {
                                Event event = new Event(eventId, title, description, date, startTime, endTime, numberOfPeople, status, userId);

                                if (!eventsMap.containsKey(date)) {
                                    eventsMap.put(date, new ArrayList<>());
                                }
                                eventsMap.get(date).add(event);
                            }
                        }
                    }
                }

                markEventsOnCalendar(); // Atzīmē notikumus kalendārā
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HomeFragment", "Failed to load events: " + error.getMessage());
            }
        });
    }

    // Atzīmē notikumus kalendārā izmantojot dekoratorus (piemēram, krāsainus punktus)
    private void markEventsOnCalendar() {
        calendarView.removeDecorators(); // Notīra iepriekšējos dekoratorus

        for (Map.Entry<String, List<Event>> entry : eventsMap.entrySet()) {
            String dateString = entry.getKey();
            List<Event> events = entry.getValue();

            CalendarDay calendarDay = parseDate(dateString);
            if (calendarDay != null) {
                for (Event event : events) {
                    calendarView.addDecorator(new EventDecorator(calendarDay, event.getStatus()));
                }
            }
        }
    }

    // Pārvērš datuma tekstu uz CalendarDay objektu
    private CalendarDay parseDate(String date) {
        try {
            String[] parts = date.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // Mēneši sākas no 0
            int day = Integer.parseInt(parts[2]);
            return CalendarDay.from(year, month, day);
        } catch (Exception e) {
            Log.e("HomeFragment", "Date parsing error: " + e.getMessage());
            return null;
        }
    }

    // Iegūst notikumu sarakstu konkrētai dienai
    private List<Event> getEventsForDay(CalendarDay date) {
        String dateKey = date.getYear() + "-" + (date.getMonth() + 1) + "-" + date.getDay();
        return eventsMap.getOrDefault(dateKey, new ArrayList<>());
    }

}