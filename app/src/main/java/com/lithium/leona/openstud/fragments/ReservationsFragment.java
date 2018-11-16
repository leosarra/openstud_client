package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.ExamsActivity;
import com.lithium.leona.openstud.activities.LauncherActivity;
import com.lithium.leona.openstud.adapters.ActiveReservationsAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class ReservationsFragment extends Fragment {

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

    @OnClick(R.id.empty_button_reload)
    public void OnClick(View v) {
        refreshReservations();
    }

    private List<ExamReservation> reservations;
    private Openstud os;
    private ActiveReservationsAdapter adapter;
    private LocalDateTime lastUpdate;
    private boolean firstStart = true;
    private ReservationsHandler h = new ReservationsHandler(this);

    private static class ReservationsHandler extends Handler {
        private final WeakReference<ReservationsFragment> frag;

        private ReservationsHandler(ReservationsFragment frag) {
            this.frag = new WeakReference<>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            final ReservationsFragment reservationsFrag = frag.get();
            if (reservationsFrag == null) return;
            ExamsActivity activity = (ExamsActivity) reservationsFrag.getActivity();
            if (activity != null) {
                View.OnClickListener ocl = v -> reservationsFrag.refreshReservations();
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG, ocl);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    activity.createRetrySnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG, ocl);
                } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    InfoManager.clearSharedPreferences(activity.getApplication());
                    Intent i = new Intent(activity, LauncherActivity.class);
                    i.putExtra("error", msg.what);
                    activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    activity.finish();
                } else if (msg.what == (ClientHelper.Status.FAILED_DELETE).getValue()) {
                    activity.createTextSnackBar(R.string.failed_delete, Snackbar.LENGTH_LONG);
                } else if (msg.what == (ClientHelper.Status.OK_DELETE).getValue()) {
                    activity.createTextSnackBar(R.string.ok_delete, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.FAILED_GET_IO.getValue()) {
                    activity.createTextSnackBar(R.string.failed_get_io, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.FAILED_GET.getValue()) {
                    activity.createTextSnackBar(R.string.failed_get_network, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.CLOSED_RESERVATION.getValue()) {
                    activity.createTextSnackBar(R.string.closed_reservation, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                    activity.createTextSnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.base_swipe_fragment, null);
        ButterKnife.bind(this, v);
        reservations = new LinkedList<>();
        os = InfoManager.getOpenStud(getActivity());
        final Activity activity = getActivity();
        if (activity == null) return v;
        if (os == null) {
            InfoManager.clearSharedPreferences(getActivity().getApplication());
            Intent i = new Intent(getActivity(), LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return v;
        }
        emptyText.setText(getResources().getString(R.string.no_reservations_found));
        List<ExamReservation> reservations_cached = InfoManager.getActiveReservationsCached(getActivity().getApplication(), os);
        if (reservations_cached != null && !reservations_cached.isEmpty()) {
            reservations.addAll(reservations_cached);
        }

        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        rv.setLayoutManager(llm);
        adapter = new ActiveReservationsAdapter(activity, reservations, new ActiveReservationsAdapter.ReservationAdapterListener() {
            @Override
            public void deleteReservationOnClick(final ExamReservation res) {
                createConfirmDeleteDialog(activity, res);
            }

            @Override
            public void downloadReservationOnClick(final ExamReservation res) {
                if (!ClientHelper.isExternalStorageAvailable() && ClientHelper.isExternalStorageReadOnly())
                    return;
                boolean result = ClientHelper.requestReadWritePermissions(activity);
                if (!result) {
                    return;
                }
                new Thread(() -> getFile(activity, res)).start();
            }

            @Override
            public void addCalendarOnClick(ExamReservation res) {
                addToCalendar(activity, res);
            }
        });
        rv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(this::refreshReservations);
        refreshReservations();
        return v;
    }


    private void getFile(Activity activity, ExamReservation res) {
        boolean check = false;
        String directory = Environment.getExternalStorageDirectory() + "/OpenStud/pdf/";
        File dirs = new File(directory);
        dirs.mkdirs();
        File pdfFile = new File(directory + res.getSessionID() + "_" + res.getExamSubject() + "_" + res.getReservationNumber() + ".pdf");
        try {
            if (pdfFile.exists()) {
                openActionViewPDF(activity, pdfFile);
                return;
            }
            pdfFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(pdfFile);
            byte[] content = os.getPdf(res);
            fos.write(content);
            fos.close();
            check = true;
        } catch (OpenstudConnectionException | OpenstudInvalidResponseException e) {
            h.sendEmptyMessage(ClientHelper.Status.FAILED_GET.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidCredentialsException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
            e.printStackTrace();
        } catch (IOException e) {
            h.sendEmptyMessage(ClientHelper.Status.FAILED_GET_IO.getValue());
            e.printStackTrace();
        }
        if (!check) {
            pdfFile.delete();
            return;
        }
        openActionViewPDF(activity, pdfFile);
    }

    private void openActionViewPDF(Activity activity, File pdfFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(activity, "com.lithium.leona.openstud.provider", pdfFile);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        Activity activity = getActivity();
        if (firstStart) firstStart = false;
        else if (activity != null && (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 30))
            refreshReservations();
        else if (activity != null && InfoManager.getReservationUpdateFlag(activity)) {
            refreshReservations();
            InfoManager.setReservationUpdateFlag(activity, false);
        }
    }

    private void refreshReservations() {
        final Activity activity = getActivity();
        if (activity == null || os == null) return;
        setRefreshing(true);
        setButtonReloadStatus(false);
        new Thread(() -> {
            List<ExamReservation> update = null;
            try {
                update = InfoManager.getActiveReservations(activity.getApplication(), os);
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

    public synchronized void refreshDataSet(List<ExamReservation> update) {
        boolean flag = false;
        if (update != null && !reservations.equals(update)) {
            flag = true;
            reservations.clear();
            reservations.addAll(update);
        }
        final boolean finalFlag = flag;
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (finalFlag) adapter.notifyDataSetChanged();
            swapViews(reservations);
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

    private void swapViews(final List<ExamReservation> reservations) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (reservations.isEmpty()) {
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

    private void addToCalendar(Activity activity, final ExamReservation res) {
        ZoneId zoneId = ZoneId.systemDefault();
        Timestamp timestamp = new Timestamp(res.getExamDate().atStartOfDay(zoneId).toEpochSecond());
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        String title;
        if (Locale.getDefault().getLanguage().equals("it"))
            title = "Esame: " + res.getExamSubject();
        else title = "Exam: " + res.getExamSubject();
        intent.putExtra(CalendarContract.Events.TITLE, title);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                timestamp.getTime() * 1000L);
        intent.putExtra(CalendarContract.Events.ALL_DAY, true);
        startActivity(intent);


    }

    private void createConfirmDeleteDialog(Activity activity, final ExamReservation res) {
        if (Period.between(res.getEndDate(), LocalDate.from(LocalDateTime.now())).getDays() >= 1) {
            h.sendEmptyMessage(ClientHelper.Status.CLOSED_RESERVATION.getValue());
            return;
        }
        int themeId = ThemeEngine.getAlertDialogTheme(activity);
        new AlertDialog.Builder(new ContextThemeWrapper(activity, themeId))
                .setTitle(getResources().getString(R.string.delete_res_dialog_title))
                .setMessage(getResources().getString(R.string.delete_res_dialog_description, res.getExamSubject()))
                .setPositiveButton(getResources().getString(R.string.delete_ok), (dialog, which) -> new Thread(() -> deleteReservation(res)).start())
                .setNegativeButton(getResources().getString(R.string.delete_abort), (dialog, which) -> {
                })
                .show();
    }

    private void deleteReservation(ExamReservation res) {
        try {
            int ret = os.deleteReservation(res);
            if (ret != -1) {
                synchronized (this) {
                    refreshReservations();
                }
                h.sendEmptyMessage(ClientHelper.Status.OK_DELETE.getValue());
            } else h.sendEmptyMessage(ClientHelper.Status.FAILED_DELETE.getValue());
        } catch (OpenstudInvalidResponseException | OpenstudConnectionException e) {
            h.sendEmptyMessage(ClientHelper.Status.FAILED_DELETE.getValue());
        } catch (OpenstudInvalidCredentialsException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
        }
    }
}
