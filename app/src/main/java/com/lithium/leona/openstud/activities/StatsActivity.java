package com.lithium.leona.openstud.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.lithium.leona.openstud.AboutActivity;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.fragments.ExamsDoneFragment;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.OpenstudHelper;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class StatsActivity extends AppCompatActivity {

    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.arithmeticValue) TextView arithmeticValue;
    @BindView(R.id.weightedValue) TextView weightedValue;
    @BindView(R.id.totalCFU) TextView totalCFU;
    @BindView(R.id.graph) GraphView graph;
    @BindView(R.id.graph2) GraphView graph2;
    private DelayedDrawerListener ddl;
    private NavigationView nv;
    private Openstud os;
    private StatsHandler h = new StatsHandler(this);
    private List<ExamDone> exams = new LinkedList<>();
    private LocalDateTime lastUpdate;
    private boolean firstStart = true;

    private static class StatsHandler extends Handler {
        private final WeakReference<StatsActivity> activity;

        private StatsHandler(StatsActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final StatsActivity activity = this.activity.get();
            if (activity== null) return;
            if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                View.OnClickListener ocl = v -> activity.refreshExamsDone();
                activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG,ocl);
            }
            else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                View.OnClickListener ocl = v -> activity.refreshExamsDone();
                activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG,ocl);
            }
            else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                ClientHelper.createTextSnackBar(activity.getWindow().getDecorView(),R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
            }
            else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                InfoManager.clearSharedPreferences(activity.getApplication());
                Intent i = new Intent(activity, LauncherActivity.class);
                activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                activity.finish();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        ThemeEngine.applyPaymentsTheme(this);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this,toolbar, R.drawable.ic_baseline_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        nv = LayoutHelper.setupNavigationDrawer(this, mDrawerLayout);
        getSupportActionBar().setTitle(R.string.stats);
        setupListeners();
        os = InfoManager.getOpenStud(getApplication());
        if (os == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(StatsActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        List<ExamDone> exams_cached  = InfoManager.getExamsDoneCached(this,os);
        if (exams_cached != null && !exams_cached.isEmpty())  {
            exams.addAll(exams_cached);
            updateStats();
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1,R.color.refresh2,R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(() -> refreshExamsDone());
        if (firstStart) refreshExamsDone();

    }



    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        if (firstStart) {
            firstStart = false;
        }
        else if (time==null || Duration.between(time,LocalDateTime.now()).toMinutes()>30) refreshExamsDone();
    }

    private void updateStats(){
        runOnUiThread(() -> {
            if (exams == null || exams.isEmpty()) {
                totalCFU.setText("--");
                arithmeticValue.setText("--");
                weightedValue.setText("--");
                return;
            }
            NumberFormat numFormat = NumberFormat.getInstance();
            numFormat.setMaximumFractionDigits(2);
            numFormat.setMinimumFractionDigits(1);
            totalCFU.setText(String.valueOf(OpenstudHelper.getSumCFU(exams)));
            arithmeticValue.setText(numFormat.format(OpenstudHelper.computeArithmeticAverage(exams, 31)));
            weightedValue.setText(numFormat.format(OpenstudHelper.computeWeightedAverage(exams,31)));


            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                    new DataPoint(0, 1),
                    new DataPoint(1, 5),
                    new DataPoint(2, 3)
            });
            graph.addSeries(series);
            graph2.addSeries(series);

        });
    }

    private void  refreshExamsDone(){
        if (os == null) return;
        setRefreshing(true);
        new Thread(() -> {
            List<ExamDone> update = null;
            try {
                update = InfoManager.getExamsDone(this,os);
                if (update == null) h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
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

            if (update==null) {
                setRefreshing(false);
                return;
            }
            updateTimer();
            updateStats();
            setRefreshing(false);
        }).start();
    }

    private void setRefreshing(final boolean bool){
        this.runOnUiThread(() -> swipeRefreshLayout.setRefreshing(bool));
    }

    private synchronized void updateTimer(){
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer(){
        return lastUpdate;
    }


    public synchronized  void createRetrySnackBar(final int string_id, int length, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar
                .make(mDrawerLayout, getResources().getString(string_id), length).setAction(R.string.retry, listener);
        snackbar.show();
    }

    private void setupListeners(){
        ddl = new DelayedDrawerListener(){
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                int item = getItemPressedAndReset();
                if (item == -1) return;
                switch (item) {
                    case R.id.payments_menu: {
                        Intent intent = new Intent(StatsActivity.this, PaymentsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.exit_menu: {
                        InfoManager.clearSharedPreferences(getApplication());
                        Intent i = new Intent(StatsActivity.this, LauncherActivity.class);
                        startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        break;
                    }
                    case R.id.exams_menu: {
                        Intent intent = new Intent(StatsActivity.this, ExamsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.about_menu: {
                        Intent intent = new Intent(StatsActivity.this, AboutActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.settings_menu: {
                        Intent intent = new Intent(StatsActivity.this, SettingsPrefActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.stats_menu: {
                        Intent intent = new Intent(StatsActivity.this, StatsActivity.class);
                        startActivity(intent);
                        break;
                    }
                }
            }

        };
        mDrawerLayout.addDrawerListener(ddl);
        nv.setNavigationItemSelectedListener(
                item -> {
                    mDrawerLayout.closeDrawers();
                    ddl.setItemPressed(item.getItemId());
                    return true;
                });
    }
}
