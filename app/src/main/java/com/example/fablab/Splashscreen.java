package com.example.fablab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class Splashscreen extends AppCompatActivity {

    ImageView imageView;
    Animation imanim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        imageView = findViewById(R.id.fablablogo);

        imanim = AnimationUtils.loadAnimation(this, R.anim.imageanim);

        imageView.setAnimation(imanim);

        final Handler myhandler = new Handler();
        myhandler.postDelayed(() -> {

            // Set locale based on saved language preference
            String languageCode = sharedPreferences.getString("language_preference", "en");
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Resources resources = getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            configuration.setLocale(locale);

            // Update the configuration and display metrics
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            startActivity(new Intent(Splashscreen.this, MainActivity.class));
            finish();
        }, 2000);
    }
}