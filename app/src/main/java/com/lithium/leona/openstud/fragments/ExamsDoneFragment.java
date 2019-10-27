package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.ExamsActivity;
import com.lithium.leona.openstud.adapters.ExamDoneAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.OpenstudHelper;
import lithium.openstud.driver.core.models.ExamDone;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class ExamsDoneFragment extends BaseDataFragment {

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
    private List<ExamDone> exams;
    private ExamDoneAdapter adapter;
    private LocalDateTime lastUpdate;
    private boolean firstStart = true;
    private ExamsDoneHandler h = new ExamsDoneHandler(this);
    private boolean showExamDate;

    @OnClick(R.id.empty_button_reload)
    public void OnClick(View v) {
        refreshExamsDone();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.base_swipe_fragment, null);
        Activity activity = getActivity();
        if (!initData() || activity == null) return v;
        ButterKnife.bind(this, v);
        exams = new LinkedList<>();
        showExamDate = PreferenceManager.isExamDateEnabled(activity);
        emptyText.setText(getResources().getString(R.string.no_exams_done_found));
        List<ExamDone> exams_cached = InfoManager.getExamsDoneCached(activity, os);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        rv.setLayoutManager(llm);
        adapter = new ExamDoneAdapter(activity, exams, 0);
        rv.setAdapter(adapter);
        if (exams_cached != null && !exams_cached.isEmpty()) {
            exams.addAll(exams_cached);
            sortList(ClientHelper.Sort.getSort(InfoManager.getSortType(activity)));
        } else swapViews(exams_cached);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(this::refreshExamsDone);
        if (savedInstanceState == null) refreshExamsDone();
        return v;
    }

    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        Activity activity = getActivity();
        if (activity != null && showExamDate != PreferenceManager.isExamDateEnabled(activity)) {
            adapter.notifyDataSetChanged();
            showExamDate = !showExamDate;
        }
        if (firstStart) firstStart = false;
        else if (activity != null && (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 30))
            refreshExamsDone();
    }

    public synchronized void sortList(ClientHelper.Sort sort) {
        if (adapter == null || sort == null) return;
        if (sort == ClientHelper.Sort.Date) {
            OpenstudHelper.sortExamByDate(exams, false);
            adapter.notifyDataSetChanged();
        } else if (sort == ClientHelper.Sort.Mark) {
            OpenstudHelper.sortExamByGrade(exams, false);
            adapter.notifyDataSetChanged();
        }
    }

    private void refreshExamsDone() {
        final Activity activity = getActivity();
        if (activity == null || os == null) return;
        setRefreshing(true);
        setButtonReloadStatus(false);
        new Thread(() -> {
            List<ExamDone> update = null;
            try {
                update = InfoManager.getExamsDone(activity.getApplication(), os);
                if (update == null)
                    h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.OK.getValue());

            } catch (OpenstudConnectionException e) {
                h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidResponseException e) {
                if (e.isMaintenance())
                    h.sendEmptyMessage(ClientHelper.Status.MAINTENANCE.getValue());
                h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidCredentialsException e) {
                h.sendEmptyMessage(ClientHelper.getStatusFromLoginException(e).getValue());
                e.printStackTrace();
            }

            if (update == null) {
                setRefreshing(false);
                setButtonReloadStatus(true);
                swapViews(exams);
                return;
            }
            updateTimer();
            refreshDataSet(update);
        }).start();
    }

    private synchronized void refreshDataSet(List<ExamDone> update) {
        boolean flag = false;
        Activity activity = getActivity();
        if (activity == null) return;
        if (update != null && !exams.equals(update)) {
            flag = true;
            exams.clear();
            exams.addAll(update);
            ClientHelper.updateGradesWidget(activity, true);
        }
        final boolean finalFlag = flag;
        activity.runOnUiThread(() -> {
            if (finalFlag) {
                int sort = InfoManager.getSortType(activity);
                if (sort != ClientHelper.Sort.Date.getValue()) {
                    if (sort == ClientHelper.Sort.Mark.getValue()) sortList(ClientHelper.Sort.Mark);
                }
                adapter.notifyDataSetChanged();
            }
            swapViews(exams);
            swipeRefreshLayout.setRefreshing(false);
            emptyButton.setEnabled(true);
        });
    }

    private void setRefreshing(final boolean bool) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> swipeRefreshLayout.setRefreshing(bool));
    }

    private synchronized void updateTimer() {
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer() {
        return lastUpdate;
    }

    private void setButtonReloadStatus(final boolean bool) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> emptyButton.setEnabled(bool));
    }

    private void swapViews(final List<ExamDone> exams) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (exams == null || exams.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        });
    }

    private static class ExamsDoneHandler extends Handler {
        private final WeakReference<ExamsDoneFragment> frag;

        private ExamsDoneHandler(ExamsDoneFragment frag) {
            this.frag = new WeakReference<>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            final ExamsDoneFragment examsDoneFrag = frag.get();
            if (examsDoneFrag == null) return;
            ExamsActivity activity = (ExamsActivity) examsDoneFrag.getActivity();
            if (activity != null) {
                View.OnClickListener listener = v -> examsDoneFrag.refreshExamsDone();
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    activity.createRetrySnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.MAINTENANCE.getValue()) {
                    activity.createRetrySnackBar(R.string.infostud_maintenance, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue() || msg.what == ClientHelper.Status.ACCOUNT_BLOCKED.getValue()) {
                    ClientHelper.rebirthApp(activity, msg.what);
                } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                    activity.createTextSnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
            }
        }
    }

}
