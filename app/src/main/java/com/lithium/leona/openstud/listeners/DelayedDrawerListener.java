package com.lithium.leona.openstud.listeners;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.lithium.leona.openstud.PaymentsActivity;
import com.lithium.leona.openstud.ProfileActivity;
import com.lithium.leona.openstud.R;

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

    public synchronized int getItemPressedAndReset(){
        int ret = item_pressed;
        item_pressed = -1;
        return ret;
    }

    public synchronized void setItemPressed(int item){
        item_pressed = item;
    }
}