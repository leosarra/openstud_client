package com.lithium.leona.openstud.helpers;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.SettingsPrefActivity;


public class LayoutHelper {
    public static NavigationView setupNavigationDrawer(AppCompatActivity aca, DrawerLayout dl){
        NavigationView navigationView = aca.findViewById(R.id.nav_view);
        return navigationView;
    }

    public static void setupToolbar(AppCompatActivity aca, Toolbar toolbar, int icon) {
        aca.setSupportActionBar(toolbar);
        ActionBar actionbar = aca.getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        Drawable drawable = ResourcesCompat.getDrawable(aca.getResources(), icon, null);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, Color.WHITE);
        actionbar.setHomeAsUpIndicator(drawable);
    }
}
