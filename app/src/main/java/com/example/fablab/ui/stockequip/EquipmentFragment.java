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
    // Saraksts, kurā tiks glabāti visi aprīkojuma objekti
    private List<Equipment> equipmentList;
    // Firebase atsauce uz "stations" mezglu
    private DatabaseReference databaseReference;
    // RecyclerView, kas parāda aprīkojumu sarakstu
    private RecyclerView recyclerView;
    // Adapteris, kas sasaista datus ar RecyclerView
    private AllEquipmentAdapter adapter;
    // Komplekts, lai sekotu unikālajiem aprīkojuma nosaukumiem (lai nerādītu dublikātus)
    private Set<String> addedEquipmentNames;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Iegūst Firebase atsauci uz "stations"
        databaseReference = FirebaseDatabase.getInstance().getReference().child("stations");

        // Inicializē tukšus sarakstus un komplektu
        equipmentList = new ArrayList<>();
        addedEquipmentNames = new HashSet<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Uzpūš fragmenta izkārtojumu no XML
        View view = inflater.inflate(R.layout.fragment_equipment, container, false);

        // Inicializē RecyclerView un uzstāda LayoutManager
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializē adapteri ar tukšu sarakstu un pievieno to RecyclerView
        adapter = new AllEquipmentAdapter(equipmentList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Iegūst atsauci uz "stations" datiem vienreizējai nolasīšanai
        DatabaseReference stationsRef = databaseReference;

        stationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Notīra iepriekšējo sarakstu un nosaukumu komplektu
                equipmentList.clear();
                addedEquipmentNames.clear();

                // Iterē cauri katrai stacijai
                for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                    // Iegūst "Equipment" mezglu no katras stacijas
                    DataSnapshot equipmentSnapshot = stationSnapshot.child("Equipment");

                    // Iterē cauri katram aprīkojuma ierakstam
                    for (DataSnapshot snapshot : equipmentSnapshot.getChildren()) {
                        // Nolasīt aprīkojuma laukus no Firebase
                        String name = snapshot.child("Nosaukums").getValue(String.class);
                        String description = snapshot.child("Description").getValue(String.class);
                        String image = snapshot.child("Attēls").getValue(String.class);

                        // Pievieno tikai unikālus nosaukumus, lai izvairītos no dublikātiem
                        if (!addedEquipmentNames.contains(name)) {
                            Equipment equipment = new Equipment(name, description, image);
                            equipmentList.add(equipment);
                            addedEquipmentNames.add(name); // Atzīmē nosaukumu kā pievienotu
                        }
                    }
                }

                // Atjaunina lietotāja saskarni
                updateUI();

                Log.d("EquipmentFragment", "Data set changed, total items: " + equipmentList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Apstrādā kļūdas gadījumā
                Log.e("EquipmentFragment", "Database error: " + databaseError.getMessage());
            }
        });
    }

    // Atjauno adapteri, lai parādītu jaunākos datus
    private void updateUI() {
        if (getContext() != null) {
            adapter.notifyDataSetChanged(); // Informē adapteri, ka dati ir mainīti
        } else {
            Log.e("EquipmentFragment", "Context is null"); // Drošības pārbaude
        }
    }
}
