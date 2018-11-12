package com.lithium.leona.openstud.listeners;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class ClickListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }
}
