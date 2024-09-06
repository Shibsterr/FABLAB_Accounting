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

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Common.isConnectedToInternet(context)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View layoutDialog = LayoutInflater.from(context).inflate(R.layout.activity_main_no_internet, null);
            builder.setView(layoutDialog);

            AppCompatButton btnRetry = layoutDialog.findViewById(R.id.try_again_button);
            TextView headingText = layoutDialog.findViewById(R.id.no_internet_heading);
            TextView infoText = layoutDialog.findViewById(R.id.no_internet_text);

            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false);

            // Centering the text views programmatically (if needed)
            headingText.setGravity(Gravity.CENTER);
            infoText.setGravity(Gravity.CENTER);

            // Center the dialog itself
            dialog.getWindow().setGravity(Gravity.CENTER);

            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    onReceive(context, intent);
                }
            });
        }
    }
}
