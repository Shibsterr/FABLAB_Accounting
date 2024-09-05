package com.example.fablab.ui.logs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fablab.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private List<LogItem> logList;
    private int currentSortOrder = 0;  // Tracks current sort order
    private TextView sortTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        logList = new ArrayList<>();
        adapter = new LogAdapter(logList);
        recyclerView.setAdapter(adapter);

        // Reference to the button and TextView
        Button sortButton = view.findViewById(R.id.sort_button);
        sortTextView = view.findViewById(R.id.sort_textview);

        // Populate logList with data from Firebase Realtime Database
        populateLogListFromDatabase();

        // Handle sorting on button click
        sortButton.setOnClickListener(v -> sortLogs());

        return view;
    }

    private void populateLogListFromDatabase() {
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference().child("Logs");

        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    String dateTime = logSnapshot.getKey();
                    if (logSnapshot.child("Pievienošana?").getValue(Boolean.class) == null) {
                        String desc = logSnapshot.child("Apraksts").getValue(String.class);
                        String fullName = logSnapshot.child("Vārds uzvārds").getValue(String.class);
                        String email = logSnapshot.child("Epasts").getValue(String.class);
                        String code = logSnapshot.child("Priekšmeta kods").getValue(String.class);

                        logList.add(new LogItem(dateTime, fullName, email, code, desc));

                    } else {
                        String itemName = logSnapshot.child("Priekšmeta nosaukums").getValue(String.class);
                        String quantity = logSnapshot.child("Daudzums").getValue(String.class);
                        boolean isAddition = logSnapshot.child("Pievienošana?").getValue(Boolean.class);
                        String fullName = logSnapshot.child("Vārds uzvārds").getValue(String.class);
                        String email = logSnapshot.child("Epasts").getValue(String.class);

                        logList.add(new LogItem(dateTime, fullName, email, itemName, quantity, isAddition));
                    }
                }

                // Sort by default (e.g., date descending)
                Collections.sort(logList, Comparator.comparing(LogItem::getDateTime).reversed());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to retrieve log data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sortLogs() {
        switch (currentSortOrder) {
            case 0:
                // Sort by name
                Collections.sort(logList, Comparator.comparing(LogItem::getUserName));
                sortTextView.setText(R.string.currently_sorted_by_name);
                Toast.makeText(getContext(), "Sorted by name", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                // Sort by most recent log (date descending)
                Collections.sort(logList, Comparator.comparing(LogItem::getDateTime).reversed());
                sortTextView.setText(R.string.currently_sorted_by_most_recent_log);
                Toast.makeText(getContext(), "Sorted by most recent log", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                // Sort by oldest log (date ascending)
                Collections.sort(logList, Comparator.comparing(LogItem::getDateTime));
                sortTextView.setText(R.string.currently_sorted_by_oldest_log);
                Toast.makeText(getContext(), "Sorted by oldest log", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                // Sort by another field, for example, "Noskanēts objekts"
                Collections.sort(logList, Comparator.comparing(LogItem::getItemName));
                sortTextView.setText(R.string.currently_sorted_by_item_name);
                Toast.makeText(getContext(), "Sorted by item name", Toast.LENGTH_SHORT).show();
                break;
        }

        // Update sort order (cycle through 4 options)
        currentSortOrder = (currentSortOrder + 1) % 4;

        // Notify the adapter to refresh the list
        adapter.notifyDataSetChanged();
    }
}
