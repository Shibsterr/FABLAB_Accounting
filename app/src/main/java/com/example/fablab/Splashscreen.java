package com.example.fablab;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Splashscreen extends AppCompatActivity {

    ImageView imageView;

    Animation imanim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        imageView = findViewById(R.id.fablablogo);

        imanim = AnimationUtils.loadAnimation(this, R.anim.imageanim);

        imageView.setAnimation(imanim);

        final Handler myhandler = new Handler();
        myhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Splashscreen.this, MainActivity.class));
                finish();
            }
        },2000);
    }
}