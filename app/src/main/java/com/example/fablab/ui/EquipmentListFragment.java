package com.example.fablab.ui;

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
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

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
        return view;
    }

    private void updateUI() {
        RecyclerView recyclerView = getView().findViewById(R.id.recyclerView);
        if (recyclerView != null && getContext() != null) {
            EquipmentAdapter adapter = new EquipmentAdapter(equipmentList);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else{
            Log.e("EquipmentListFragment", "RecyclerView or context is null");
        }
    }
}
