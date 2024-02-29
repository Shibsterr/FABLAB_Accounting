package com.example.fablab;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fablab.ui.authen.RegisterUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;


public class NewEvent extends AppCompatActivity {

    EditText nodarbiba, stacija;
    Button addEvent;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_event);

        nodarbiba = findViewById(R.id.nodarbiba_text);
        stacija = findViewById(R.id.stacija_text);
        addEvent = findViewById(R.id.pieteikties);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            startActivity(new Intent(NewEvent.this, RegisterUser.class));
        }

        addEvent.setOnClickListener(v -> {
            // Write a message to the database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("message");

            myRef.setValue("hello");

        });
    }
}
