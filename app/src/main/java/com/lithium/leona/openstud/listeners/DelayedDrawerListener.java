package com.lithium.leona.openstud.listeners;

import android.view.View;

import com.mikepenz.materialdrawer.Drawer;

import androidx.annotation.NonNull;

public class DelayedDrawerListener implements Drawer.OnDrawerListener {
    private long item_pressed = -1;

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }

    public synchronized long getItemPressedAndReset() {
        long ret = item_pressed;
        item_pressed = -1;
        return ret;
    }

    public synchronized void setItemPressed(long item) {
        item_pressed = item;
    }
}
