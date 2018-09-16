package com.lithium.leona.openstud;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.lithium.leona.openstud.data.InfoManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        if (!InfoManager.getSaveFlag(getApplication())) InfoManager.clearSharedPreferences(getApplication());
        if (InfoManager.hasLogin(getApplication())) {
            Intent intent = new Intent(LauncherActivity.this, ProfileActivity.class);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
            startActivity(intent);
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        if (!InfoManager.getSaveFlag(getApplication())) {
            InfoManager.clearSharedPreferences(getApplication());
        }
    }
}
