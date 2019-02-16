package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.Intent;

import com.lithium.leona.openstud.activities.LauncherActivity;
import com.lithium.leona.openstud.data.InfoManager;

import androidx.fragment.app.Fragment;
import lithium.openstud.driver.core.Openstud;

public abstract class BaseDataFragment extends Fragment {
    Openstud os;

    public BaseDataFragment() {
        super();
    }

    public boolean initData() {
        Activity activity = getActivity();
        if (activity == null) return false;
        os = InfoManager.getOpenStud(activity);
        if (os == null) {
            InfoManager.clearSharedPreferences(activity);
            Intent i = new Intent(activity, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return false;
        }
        return true;
    }
}
