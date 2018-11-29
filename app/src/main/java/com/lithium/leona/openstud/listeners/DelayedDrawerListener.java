package com.lithium.leona.openstud.listeners;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

public class DelayedDrawerListener implements DrawerLayout.DrawerListener {
    private int item_pressed = -1;

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    public synchronized int getItemPressedAndReset() {
        int ret = item_pressed;
        item_pressed = -1;
        return ret;
    }

    public synchronized void setItemPressed(int item) {
        item_pressed = item;
    }
}
