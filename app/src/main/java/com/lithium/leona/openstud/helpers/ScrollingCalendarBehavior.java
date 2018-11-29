package com.lithium.leona.openstud.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.material.appbar.AppBarLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

@SuppressWarnings("unused")
public class ScrollingCalendarBehavior extends AppBarLayout.Behavior {

    public ScrollingCalendarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        return false;/*super.onInterceptTouchEvent(parent, child, ev);*/
    }
}