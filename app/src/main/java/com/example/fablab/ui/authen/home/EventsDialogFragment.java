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

        events = (List<Event>) getArguments().getSerializable(ARG_EVENTS);

        if (events == null || events.isEmpty()) {
            Toast.makeText(getContext(), "No events available", Toast.LENGTH_SHORT).show();
            return builder.create();
        }

        builder.setView(view)
                .setTitle("Events")
                .setPositiveButton("OK", (dialog, id) -> dismiss());

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
                updateEventStatus(event.getEventDate(), event.getUserId(), "Accepted");
            }

            @Override
            public void onDecline(Event event) {
                updateEventStatus(event.getEventDate(), event.getUserId(), "Declined");
            }

            @Override
            public void onFinish(Event event) {
                updateEventStatus(event.getEventDate(), event.getUserId(), "Finished");
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void updateEventStatus(String eventDate, String userId, String newStatus) {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference().child("events").child(eventDate).child(userId);

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null) {
                        eventSnapshot.getRef().child("status").setValue(newStatus).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Event status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to update event status", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                }
                Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("EventsDialogFragment", "Failed to find event: " + error.getMessage());
            }
        });
    }
}