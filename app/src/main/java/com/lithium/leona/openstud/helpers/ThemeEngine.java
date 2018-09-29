package com.lithium.leona.openstud.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.lithium.leona.openstud.R;

public class ThemeEngine {
    private static SharedPreferences pref;

    public enum Theme {
        LIGHT(0), DARK(1), BLACK(2);
        private int value;

        Theme(int value) {
            this.value = value;
        }

        private int getValue() {
            return value;
        }
    }

    private static void setupSharedPreferences(Context context) {
        if (pref != null) return;
        pref = context.getSharedPreferences("ThemePrefs", 0); // 0 - for private mode
    }

    public static void setTheme(Context context, Theme theme) {
        setupSharedPreferences(context);
        pref.edit().putInt("Theme", theme.getValue()).commit();
    }

    private static int getThemeValue(Context context) {
        setupSharedPreferences(context);
        return pref.getInt("Theme", 0);
    }

    public static void applyExamTheme(Activity activity) {
        int themeId = getThemeValue(activity);
        switch (themeId) {
            case 0:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case 1:
                activity.setTheme(R.style.ExamDarkTheme);
        }
    }

    public static void applyProfileTheme(Activity activity) {
        int themeId = getThemeValue(activity);
        switch (themeId) {
            case 0:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case 1:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applyPaymentsTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case 1:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applySearchTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case 1:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applyAboutTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0:
                activity.setTheme(R.style.AppTheme_MaterialAboutActivityLight);
                break;
            case 1:
                activity.setTheme(R.style.AppTheme_MaterialAboutActivityDark);
                break;
        }
    }

    public static int getDialogTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0: return R.style.ThemeLightDialog;
            case 1: return R.style.ThemeDarkDialog;
            default: return R.style.ThemeLightDialog;
        }
    }

    public static int getAlertDialogTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0: return R.style.ThemeLightAlertDialog;
            case 1: return R.style.ThemeDarkDialog;
            default: return R.style.ThemeLightAlertDialog;
        }
    }


    public static void applySettingsTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0:
                activity.setTheme(R.style.PreferencesLight);
                break;
            case 1:
                activity.setTheme(R.style.PreferencesDark);
                break;
        }
    }

    public static boolean isLightTheme(Activity activity) {
        int themeId = ThemeEngine.getThemeValue(activity);
        switch (themeId) {
            case 0:
                return true;
            case 1:
                return false;
        }
        return false;
    }
}