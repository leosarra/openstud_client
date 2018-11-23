package com.lithium.leona.openstud.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lithium.leona.openstud.R;

import java.lang.reflect.Type;
import java.util.List;

public class PreferenceManager {
    private static SharedPreferences pref;

    private static synchronized void setupSharedPreferences(Context context) {
        if (pref != null) return;
        pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean getStatsNotificationEnabled(Context context) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            return pref.getBoolean("statsNotification", true);
        }
    }

    public static boolean isLessonEnabled(Context context) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            return pref.getBoolean(context.getResources().getString(R.string.key_enable_lesson), false);
        }
    }


    public static void setLessonEnabled(Context context, boolean enabled) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            pref.edit().putBoolean(context.getResources().getString(R.string.key_default_laude), enabled).apply();
        }
    }

    public static void saveSuggestions(Context context, List suggestions) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List>() {}.getType();
            String json = gson.toJson(suggestions, listType);
            pref.edit().putString("suggestion", json).apply();
        }
    }

    public static List getSuggestions(Context context) {
        setupSharedPreferences(context);
        Gson gson = new Gson();
        String json;
        synchronized (PreferenceManager.class) {
            json = pref.getString("suggestion", "null");
        }
        if (json == null) return null;
        Type listType = new TypeToken<List>() {
        }.getType();
        return gson.fromJson(json, listType);
    }


    public static void setStatsNotificationEnabled(Context context, boolean enabled) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            pref.edit().putBoolean("statsNotification", enabled).apply();
        }
    }

    public static boolean getCalendarNotificationEnabled(Context context) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            return pref.getBoolean("calendarLessonNotification", true);
        }
    }

    public static void setCalendarNotificationEnabled(Context context, boolean enabled) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            pref.edit().putBoolean("calendarLessonNotification", enabled).apply();
        }
    }

    public static boolean getClassroomNotificationEnabled(Context context) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            return pref.getBoolean("classroomNotification", true);
        }
    }

    public static void setClassroomNotificationEnabled(Context context, boolean enabled) {
        setupSharedPreferences(context);
        synchronized (PreferenceManager.class) {
            pref.edit().putBoolean("classroomNotification", enabled).apply();
        }
    }

    public synchronized static int getLaudeValue(Context context) {
        setupSharedPreferences(context);
        int laudeValue = 30;
        try {
            laudeValue = Integer.parseInt(pref.getString(context.getResources().getString(R.string.key_default_laude), "30"));
            if (laudeValue < 30 || laudeValue > 34) laudeValue = 30;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return laudeValue;
    }

    public static boolean isExamDateEnabled(Context context) {
        setupSharedPreferences(context);
        return  pref.getBoolean(context.getResources().getString(R.string.key_exam_date), false);
    }

}
