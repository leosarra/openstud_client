package com.lithium.leona.openstud.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;

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

        public static Theme getTheme(int theme) {
            if (theme == 0) return LIGHT;
            else if (theme == 1) return DARK;
            else return LIGHT;
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

    public static Theme getTheme(Context context) {
        setupSharedPreferences(context);
        return ThemeEngine.Theme.getTheme(pref.getInt("Theme", 0));
    }

    public static void applyExamTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.ExamDarkTheme);
        }
    }

    public static void applyProfileTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applyPaymentsTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applySearchTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applyStatsTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static void applySearchClassroomTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }



    public static int getPrimaryTextColor(Activity activity) {
        int tintColor;
        TypedValue tV = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        boolean success = theme.resolveAttribute(R.attr.primaryTextColor, tV, true);
        if (success) tintColor = tV.data;
        else tintColor = ContextCompat.getColor(activity, android.R.color.white);
        return tintColor;
    }

    public static int getSecondaryTextColor(Activity activity) {
        int tintColor;
        TypedValue tV = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        boolean success = theme.resolveAttribute(R.attr.secondaryTextColor, tV, true);
        if (success) tintColor = tV.data;
        else tintColor = ContextCompat.getColor(activity, android.R.color.darker_gray);
        return tintColor;
    }

    public static void applyAboutTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.AppTheme_MaterialAboutActivityLight);
                break;
            case DARK:
                activity.setTheme(R.style.AppTheme_MaterialAboutActivityDark);
                break;
        }
    }

    public static void applyCalendarTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.NoActionBarAppLightTheme);
                break;
            case DARK:
                activity.setTheme(R.style.NoActionBarAppDarkTheme);
                break;
        }
    }

    public static int getDialogTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                return R.style.ThemeLightDialog;
            case DARK:
                return R.style.ThemeDarkDialog;
            default:
                return R.style.ThemeLightDialog;
        }
    }

    public static int getAlertDialogTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                return R.style.ThemeLightAlertDialog;
            case DARK:
                return R.style.ThemeDarkAlertDialog;
            default:
                return R.style.ThemeLightAlertDialog;
        }
    }


    public static void applySettingsTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                activity.setTheme(R.style.PreferencesLight);
                break;
            case DARK:
                activity.setTheme(R.style.PreferencesDark);
                break;
        }
    }

    public static int getAboutTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                return R.style.AboutLibrariesThemeLight;
            case DARK:
                return R.style.AboutLibrariesThemeDark;
            default:
                return R.style.AboutLibrariesThemeLight;
        }
    }

    private static int getPrimaryTextColor(Context context) {
        int tintColor;
        TypedValue tV = new TypedValue();
        Resources.Theme theme = context.getTheme();
        boolean success = theme.resolveAttribute(R.attr.primaryTextColor, tV, true);
        if (success) tintColor = tV.data;
        else tintColor = ContextCompat.getColor(context, android.R.color.black);
        return tintColor;
    }

    public static boolean isLightTheme(Activity activity) {
        Theme theme = getTheme(activity);
        switch (theme) {
            case LIGHT:
                return true;
            case DARK:
                return false;
        }
        return false;
    }
}