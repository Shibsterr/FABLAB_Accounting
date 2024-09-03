package com.example.fablab.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EquipmentListFragment extends Fragment {

    public static String statNodeName;
    private DatabaseReference databaseReference;
    private List<Equipment> equipmentList;
    private Set<String> addedEquipmentNames;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private Button refreshButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        equipmentList = new ArrayList<>();
        addedEquipmentNames = new HashSet<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.empty_view);
        refreshButton = view.findViewById(R.id.refresh_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set up the refresh button to reload the data
        refreshButton.setOnClickListener(v -> loadData());

        // Initial data load
        loadData();

        return view;
    }

    private void loadData() {
        Bundle args = getArguments();
        if (args != null) {
            String stationNodeName = args.getString("stationNodeName");
            statNodeName = stationNodeName;

            if (stationNodeName != null) {
                databaseReference = FirebaseDatabase.getInstance().getReference().child("stations").child(stationNodeName).child("Equipment");
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        equipmentList.clear(); // Clear the list before populating it again
                        addedEquipmentNames.clear(); // Clear the set of added equipment names

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String name = snapshot.child("Nosaukums").getValue(String.class);
                            String description = snapshot.child("Description").getValue(String.class);
                            String image = snapshot.child("Attēls").getValue(String.class);

                            if (!addedEquipmentNames.contains(name)) { // Check if the equipment name is not already added
                                Equipment equipment = new Equipment(name, description, image);
                                equipmentList.add(equipment);
                                addedEquipmentNames.add(name); // Add the equipment name to the set of added names
                            }
                        }
                        updateUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("EquipmentListFragment", "Database error: " + databaseError.getMessage());
                    }
                });
            }
        }
    }

    private void updateUI() {
        if (equipmentList.isEmpty()) {
            // Show the empty view and hide the RecyclerView
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Show the RecyclerView and hide the empty view
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            EquipmentAdapter adapter = new EquipmentAdapter(equipmentList);
            recyclerView.setAdapter(adapter);
        }
    }
}
