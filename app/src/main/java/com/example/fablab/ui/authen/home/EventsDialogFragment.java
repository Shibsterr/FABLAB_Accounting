package com.example.fablab.ui.authen.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventsDialogFragment extends DialogFragment {

    private static final String ARG_EVENTS = "events";
    private List<Event> events;
    private String userStatus;
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private DatabaseReference eventsRef;

    public static EventsDialogFragment newInstance(List<Event> events) {
        EventsDialogFragment fragment = new EventsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENTS, new ArrayList<>(events)); // Send the list of events
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_events, null);

        recyclerView = view.findViewById(R.id.recyclerViewEvents);

        assert getArguments() != null;
        events = (List<Event>) getArguments().getSerializable(ARG_EVENTS);

        if (events == null || events.isEmpty()) {
            Toast.makeText(getContext(), "No events available", Toast.LENGTH_SHORT).show();
            return builder.create();
        }

        builder.setView(view)
                .setTitle(getString(R.string.events_title))
                .setPositiveButton(getString(R.string.ok_button), (dialog, id) -> dismiss());

        checkUserStatus(); // Ensure we get user status before setting up the adapter

        return builder.create();
    }

    private void checkUserStatus() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userStatus = dataSnapshot.child("Statuss").getValue(String.class);
                Log.d("EventsDialogFragment", "User status: " + userStatus);

                setupRecyclerView();
                setupEventListener(); // Set up real-time listener for event status updates
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("EventsDialogFragment", "ERROR WITH USER STATUS");
            }
        });
    }

    private void setupRecyclerView() {
        if (recyclerView == null) {
            return; // View might not be ready yet
        }

        adapter = new EventAdapter(events, userStatus, new EventAdapter.OnEventActionListener() {
            @Override
            public void onAccept(Event event) {
                if (isEventValid(event)) {
                    updateEventStatus(event.getEventDate(), event.getUserId(), "Accepted", event.getEventId());
                } else {
                    Log.e("EventsDialogFragment", "Invalid event data: " + event);
                }
            }

            @Override
            public void onDecline(Event event) {
                if (isEventValid(event)) {
                    updateEventStatus(event.getEventDate(), event.getUserId(), "Declined", event.getEventId());
                } else {
                    Log.e("EventsDialogFragment", "Invalid event data: " + event);
                }
            }

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

    private boolean isEventValid(Event event) {
        return event != null && event.getEventDate() != null && event.getUserId() != null && event.getEventId() != null;
    }

    private void setupEventListener() {
        if (events == null || events.isEmpty()) {
            return;
        }

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
                                e.setStatus(status);
                                break;
                            }
                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("EventsDialogFragment", "ERROR WITH EVENT DATA");
                }
            });
        }
    }

    private void updateEventStatus(String eventDate, String userId, String newStatus, String eventId) {
        if (eventDate == null || userId == null || eventId == null) {
            Log.e("EventsDialogFragment", "Invalid parameters for updating event status");
            return;
        }

        // Reference to the specific event by its eventId
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference()
                .child("events")
                .child(eventDate)
                .child(userId)
                .child(eventId);

        // Update the status of the specific event
        eventRef.child("status").setValue(newStatus).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.status_succ, newStatus), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), getString(R.string.status_err), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
