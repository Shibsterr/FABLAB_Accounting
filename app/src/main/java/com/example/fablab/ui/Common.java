package com.example.fablab.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

// Palīgklase kopīgu funkcionalitāšu glabāšanai
public class Common {

    // Pārbauda, vai ierīce ir pieslēgusies internetam
    public static boolean isConnectedToInternet(Context context) {

        // Iegūst ConnectivityManager objektu, kas ļauj piekļūt tīkla statusam
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            // Iegūst informāciju par visiem tīkla savienojumiem (WiFi, mobilais)
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null) {
                // Pārbauda katru tīkla savienojumu
                for (int i = 0; i < info.length; i++) {
                    // Ja kāds no savienojumiem ir AKTĪVS, atgriež true
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                        return true;
                }
            }
        }
        // Ja nav neviena aktīva savienojuma, atgriež false
        return false;
    }
}
