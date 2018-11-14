package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.fragments.BottomSheetRecoveryFragment;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidAnswerException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;
import lithium.openstud.driver.exceptions.OpenstudUserNotEnabledException;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.studentIdLogin)
    EditText username;
    @BindView(R.id.passwordLogin)
    EditText password;
    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout layout;
    @BindView(R.id.button)
    Button btn;
    @BindView(R.id.recovery)
    Button recovery;
    @BindView(R.id.rememberFlag)
    CheckBox rememberFlag;

    @OnFocusChange({R.id.studentIdLogin, R.id.passwordLogin})
    void onFocusChanged(View v, boolean focused) {
        if (!focused) ClientHelper.hideKeyboard(v, getApplication());
    }

    @OnClick(R.id.recovery)
    void onClickRecovery(View v) {
        if (!ClientHelper.isNetworkAvailable(getApplication())) {
            LayoutHelper.createTextSnackBar(layout, R.string.device_no_internet, Snackbar.LENGTH_LONG);
            recovery.setEnabled(true);
            return;
        }
        recovery();
    }

    @OnClick(R.id.button)
    void onClickLogin(View v) {
        if (!ClientHelper.isNetworkAvailable(getApplication())) {
            LayoutHelper.createTextSnackBar(layout, R.string.device_no_internet, Snackbar.LENGTH_LONG);
            runOnUiThread(() -> setElementsEnabled(true));
            return;
        }
        login();
    }

    private Openstud os;
    private LoginEventHandler h = new LoginEventHandler(this);

    private static class LoginEventHandler extends Handler {
        private final WeakReference<LoginActivity> mActivity;

        LoginEventHandler(LoginActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LoginActivity activity = mActivity.get();
            if (activity != null) {
                View.OnClickListener listener = v -> new Thread(activity::login).start();
                View.OnClickListener listener2 = v -> new Thread(activity::recovery).start();
                if (msg.what == ClientHelper.Status.OK.getValue()) {
                    Intent intent = new Intent(activity, ExamsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                } else if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.CONNECTION_ERROR_RECOVERY.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, listener2);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.invalid_response_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.user_not_enabled_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.invalid_password_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == (ClientHelper.Status.EXPIRED_CREDENTIALS).getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.expired_password_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.RECOVERY_OK.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.recovery_ok, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.INVALID_STUDENT_ID.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.invalid_student_id, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.INVALID_ANSWER.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.invalid_answer, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.NO_RECOVERY.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.no_recovery, Snackbar.LENGTH_LONG);
                }
                if (msg.what != ClientHelper.Status.OK.getValue()) {
                    activity.runOnUiThread(() -> activity.setElementsEnabled(true));
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        analyzeExtras(getIntent().getExtras());
    }

    public void sendRecoveryRequest(String answer, String studentID) {
        runOnUiThread(() -> setElementsEnabled(false));
        Openstud os = InfoManager.getOpenStudRecovery(this, studentID);
        if (os == null) return;
        try {
            os.recoverPassword(answer);
            h.sendEmptyMessage(ClientHelper.Status.RECOVERY_OK.getValue());
        } catch (OpenstudConnectionException e) {
            h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidResponseException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidCredentialsException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_STUDENT_ID.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidAnswerException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_ANSWER.getValue());
            e.printStackTrace();
        }

    }

    private void recovery() {
        runOnUiThread(() -> setElementsEnabled(false));
        String username = this.username.getText().toString();
        if (username.isEmpty()) {
            LayoutHelper.createTextSnackBar(layout, R.string.blank_username_error, Snackbar.LENGTH_LONG);
            h.sendEmptyMessage(ClientHelper.Status.FAIL_LOGIN.getValue());
            return;
        }
        os = InfoManager.getOpenStudRecovery(getApplication(), username);
        new Thread(() -> _recovery(os, username)).start();
    }

    private void _recovery(Openstud os, String username) {
        if (os == null) {
            h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
            return;
        }
        try {
            String question = os.getSecurityQuestion();
            if (question == null) {
                h.sendEmptyMessage(ClientHelper.Status.NO_RECOVERY.getValue());
                return;
            }
            BottomSheetRecoveryFragment recoveryFrag = BottomSheetRecoveryFragment.newInstance(username, question);
            recoveryFrag.show(getSupportFragmentManager(), recoveryFrag.getTag());
            new Handler(Looper.getMainLooper()).postDelayed(() -> h.sendEmptyMessage(ClientHelper.Status.ENABLE_BUTTONS.getValue()), 1000);
        } catch (OpenstudConnectionException e) {
            h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR_RECOVERY.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidResponseException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidCredentialsException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_STUDENT_ID.getValue());
            e.printStackTrace();
        }
    }

    private void login() {
        runOnUiThread(() -> setElementsEnabled(false));
        String username = this.username.getText().toString();
        final String password = this.password.getText().toString();
        if (username.isEmpty()) {
            if (password.isEmpty())
                LayoutHelper.createTextSnackBar(layout, R.string.blank_username_password_error, Snackbar.LENGTH_LONG);
            else
                LayoutHelper.createTextSnackBar(layout, R.string.blank_username_error, Snackbar.LENGTH_LONG);
            h.sendEmptyMessage(ClientHelper.Status.FAIL_LOGIN.getValue());
            return;
        }
        if (password.isEmpty()) {
            LayoutHelper.createTextSnackBar(layout, R.string.blank_password_error, Snackbar.LENGTH_LONG);
            h.sendEmptyMessage(ClientHelper.Status.FAIL_LOGIN.getValue());
            return;
        }
        os = InfoManager.getOpenStud(getApplication(), username, password);
        new Thread(() -> _login(os, username, password, rememberFlag.isChecked())).start();
    }

    private synchronized void _login(Openstud os, String id, String password, boolean rememberFlag) {
        if (os == null) {
            h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
            return;
        }
        try {
            os.login();
            InfoManager.saveOpenStud(this, os, id, password, rememberFlag);
            InfoManager.getInfoStudent(this, os);
            InfoManager.getIsee(this, os);
            h.sendEmptyMessage(ClientHelper.Status.OK.getValue());
        } catch (OpenstudInvalidCredentialsException e) {
            if (e.isPasswordExpired())
                h.sendEmptyMessage(ClientHelper.Status.EXPIRED_CREDENTIALS.getValue());
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

    private void analyzeExtras(Bundle bdl) {
        if (bdl == null) return;
        int error = bdl.getInt("error", -1);
        if (error == -1) return;
        else if (error == ClientHelper.Status.INVALID_CREDENTIALS.getValue())
            LayoutHelper.createTextSnackBar(layout, R.string.invalid_password_error, Snackbar.LENGTH_LONG);
        else if (error == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue())
            LayoutHelper.createTextSnackBar(layout, R.string.expired_password_error, Snackbar.LENGTH_LONG);
    }

    private void setElementsEnabled(boolean enabled) {
        btn.setEnabled(enabled);
        recovery.setEnabled(enabled);
        rememberFlag.setEnabled(enabled);
        username.setEnabled(enabled);
        password.setEnabled(enabled);
    }


}
