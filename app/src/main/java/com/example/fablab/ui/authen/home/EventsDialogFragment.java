package com.example.fablab.ui.authen.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventsDialogFragment extends DialogFragment {

    // Konstantes un mainīgie
    private static final String ARG_EVENTS = "events";
    private List<Event> events;
    private String userStatus;
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private DatabaseReference eventsRef;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String title;

    // Statiskā metode fragmenta izveidei ar notikumu sarakstu
    public static EventsDialogFragment newInstance(List<Event> events) {
        EventsDialogFragment fragment = new EventsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENTS, new ArrayList<>(events)); // Nosūta notikumu sarakstu kā argumentu
        fragment.setArguments(args);
        return fragment;
    }

    // Fragmenta dialoga izveides metode
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_events, null);

        recyclerView = view.findViewById(R.id.recyclerViewEvents);

        // Iegūst nodotos notikumus
        assert getArguments() != null;
        events = (List<Event>) getArguments().getSerializable(ARG_EVENTS);

        // Ja nav notikumu, parāda paziņojumu
        if (events == null || events.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.no_events_availabe), Toast.LENGTH_SHORT).show();
            return builder.create();
        }

        // Dialoga izskats un pogas
        builder.setView(view)
                .setTitle(getString(R.string.events_title))
                .setPositiveButton(getString(R.string.ok_button), (dialog, id) -> dismiss());

        // Pārbauda lietotāja statusu un pēc tam uzstāda RecyclerView
        checkUserStatus();

        return builder.create();
    }

    // Pārbauda pieslēgušā lietotāja statusu no Firebase
    private void checkUserStatus() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userStatus = dataSnapshot.child("Statuss").getValue(String.class);
                Log.d("EventsDialogFragment", "User status: " + userStatus);

                setupRecyclerView(); // Iestata adapteri ar lietotāja statusu
                setupEventListener(); // Uzstāda reāllaika klausītāju, lai atjaunotu notikumu statusus
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("EventsDialogFragment", "ERROR WITH USER STATUS");
            }
        });
    }

    // Uzstāda RecyclerView un adapteri ar notikumu sarakstu
    private void setupRecyclerView() {
        if (recyclerView == null) return;

        adapter = new EventAdapter(events, userStatus, new EventAdapter.OnEventActionListener() {
            // Apstrādā pogas klikšķi: Apstiprināt
            @Override
            public void onAccept(Event event) {
                if (isEventValid(event)) {
                    updateEventStatus(event.getEventDate(), event.getUserId(), "Accepted", event.getEventId());
                } else {
                    Log.e("EventsDialogFragment", "Invalid event data: " + event);
                }
            }

            // Apstrādā pogas klikšķi: Noraidīt
            @Override
            public void onDecline(Event event) {
                if (isEventValid(event)) {
                    updateEventStatus(event.getEventDate(), event.getUserId(), "Declined", event.getEventId());
                } else {
                    Log.e("EventsDialogFragment", "Invalid event data: " + event);
                }
            }

            // Apstrādā pogas klikšķi: Pabeigt
            @Override
            public void onFinish(Event event) {
                if (isEventValid(event)) {
                    updateEventStatus(event.getEventDate(), event.getUserId(), "Finished", event.getEventId());
                } else {
                    Log.e("EventsDialogFragment", "Invalid event data: " + event);
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // Pārbauda, vai notikums ir derīgs (ne null)
    private boolean isEventValid(Event event) {
        return event != null && event.getEventDate() != null && event.getUserId() != null && event.getEventId() != null;
    }

    // Iestata reāllaika klausītājus katram notikumam, lai atjaunotu statusu
    private void setupEventListener() {
        if (events == null || events.isEmpty()) return;

        for (Event event : events) {
            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference()
                    .child("events")
                    .child(event.getEventDate())
                    .child(event.getUserId())
                    .child(event.getEventId());

            eventRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String status = dataSnapshot.child("status").getValue(String.class);
                    if (status != null) {
                        for (Event e : events) {
                            if (e.getEventId().equals(event.getEventId())) {
                                e.setStatus(status); // Atjauno statusu lokālajā sarakstā
                                break;
                            }
                        }
                        if (adapter != null) adapter.notifyDataSetChanged(); // Paziņo adapterim par izmaiņām
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("EventsDialogFragment", "ERROR WITH EVENT DATA");
                }
            });
        }
    }

    // Atjauno notikuma statusu Firebase datubāzē
    private void updateEventStatus(String eventDate, String userId, String newStatus, String eventId) {
        if (eventDate == null || userId == null || eventId == null) {
            Log.e("EventsDialogFragment", "Invalid parameters for updating event status");
            return;
        }

        executor.execute(() -> {
            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference()
                    .child("events")
                    .child(eventDate)
                    .child(userId)
                    .child(eventId);

            eventRef.child("status").setValue(newStatus).addOnCompleteListener(task -> {
                mainHandler.post(() -> {
                    if (task.isSuccessful()) {
                        // Parāda paziņojumu par veiksmīgu statusa maiņu
                        Toast.makeText(getContext(), getString(R.string.status_succ, newStatus), Toast.LENGTH_SHORT).show();
                        addLogEntry(newStatus);

                    } else {
                        Toast.makeText(getContext(), getString(R.string.status_err), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    // Pievieno ierakstu žurnālā (log), ja notikuma statuss tiek mainīts
    private void addLogEntry(String data) {
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

                // Formatē datumu un laiku
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                String dateTime = sdf.format(new Date());

                String title = "Notikuma statusa maiņa " + dateTime;
                String summary = fullName + " Nomainīja notikuma statusu uz: " + data + "'.";

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
}
