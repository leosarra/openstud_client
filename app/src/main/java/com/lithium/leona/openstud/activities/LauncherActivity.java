package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Bundle;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;

import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bdl = getIntent().getExtras();
        new Thread(() -> {
            if (!InfoManager.getSaveFlag(getApplication()))
                InfoManager.clearSharedPreferences(getApplication());
            if (InfoManager.hasLogin(getApplication())) {
                Intent intent = new Intent(LauncherActivity.this, ExamsActivity.class);
                if (bdl != null) intent.putExtras(bdl);
                startActivity(intent);
            } else {
                Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
                if (bdl != null) intent.putExtras(bdl);
                startActivity(intent);
            }
        }).start();
    }
}
