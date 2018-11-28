package com.lithium.leona.openstud.helpers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.lithium.leona.openstud.R;

import java.util.Objects;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.drawerlayout.widget.DrawerLayout;


public class LayoutHelper {
    public static NavigationView setupNavigationDrawer(AppCompatActivity aca, DrawerLayout dl) {
        NavigationView navigationView = aca.findViewById(R.id.nav_view);
        return navigationView;
    }

    public static void setupToolbar(AppCompatActivity aca, Toolbar toolbar, int icon) {
        aca.setSupportActionBar(toolbar);
        ActionBar actionbar = aca.getSupportActionBar();
        Objects.requireNonNull(actionbar).setDisplayHomeAsUpEnabled(true);
        Drawable drawable = ResourcesCompat.getDrawable(aca.getResources(), icon, null);
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, Color.WHITE);
            actionbar.setHomeAsUpIndicator(drawable);
        }
    }


    public static Snackbar createTextSnackBar(View v, int string_id, int length) {
        Snackbar snackbar = Snackbar
                .make(v, string_id, length);
        snackbar.show();
        return snackbar;
    }

    public synchronized static void createActionSnackBar(View v, final int string_id, final int action_id, int length, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar
                .make(v, string_id, length).setAction(action_id, listener);
        snackbar.show();
    }

    public static void createCalendarNotification(Context context, int styleId) {
        AlertDialog dialog2 = new AlertDialog.Builder(new ContextThemeWrapper(context, styleId))
                .setTitle(context.getResources().getString(R.string.experimental_feature))
                .setMessage(context.getResources().getString(R.string.calendar_feature_description))
                .setPositiveButton(context.getResources().getString(R.string.ok), (dialog, which) -> {
                })
                .show();
    }

    public static void createSearchClassroomNotification(Context context, int styleId) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, styleId))
                .setTitle(context.getResources().getString(R.string.experimental_feature))
                .setMessage(context.getResources().getString(R.string.classroom_feature_description))
                .setPositiveButton(context.getResources().getString(R.string.ok), (dialog, which) -> {
                })
                .show();
    }


}
