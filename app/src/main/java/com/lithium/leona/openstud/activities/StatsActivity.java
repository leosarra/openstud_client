package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
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
    @BindView(R.id.graph) LineChart graph;
    @BindView(R.id.graph2) BarChart graph2;
    @BindView(R.id.graph_card) CardView graphCard;
    @BindView(R.id.graph2_card) CardView graphCard2;
    private DelayedDrawerListener ddl;
    private NavigationView nv;
    private Openstud os;
    private StatsHandler h = new StatsHandler(this);
    private List<ExamDone> exams = new LinkedList<>();
    private LocalDateTime lastUpdate;
    private boolean firstStart = true;
    private int laude;
    private Student student;

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
                activity.createRetrySnackBar(R.string.connection_error,R.string.retry, Snackbar.LENGTH_LONG,ocl);
            }
            else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                View.OnClickListener ocl = v -> activity.refreshExamsDone();
                activity.createRetrySnackBar(R.string.connection_error,R.string.retry, Snackbar.LENGTH_LONG,ocl);
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
            else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                ClientHelper.createTextSnackBar(activity.getWindow().getDecorView(), R.string.invalid_response_error, Snackbar.LENGTH_LONG);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyStatsTheme(this);
        setContentView(R.layout.activity_stats);
        ThemeEngine.applyPaymentsTheme(this);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this,toolbar, R.drawable.ic_baseline_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        nv = LayoutHelper.setupNavigationDrawer(this, mDrawerLayout);
        getSupportActionBar().setTitle(R.string.stats);
        setupListeners();
        os = InfoManager.getOpenStud(getApplication());
        student = InfoManager.getInfoStudentCached(this,os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(StatsActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        View headerLayout = nv.getHeaderView(0);
        TextView navTitle = headerLayout.findViewById(R.id.nav_title);
        navTitle.setText(getString(R.string.fullname, student.getFirstName(), student.getLastName()));
        TextView subTitle = headerLayout.findViewById(R.id.nav_subtitle);
        subTitle.setText(String.valueOf(student.getStudentID()));


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
        else {
            graphCard.setVisibility(View.GONE);
            graphCard2.setVisibility(View.GONE);
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
        else if (PreferenceManager.getLaudeValue(this) != laude || time==null || Duration.between(time,LocalDateTime.now()).toMinutes()>30) refreshExamsDone();
    }

    private void updateStats(){
        runOnUiThread(() -> {
            if (exams == null || exams.isEmpty() || !ClientHelper.hasPassedExams(exams)) {
                totalCFU.setText("--");
                arithmeticValue.setText("--");
                weightedValue.setText("--");
                graphCard.setVisibility(View.GONE);
                graphCard2.setVisibility(View.GONE);
                return;
            }
            showLaudeNotification();
            laude = PreferenceManager.getLaudeValue(this);
            updateGraphs();
        });
    }


    private void updateGraphs(){
        runOnUiThread(() -> {
            graphCard.setVisibility(View.VISIBLE);
            graphCard2.setVisibility(View.VISIBLE);
            NumberFormat numFormat = NumberFormat.getInstance();
            numFormat.setMaximumFractionDigits(2);
            numFormat.setMinimumFractionDigits(1);
            totalCFU.setText(String.valueOf(OpenstudHelper.getSumCFU(exams)));
            arithmeticValue.setText(numFormat.format(OpenstudHelper.computeArithmeticAverage(exams, laude)));
            weightedValue.setText(numFormat.format(OpenstudHelper.computeWeightedAverage(exams, laude)));
            makeAverageGraph();
            makeGradeBarGraph();
        });
    }

    private void makeAverageGraph(){
        graph.clear();
        List<Entry> serie1 = ClientHelper.generateMarksPoints(exams, laude);
        List<Entry> serie2 = ClientHelper.generateWeightPoints(exams, laude);
        LineDataSet datasetLine1 = new LineDataSet(serie1, getResources().getString(R.string.grades));
        LineDataSet datasetLineGraph2 = new LineDataSet(serie2, getResources().getString(R.string.weighted_average));
        datasetLineGraph2.setDrawCircles(false);
        datasetLine1.setColor(Color.parseColor("#0077CC"));
        datasetLine1.setCircleColor(Color.parseColor("#004a80"));
        datasetLineGraph2.setColor(Color.parseColor("#b40a23"));
        datasetLine1.setLineWidth(2);
        datasetLineGraph2.setLineWidth(2);
        LineData dataLine = new LineData();
        dataLine.addDataSet(datasetLine1);
        dataLine.addDataSet(datasetLineGraph2);
        dataLine.setDrawValues(false);
        dataLine.setHighlightEnabled(false);
        graph.getLegend().setTextColor(ThemeEngine.getPrimaryTextColor(this));
        graph.getLegend().setTextSize(Utils.convertDpToPixel(5));
        graph.getLegend().setXEntrySpace(20);
        graph.setData(dataLine);
        graph.invalidate();
        graph.getAxisRight().setEnabled(false);
        graph.setScaleEnabled(false);
        graph.getDescription().setEnabled(false);
        graph.getAxisLeft().setTextSize(Utils.convertDpToPixel(5));
        graph.getXAxis().setEnabled(false);
        graph.getAxisLeft().setTextColor(ThemeEngine.getPrimaryTextColor(this)); // left y-axis
        graph.getAxisLeft().setGridColor(ThemeEngine.getSecondaryTextColor(this));
        graph.getXAxis().setTextColor(ThemeEngine.getPrimaryTextColor(this));
        graph.getXAxis().setGridColor(ThemeEngine.getSecondaryTextColor(this));
        graph.getAxisLeft().setGranularity(2);
        graph.setScaleEnabled(false);
    }

    private void makeGradeBarGraph(){
        graph2.clear();
        ArrayList<BarEntry> entriesGraph2 = ClientHelper.generateMarksBar(exams);
        BarDataSet datasetBar = new BarDataSet(entriesGraph2, "Marks");
        BarData dataBar = new BarData(datasetBar);
        dataBar.setHighlightEnabled(false);
        dataBar.setDrawValues(false);
        datasetBar.setColor(Color.parseColor("#0077CC"));
        graph2.setData(dataBar);
        graph2.getAxisRight().setEnabled(false);
        graph2.setScaleEnabled(false);
        graph2.getDescription().setEnabled(false);
        graph2.getLegend().setEnabled(false);
        graph2.getAxisLeft().setTextSize(Utils.convertDpToPixel(5));
        graph2.getAxisLeft().setGranularity(1);
        graph2.getAxisLeft().setMinWidth(0);
        graph2.getXAxis().setTextSize(Utils.convertDpToPixel(4));
        graph2.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        graph2.getAxisLeft().setTextColor(ThemeEngine.getPrimaryTextColor(this)); // left y-axis
        graph2.getAxisLeft().setGridColor(ThemeEngine.getSecondaryTextColor(this));
        graph2.getXAxis().setTextColor(ThemeEngine.getPrimaryTextColor(this));
        graph2.getXAxis().setGridColor(ThemeEngine.getSecondaryTextColor(this));
        graph2.getXAxis().setValueFormatter((value, axis) -> {
            if (value <= 30) {
                return String.valueOf((int) value);
            } else {
                return "30L";
            }
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
            if (update != null && !update.isEmpty()) {
                exams.clear();
                exams.addAll(update);
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


    public synchronized  void createRetrySnackBar(final int string_id, final int action_id, int length, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar
                .make(mDrawerLayout, getResources().getString(string_id), length).setAction(action_id, listener);
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
                    case R.id.profile_menu: {
                        Intent intent = new Intent(StatsActivity.this, ProfileActivity.class);
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

    private void showLaudeNotification(){
        if (com.lithium.leona.openstud.data.PreferenceManager.getStatsNotificationEnabled(this)) {
            createRetrySnackBar(R.string.no_value_laude, R.string.settings, 4000, v -> {
                InfoManager.clearSharedPreferences(getApplication());
                Intent i = new Intent(StatsActivity.this, SettingsPrefActivity.class);
                startActivity(i);
            });
            com.lithium.leona.openstud.data.PreferenceManager.setStatsNotificationEnabled(this, false);
        }
    }
}
