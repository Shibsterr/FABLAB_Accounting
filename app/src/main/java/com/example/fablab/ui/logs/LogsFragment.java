package com.example.fablab.ui.logs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Piesaistām fragmenta izkārtojuma failu
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        // Inicializējam RecyclerView un tā adapteri
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        logList = new ArrayList<>();
        adapter = new LogAdapter(logList);
        recyclerView.setAdapter(adapter);

        // Inicializējam Spinner (izvēlni) un uzstādām tās saturu
        Spinner sortSpinner = view.findViewById(R.id.sort_spinner);

        // Izveidojam adapteri Spinner izvēlnes iespējām no resursa
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sort_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);

        // Ielādējam žurnāla datus no Firebase datubāzes
        populateLogListFromDatabase();

        // Uzstādām klausītāju izvēlnes izmaiņām (šķirošanai)
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortLogs(position); // Izsaucam šķirošanu pēc izvēlētā parametra
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nekā nedarīt, ja nekas nav izvēlēts
            }
        });

        return view;
    }

    // Funkcija, kas iegūst žurnāla ierakstus no Firebase
    private void populateLogListFromDatabase() {
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference().child("Logs");

        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                logList.clear(); // Notīram sarakstu pirms jaunu datu ielādes

                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    // Nolasām katra ieraksta datus
                    String dateTime = logSnapshot.getKey();
                    String user = logSnapshot.child("user").getValue(String.class);
                    String email = logSnapshot.child("email").getValue(String.class);
                    String title = logSnapshot.child("title").getValue(String.class);
                    String summary = logSnapshot.child("summary").getValue(String.class);

                    // Pievienojam ierakstu sarakstam
                    logList.add(new LogItem(dateTime, user, email, title, summary));
                }

                // Pēc noklusējuma šķirojam pēc jaunākā datuma
                Collections.sort(logList, Comparator.comparing(LogItem::getDateTime).reversed());
                adapter.notifyDataSetChanged(); // Paziņojam adapterim, ka dati ir atjaunoti
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Ja neizdevās nolasīt datus, parādam kļūdas ziņu
                Toast.makeText(getContext(), getString(R.string.failed_log_Data), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Funkcija, kas šķiro žurnāla ierakstus atbilstoši izvēlētajam kritērijam
    private void sortLogs(int position) {
        switch (position) {
            case 0: // Jaunākie
                Collections.sort(logList, Comparator.comparing(LogItem::getDateTime).reversed());
                break;
            case 1: // Vecākie
                Collections.sort(logList, Comparator.comparing(LogItem::getDateTime));
                break;
            case 2: // Lietotāja vārds
                Collections.sort(logList, Comparator.comparing(LogItem::getUser, String.CASE_INSENSITIVE_ORDER));
                break;
            case 3: // Virsraksts
                Collections.sort(logList, Comparator.comparing(LogItem::getTitle, String.CASE_INSENSITIVE_ORDER));
                break;
        }

        adapter.notifyDataSetChanged(); // Atjaunojam saraksta skatu
    }
}
