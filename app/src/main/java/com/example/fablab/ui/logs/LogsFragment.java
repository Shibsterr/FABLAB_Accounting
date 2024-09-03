package com.example.fablab.ui.logs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class LogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private List<LogItem> logList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        logList = new ArrayList<>();
        adapter = new LogAdapter(logList);
        recyclerView.setAdapter(adapter);

        // Populate logList with data from Firebase Realtime Database
        populateLogListFromDatabase();

        return view;
    }

    private void populateLogListFromDatabase() {
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference().child("Logs");

        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    String dateTime = logSnapshot.getKey();
                    if(logSnapshot.child("Pievienošana?").getValue(Boolean.class) == null){
                        String desc = logSnapshot.child("Apraksts").getValue(String.class);
                        String fullName = logSnapshot.child("Vārds uzvārds").getValue(String.class);
                        String email = logSnapshot.child("Epasts").getValue(String.class);
                        String code = logSnapshot.child("Priekšmeta kods").getValue(String.class);

                        logList.add(new LogItem(dateTime, fullName, email, code, desc));

                    }else {
                        String itemName = logSnapshot.child("Priekšmeta nosaukums").getValue(String.class);
                        String quantity = logSnapshot.child("Daudzums").getValue(String.class);
                        boolean isAddition = logSnapshot.child("Pievienošana?").getValue(Boolean.class);
                        String fullName = logSnapshot.child("Vārds uzvārds").getValue(String.class);
                        String email = logSnapshot.child("Epasts").getValue(String.class);

                        logList.add(new LogItem(dateTime, fullName, email, itemName, quantity, isAddition));
                    }
                    }

                // Reverse the order of logList
                Collections.reverse(logList);

                // Notify the adapter that the data set has changed
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to retrieve log data", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
