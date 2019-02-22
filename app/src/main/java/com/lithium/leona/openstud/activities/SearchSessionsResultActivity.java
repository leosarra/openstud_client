package com.lithium.leona.openstud.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.AvaiableReservationsAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import org.apache.commons.lang3.tuple.Pair;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.models.ExamDoable;
import lithium.openstud.driver.core.models.ExamReservation;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;


public class SearchSessionsResultActivity extends BaseDataActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView)
    RecyclerView rv;
    @BindView(R.id.empty_layout)
    LinearLayout emptyView;
    @BindView(R.id.empty_button_reload)
    Button emptyButton;
    @BindView(R.id.empty_text)
    TextView emptyText;
    @BindView(R.id.searchLayout)
    ConstraintLayout layout;
    private AvaiableReservationsAdapter adapter;
    private LocalDateTime lastUpdate;
    private ExamDoable exam;
    private List<ExamReservation> reservations;
    private SearchEventHandler h = new SearchEventHandler(this);
    private List<ExamReservation> activeReservations;
    private boolean firstStart = true;

    @OnClick(R.id.empty_button_reload)
    public void OnClick(View v) {
        refreshAvaiableReservations();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!initData()) return;
        ThemeEngine.applySearchTheme(this);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        String jsonObject;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            jsonObject = extras.getString("exam", null);
            exam = new Gson().fromJson(jsonObject, ExamDoable.class);
        }
        activeReservations = new LinkedList<>();
        List<ExamReservation> cache = InfoManager.getActiveReservationsCached(this, os);
        if (cache != null) {
            activeReservations.addAll(cache);
        }
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.search_sessions);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        reservations = new LinkedList<>();
        emptyText.setText(getResources().getString(R.string.no_sessions_found));
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        adapter = new AvaiableReservationsAdapter(this, reservations, activeReservations, this::confirmReservation);
        rv.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(this::refreshAvaiableReservations);
        if (savedInstanceState == null) refreshAvaiableReservations();
    }

    private boolean confirmReservation(ExamReservation res) {
        try {
            Pair<Integer, String> pair = os.insertReservation(res);
            InfoManager.setReservationUpdateFlag(this, true);
            if (pair == null) {
                h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                return false;
            } else if (pair.getRight() == null && pair.getLeft() == -1) {
                h.sendEmptyMessage(ClientHelper.Status.ALREADY_PLACED.getValue());
                return true;
            }
            if (pair.getRight() != null) ClientHelper.createCustomTab(this, pair.getRight());
            else {
                refreshAvaiableReservations();
                h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_OK.getValue());
                return true;
            }
        } catch (OpenstudInvalidResponseException e) {
            e.printStackTrace();
            if (e.isMaintenance()) h.sendEmptyMessage(ClientHelper.Status.MAINTENANCE.getValue());
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

    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        if (firstStart) firstStart = false;
        else if (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 30)
            refreshAvaiableReservations();
    }

    private void refreshAvaiableReservations() {
        setRefreshing(true);
        setButtonReloadStatus(false);
        Activity activity = this;
        new Thread(() -> {
            List<ExamReservation> update = null;
            List<ExamReservation> updateActiveReservations = null;
            try {
                update = os.getAvailableReservations(exam, student);
                updateActiveReservations = InfoManager.getActiveReservations(activity, os);
                if (update == null || updateActiveReservations == null)
                    h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.OK.getValue());

            } catch (OpenstudConnectionException e) {
                h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidResponseException e) {
                h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidCredentialsException e) {
                if (e.isPasswordExpired())
                    h.sendEmptyMessage(ClientHelper.Status.EXPIRED_CREDENTIALS.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
                e.printStackTrace();
            }

            if (update == null || updateActiveReservations == null) {
                setRefreshing(false);
                setButtonReloadStatus(true);
                swapViews(reservations);
                return;
            }
            updateTimer();
            refreshDataSet(update, updateActiveReservations);
        }).start();
    }

    public synchronized void refreshDataSet(List<ExamReservation> update, List<ExamReservation> updateActiveReservations) {
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

    private void setRefreshing(final boolean bool) {
        runOnUiThread(() -> swipeRefreshLayout.setRefreshing(bool));
    }

    private void setButtonReloadStatus(final boolean bool) {
        runOnUiThread(() -> emptyButton.setEnabled(bool));
    }

    private void swapViews(final List<ExamReservation> reservations) {
        runOnUiThread(() -> {
            if (reservations == null || reservations.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        });
    }

    private synchronized void updateTimer() {
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer() {
        return lastUpdate;
    }

    private static class SearchEventHandler extends Handler {
        private final WeakReference<SearchSessionsResultActivity> mActivity;

        SearchEventHandler(SearchSessionsResultActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SearchSessionsResultActivity activity = mActivity.get();
            if (activity != null) {
                OnClickListener listener = v -> new Thread(activity::refreshAvaiableReservations).start();
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.invalid_response_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.MAINTENANCE.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.layout, R.string.infostud_maintenance, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.INVALID_CREDENTIALS.getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    ClientHelper.rebirthApp(activity,msg.what);
                } else if (msg.what == ClientHelper.Status.PLACE_RESERVATION_OK.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.reservation_ok, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.PLACE_RESERVATION_INVALID_RESPONSE.getValue() || msg.what == ClientHelper.Status.PLACE_RESERVATION_CONNECTION.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.reservation_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.ALREADY_PLACED.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.already_placed_reservation, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.layout, R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
            }
        }
    }


}
