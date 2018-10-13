package com.lithium.leona.openstud.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.AvaiableReservationsAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import org.apache.commons.lang3.tuple.Pair;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.ExamDoable;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;


public class SearchResultActivity extends AppCompatActivity {
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView rv;
    @BindView(R.id.empty_layout) LinearLayout emptyView;
    @BindView(R.id.empty_button_reload) Button emptyButton;
    @BindView(R.id.empty_text) TextView emptyText;
    @BindView(R.id.searchLayout) ConstraintLayout layout;
    @OnClick(R.id.empty_button_reload) public void OnClick(View v){
        refreshAvaiableReservations();
    }

    private Openstud os;
    private AvaiableReservationsAdapter adapter;
    private LocalDateTime lastUpdate;
    private NavigationView nv;
    private Student student;
    private ExamDoable exam;
    private List<ExamReservation> reservations;
    private SearchEventHandler h = new SearchEventHandler(this);
    private List<ExamReservation> activeReservations;

    private static class SearchEventHandler extends Handler {
        private final WeakReference<SearchResultActivity> mActivity;

        public SearchEventHandler(SearchResultActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SearchResultActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    activity.createRetrySnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.INVALID_CREDENTIALS.getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    InfoManager.clearSharedPreferences(activity.getApplication());
                    Intent i = new Intent(activity, LauncherActivity.class);
                    activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    activity.finish();
                }
                else if (msg.what == ClientHelper.Status.PLACE_RESERVATION_OK.getValue()) {
                    activity.createTextSnackBar(R.string.reservation_ok, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.PLACE_RESERVATION_INVALID_RESPONSE.getValue() || msg.what == ClientHelper.Status.PLACE_RESERVATION_CONNECTION.getValue()) {
                    activity.createTextSnackBar(R.string.reservation_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.ALREADY_PLACED.getValue()) {
                    activity.createTextSnackBar(R.string.already_placed_reservation, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                    activity.createTextSnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applySearchTheme(this);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        os = InfoManager.getOpenStud(this);
        student = InfoManager.getInfoStudentCached(this, os);
        String jsonObject;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            jsonObject = extras.getString("exam",null);
            exam = new Gson().fromJson(jsonObject, ExamDoable.class);
        }
        if (os == null || student == null || exam == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(SearchResultActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        activeReservations = new LinkedList<ExamReservation>();
        List<ExamReservation> cache = InfoManager.getActiveReservationsCached(this,os);
        if (cache != null) {
            activeReservations.addAll(cache);
        }
        LayoutHelper.setupToolbar(this,toolbar, R.drawable.ic_baseline_arrow_back);
        getSupportActionBar().setTitle(R.string.search_sessions);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        reservations = new LinkedList<>();
        emptyText.setText(getResources().getString(R.string.no_sessions_found));
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        adapter = new AvaiableReservationsAdapter(this, reservations, activeReservations, this::confirmReservation);
        rv.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1,R.color.refresh2,R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(() -> refreshAvaiableReservations());
        refreshAvaiableReservations();
    }


    private boolean confirmReservation(ExamReservation res){
        try {
            Pair<Integer,String> pair = os.insertReservation(res);
            InfoManager.setReservationUpdateFlag(this,true);
            if (pair == null) {
                h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                return false;
            }
            else if (pair.getRight() == null && pair.getLeft() == -1) {
                h.sendEmptyMessage(ClientHelper.Status.ALREADY_PLACED.getValue());
                return true;
            }
            if (pair.getRight() != null) {
                Bitmap closeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_arrow_back);
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(this, R.color.redSapienza));
                builder.setCloseButtonIcon(closeIcon);
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this,Uri.parse(pair.getRight()));
            }
            else {
                refreshAvaiableReservations();
                h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_OK.getValue());
                return true;
            }
        } catch (OpenstudInvalidResponseException e) {
            e.printStackTrace();
            h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_INVALID_RESPONSE.getValue());
        } catch (OpenstudConnectionException e) {
            e.printStackTrace();
            h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_CONNECTION.getValue());
        } catch (OpenstudInvalidCredentialsException e) {
            e.printStackTrace();
            h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
        }
        return false;
    }

    private void  refreshAvaiableReservations(){
        setRefreshing(true);
        setButtonReloadStatus(false);
        Activity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ExamReservation> update = null;
                List<ExamReservation> updateActiveReservations = null;
                try {
                    update = os.getAvailableReservations(exam,student);
                    updateActiveReservations = InfoManager.getActiveReservations(activity,os);
                    if (update == null || updateActiveReservations == null) h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                    else h.sendEmptyMessage(ClientHelper.Status.OK.getValue());

                } catch (OpenstudConnectionException e) {
                    h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                    e.printStackTrace();
                } catch (OpenstudInvalidResponseException e) {
                    h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                    e.printStackTrace();
                } catch (OpenstudInvalidCredentialsException e) {
                    if (e.isPasswordExpired()) h.sendEmptyMessage(ClientHelper.Status.EXPIRED_CREDENTIALS.getValue());
                    else h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
                    e.printStackTrace();
                }

                if (update==null || updateActiveReservations==null) {
                    setRefreshing(false);
                    setButtonReloadStatus(true);
                    return;
                }
                updateTimer();
                refreshDataSet(update, updateActiveReservations);
            }
        }).start();
    }

    public synchronized void refreshDataSet(List<ExamReservation> update, List<ExamReservation> updateActiveReservations){
        boolean flag = false;
        if (update != null && !reservations.equals(update)) {
            flag = true;
            reservations.clear();
            reservations.addAll(update);
        }
        if (updateActiveReservations != null && !activeReservations.equals(updateActiveReservations)) {
            flag = true;
            activeReservations.clear();
            activeReservations.addAll(updateActiveReservations);
        }
        final boolean finalFlag = flag;
        runOnUiThread(() -> {
            if (finalFlag) adapter.notifyDataSetChanged();
            swapViews(reservations);
            swipeRefreshLayout.setRefreshing(false);
            emptyButton.setEnabled(true);
        });
    }


    private void setRefreshing(final boolean bool){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(bool);
            }
        });
    }


    private void setButtonReloadStatus(final boolean bool){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                emptyButton.setEnabled(bool);
            }
        });
    }

    private void swapViews(final List<ExamReservation> exams) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (exams.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private synchronized void updateTimer(){
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer(){
        return lastUpdate;
    }


    private void createTextSnackBar(int string_id, int length){
        ClientHelper.createTextSnackBar(layout,string_id,length);
    }
    private void createRetrySnackBar(int string_id, int length) {
        Snackbar snackbar = Snackbar
                .make(layout, getResources().getString(string_id), length).setAction(R.string.retry,
                        view -> new Thread(new Runnable() {
                            @Override
                            public void run() {
                                refreshAvaiableReservations();
                            }
                        }));
        snackbar.show();
    }

}
