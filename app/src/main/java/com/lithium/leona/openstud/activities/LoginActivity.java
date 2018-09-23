package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;


import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;
import lithium.openstud.driver.exceptions.OpenstudUserNotEnabledException;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.studentIdLogin) EditText username;
    @BindView(R.id.passwordLogin) EditText password;
    @BindView(R.id.coordinatorLayout) CoordinatorLayout layout;
    @BindView(R.id.button) Button btn;
    @BindView(R.id.rememberFlag) CheckBox rememberFlag;
    @OnFocusChange({R.id.studentIdLogin,R.id.passwordLogin}) void onFocusChanged(View v, boolean focused) {
        if (!focused) ClientHelper.hideKeyboard(v, getApplication());
    }
    @OnClick(R.id.button) void onClick(View v){
        //requestInternetPermission();
        if (!ClientHelper.isNetworkAvailable(getApplication())) {
            ClientHelper.createTextSnackBar(layout, R.string.device_no_internet, Snackbar.LENGTH_LONG);
            btn.setEnabled(true);
            rememberFlag.setEnabled(true);
            username.setEnabled(true);
            password.setEnabled(true);
            return;
        }
        login();
    }
    private Openstud os;
    private LoginEventHandler h = new LoginEventHandler(this);

    private static class LoginEventHandler extends Handler {
        private final WeakReference<LoginActivity> mActivity;

        public LoginEventHandler(LoginActivity activity) {
            mActivity = new WeakReference<LoginActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LoginActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ClientHelper.Status.OK.getValue()) {
                    Intent intent = new Intent(activity,ExamsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                }
                else if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    activity.createLoginSnackBar(R.string.connection_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    activity.createLoginSnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue()) {
                    activity.createTextSnackBar(R.string.invalid_password_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == (ClientHelper.Status.EXPIRED_CREDENTIALS).getValue()) {
                    activity.createTextSnackBar(R.string.expired_password_error, Snackbar.LENGTH_LONG);
                }
                if (msg.what != ClientHelper.Status.OK.getValue()) {
                    activity.btn.setEnabled(true);
                    activity.rememberFlag.setEnabled(true);
                    activity.username.setEnabled(true);
                    activity.password.setEnabled(true);
                }
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    private void login() {
        btn.setEnabled(false);
        rememberFlag.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
        String username = this.username.getText().toString();
        final String password = this.password.getText().toString();
        if (username.isEmpty()) {
            if (password.isEmpty())
                ClientHelper.createTextSnackBar(layout, R.string.blank_username_password_error, Snackbar.LENGTH_LONG);
            else
                ClientHelper.createTextSnackBar(layout, R.string.blank_username_error, Snackbar.LENGTH_LONG);
            return;
        }
        if (password.isEmpty()) {
            ClientHelper.createTextSnackBar(layout, R.string.blank_password_error, Snackbar.LENGTH_LONG);
            return;
        }
        final int id;
        try {
            id = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            ClientHelper.createTextSnackBar(layout, R.string.invalid_password_error, Snackbar.LENGTH_LONG);
            e.printStackTrace();
            return;
        }
        os = InfoManager.getOpenStud(getApplication(), id, password);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _login(os, id, password, rememberFlag.isChecked());
            }
        }).start();
    }

    private synchronized void _login(Openstud os, int id, String password, boolean rememberFlag){
        if (os == null) {
            h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
            return;
        }
        try {
            os.login();
            InfoManager.saveOpenStud(this,os,id,password,rememberFlag);
            InfoManager.getInfoStudent(this, os);
            InfoManager.getIsee(this,os);
            h.sendEmptyMessage(ClientHelper.Status.OK.getValue());
        } catch (OpenstudInvalidCredentialsException e) {
            if (e.isPasswordExpired()) h.sendEmptyMessage(ClientHelper.Status.EXPIRED_CREDENTIALS.getValue());
            else h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
            e.printStackTrace();
        } catch (OpenstudUserNotEnabledException e) {
            h.sendEmptyMessage(ClientHelper.Status.USER_NOT_ENABLED.getValue());
            e.printStackTrace();
        } catch (OpenstudConnectionException e) {
            h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidResponseException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
            e.printStackTrace();
        }
    }

    private void createLoginSnackBar(int string_id, int length) {
        Snackbar snackbar = Snackbar
                .make(layout, getResources().getString(string_id), length).setAction(R.string.retry,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                login();
                            }
                        });
        snackbar.show();
    }

    private void createTextSnackBar(int string_id, int length) {
        ClientHelper.createTextSnackBar(layout,string_id,length);
    }


}