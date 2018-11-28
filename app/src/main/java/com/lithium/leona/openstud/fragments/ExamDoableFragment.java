package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.ExamsActivity;
import com.lithium.leona.openstud.activities.LauncherActivity;
import com.lithium.leona.openstud.activities.SearchSessionsResultActivity;
import com.lithium.leona.openstud.adapters.ExamDoableAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.ExamDoable;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class ExamDoableFragment extends Fragment {

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
    private List<ExamDoable> examsDoable;
    private Openstud os;
    private ExamDoableAdapter adapter;
    private LocalDateTime lastUpdate;
    private boolean firstStart = true;
    private ExamsDoableHandler h = new ExamsDoableHandler(this);

    @OnClick(R.id.empty_button_reload)
    public void OnClick(View v) {
        refreshExamsDoable();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.base_swipe_fragment, null);
        ButterKnife.bind(this, v);
        examsDoable = new LinkedList<>();
        final Activity activity = getActivity();
        if (activity == null) return v;
        os = InfoManager.getOpenStud(getActivity().getApplication());

        if (os == null) {
            InfoManager.clearSharedPreferences(getActivity().getApplication());
            Intent i = new Intent(getActivity(), LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return v;
        }
        emptyText.setText(getResources().getString(R.string.no_exams_doable_found));
        List<ExamDoable> exams_cached = InfoManager.getExamsDoableCached(getActivity().getApplication(), os);
        if (exams_cached != null && !exams_cached.isEmpty()) {
            examsDoable.addAll(exams_cached);
        }

        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        rv.setLayoutManager(llm);
        adapter = new ExamDoableAdapter(activity, examsDoable, exam -> {
            Intent intent = new Intent(activity, SearchSessionsResultActivity.class);
            intent.putExtra("exam", new Gson().toJson(exam));
            activity.startActivity(intent);
        });
        rv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(this::refreshExamsDoable);
        refreshExamsDoable();
        return v;
    }

    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        Activity activity = getActivity();
        if (firstStart) firstStart = false;
        else if (activity != null && (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 30))
            refreshExamsDoable();
    }

    private void refreshExamsDoable() {
        final Activity activity = getActivity();
        if (activity == null || os == null) return;
        setRefreshing(true);
        setButtonReloadStatus(false);
        new Thread(() -> {
            List<ExamDoable> update = null;
            try {
                update = InfoManager.getExamsDoable(activity.getApplication(), os);
                if (update == null)
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

            if (update == null) {
                setRefreshing(false);
                setButtonReloadStatus(true);
                return;
            }
            updateTimer();
            refreshDataSet(update);
        }).start();
    }

    public synchronized void refreshDataSet(List<ExamDoable> update) {
        boolean flag = false;
        if (update != null && !examsDoable.equals(update)) {
            flag = true;
            examsDoable.clear();
            examsDoable.addAll(update);
        }
        final boolean finalFlag = flag;
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (finalFlag) adapter.notifyDataSetChanged();
            swapViews(examsDoable);
            swipeRefreshLayout.setRefreshing(false);
            emptyButton.setEnabled(true);
        });
    }

    private void setRefreshing(final boolean bool) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> swipeRefreshLayout.setRefreshing(bool));
    }

    private void setButtonReloadStatus(final boolean bool) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> emptyButton.setEnabled(bool));
    }

    private void swapViews(final List<ExamDoable> exams) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (exams.isEmpty()) {
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

    private static class ExamsDoableHandler extends Handler {
        private final WeakReference<ExamDoableFragment> frag;

        private ExamsDoableHandler(ExamDoableFragment frag) {
            this.frag = new WeakReference<>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            final ExamDoableFragment examsDoableFrag = frag.get();
            if (examsDoableFrag == null) return;
            ExamsActivity activity = (ExamsActivity) examsDoableFrag.getActivity();
            if (activity != null) {
                View.OnClickListener ocl = v -> examsDoableFrag.refreshExamsDoable();
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG, ocl);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG, ocl);
                } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    InfoManager.clearSharedPreferences(activity.getApplication());
                    Intent i = new Intent(activity, LauncherActivity.class);
                    activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    activity.finish();
                } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                    activity.createTextSnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
            }
        }
    }

}
