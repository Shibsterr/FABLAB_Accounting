package com.example.fablab;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

import com.example.fablab.ui.Common;

// Šī BroadcastReceiver klase reaģē uz tīkla statusa izmaiņām
public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ja nav pieejams interneta savienojums, parāda pielāgotu dialogu
        if (!Common.isConnectedToInternet(context)) {

            // Izveido AlertDialog būvētāju un pievieno pielāgotu izkārtojumu
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View layoutDialog = LayoutInflater.from(context)
                    .inflate(R.layout.activity_main_no_internet, null);
            builder.setView(layoutDialog);

            // Atrod pogu un tekstus no izkārtojuma
            AppCompatButton btnRetry = layoutDialog.findViewById(R.id.try_again_button);
            TextView headingText = layoutDialog.findViewById(R.id.no_internet_heading);
            TextView infoText = layoutDialog.findViewById(R.id.no_internet_text);

            // Izveido un parāda dialogu
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false); // Dialogs nav aizverams ar atpakaļ pogu

            // Nolīdzina tekstus un pašu dialogu uz centru
            headingText.setGravity(Gravity.CENTER);
            infoText.setGravity(Gravity.CENTER);
            dialog.getWindow().setGravity(Gravity.CENTER);

            // Kad tiek nospiesta "Mēģināt vēlreiz" poga, dialogs tiek aizvērts
            // un atkārtoti pārbaudīts tīkla savienojums
            btnRetry.setOnClickListener(v -> {
                dialog.dismiss();
                onReceive(context, intent); // Atkārtoti izsauc pārbaudi
            });
        }
    }
}
