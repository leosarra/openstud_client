package com.lithium.leona.openstud.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.lithium.leona.openstud.R;

public class PreferenceManager {
    private static SharedPreferences pref;

    private static synchronized void setupSharedPreferences(Context context) {
        if (pref != null) return;
        pref = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean getStatsNotificationEnabled(Context context) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            return pref.getBoolean("statsNotification", true);
        }
    }


    public static void setStatsNotificationEnabled(Context context, boolean enabled) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            pref.edit().putBoolean("statsNotification", enabled).apply();
        }
    }

    public synchronized static int getLaudeValue(Context context) {
        setupSharedPreferences(context);
        int laudeValue = Integer.parseInt(pref.getString(context.getResources().getString(R.string.key_default_laude), "30"));
        if (laudeValue < 30) laudeValue = 30;
        return laudeValue;
    }
}
