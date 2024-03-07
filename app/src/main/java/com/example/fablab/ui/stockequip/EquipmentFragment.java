package com.example.fablab.ui.stockequip;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fablab.R;
import com.example.fablab.ui.Equipment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EquipmentFragment extends Fragment {
    private List<Equipment> equipmentList;
    private DatabaseReference databaseReference;
    private RecyclerView recyclerView;
    private AllEquipmentAdapter adapter;
    private Set<String> addedEquipmentNames;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("stations");
        equipmentList = new ArrayList<>();
        addedEquipmentNames = new HashSet<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AllEquipmentAdapter(equipmentList);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        DatabaseReference stationsRef = databaseReference;
        stationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                equipmentList.clear(); // Clear the list before populating it again
                addedEquipmentNames.clear(); // Clear the set of added equipment names

                for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot equipmentSnapshot = stationSnapshot.child("Equipment");
                    for (DataSnapshot snapshot : equipmentSnapshot.getChildren()) {
                        String name = snapshot.child("Nosaukums").getValue(String.class);
                        String description = snapshot.child("Description").getValue(String.class);
                        String image = snapshot.child("Attēls").getValue(String.class);

                        if (!addedEquipmentNames.contains(name)) { // Check if the equipment name is not already added
                            Equipment equipment = new Equipment(name, description, image);
                            equipmentList.add(equipment);
                            addedEquipmentNames.add(name); // Add the equipment name to the set of added names
                        }
                    }
                }
                updateUI();
                Log.d("EquipmentFragment", "Data set changed, total items: " + equipmentList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("EquipmentFragment", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void updateUI() {
        if (getContext() != null) {
            adapter.notifyDataSetChanged(); // Notify adapter of data set change
        } else {
            Log.e("EquipmentFragment", "Context is null");
        }
    }
}
