package com.example.fablab;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class Splashscreen extends AppCompatActivity {

    ImageView imageView;
    private Button refreshbtn;
    private ProgressBar progbar;
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
//                if(isNetworkAvailable()){       //if connection is true then there is a connection
                    startActivity(new Intent(Splashscreen.this, MainActivity.class));
                    finish();
//                }else{ //else
//                    Log.d("MainActivity","Its false no net");
//                    // Inflate layout without internet connection
//                    setContentView(R.layout.activity_main_no_internet); //no
//
//                    refreshbtn = findViewById(R.id.try_again_button);
//                    progbar = findViewById(R.id.progressBar);
//
//                    progbar.setVisibility(View.INVISIBLE);
//                    refreshbtn.setVisibility(View.VISIBLE);
//
//                    refreshbtn.setOnClickListener(v -> {
//                        progbar.setVisibility(View.VISIBLE);
//                        refreshbtn.setVisibility(View.INVISIBLE);
//                        if(isNetworkAvailable()){
//                            startActivity(new Intent(Splashscreen.this, MainActivity.class));
//                            finish();
//                        }else{
//                            progbar.setVisibility(View.INVISIBLE);
//                            refreshbtn.setVisibility(View.VISIBLE);
//                        }
//                    });
//                }
            }
        },2000);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}