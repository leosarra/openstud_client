package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;

public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        Bundle bdl = getIntent().getExtras();
        if (!InfoManager.getSaveFlag(getApplication()))
            InfoManager.clearSharedPreferences(getApplication());
        if (InfoManager.hasLogin(getApplication())) {
            Intent intent = new Intent(LauncherActivity.this, ExamsActivity.class);
            if (bdl != null) intent.putExtras(bdl);
            if (!PreferenceManager.isBiometricsEnabled(this)) startActivity(intent);
            else startLogin(bdl);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
            if (bdl != null) intent.putExtras(bdl);
            startActivity(intent);
        }
    }


    private void startLogin(Bundle bundle) {
        Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
        if (bundle != null) intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        LauncherActivity.this.finish();
    }
}
