package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Bundle;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

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
            else handleBiometrics(bdl,intent);
        } else {
            Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
            if (bdl != null) intent.putExtras(bdl);
            startActivity(intent);
        }
    }

    private void handleBiometrics(Bundle bundle, Intent intent){
        ExecutorService exe = Executors.newSingleThreadExecutor();
        BiometricPrompt prompt = new BiometricPrompt(this, exe, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) startLogin(bundle, -1);
                else if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) startLogin(bundle, -1);
                else if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT) {
                    PreferenceManager.setBiometricsEnabled(LauncherActivity.this,false);
                    intent.putExtra("error", ClientHelper.Status.NO_BIOMETRICS.getValue());
                    startActivity(intent);
                    LauncherActivity.this.finish();
                }
                else if (errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT || errorCode == BiometricPrompt.ERROR_LOCKOUT )
                    startLogin(bundle, ClientHelper.Status.LOCKOUT_BIOMETRICS.getValue());
                else if (errorCode != BiometricPrompt.ERROR_CANCELED) startLogin(bundle,-1);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(LauncherActivity.this.getResources().getString(R.string.biometric_login))
                .setNegativeButtonText(LauncherActivity.this.getResources().getString(R.string.delete_abort))
                .build();
        prompt.authenticate(promptInfo);
    }


    private void startLogin(Bundle bundle, int flags) {
        Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
        if (bundle != null) intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("error", flags);
        startActivity(intent);
        LauncherActivity.this.finish();
    }
}
