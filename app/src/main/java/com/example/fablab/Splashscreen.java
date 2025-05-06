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

    ImageView imageView;       // Attēla skats priekš logo
    Animation imanim;          // Logo animācija

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Iegūst tēmas iestatījumus un uzstāda tēmu pirms skata ielādes
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("theme_preference", "Theme.FABLAB");
        int themeResourceId = getResources().getIdentifier(selectedTheme, "style", getPackageName());
        setTheme(themeResourceId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        // Iegūst attēla skatu un uzstāda animāciju
        imageView = findViewById(R.id.fablablogo);
        imanim = AnimationUtils.loadAnimation(this, R.anim.imageanim);
        imageView.setAnimation(imanim);

        // Izveido `Handler`, lai palaistu kodu pēc 2 sekundēm (Splash screen ilgums)
        final Handler myhandler = new Handler();
        myhandler.postDelayed(() -> {

            // Uzstāda valodu pēc lietotāja izvēles
            String languageCode = sharedPreferences.getString("language_preference", "en");
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Resources resources = getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            configuration.setLocale(locale);

            // Atjauno konfigurāciju ar izvēlēto valodu
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());

            // Startē galveno aktivitāti un aizver splash screen
            startActivity(new Intent(Splashscreen.this, MainActivity.class));
            finish();
        }, 2000); // Aizture 2000 ms (2 sekundes)
    }
}
