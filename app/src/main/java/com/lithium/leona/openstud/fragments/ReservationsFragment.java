package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lithium.leona.openstud.activities.ExamsActivity;
import com.lithium.leona.openstud.activities.LauncherActivity;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.ActiveReservationsAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class ReservationsFragment extends android.support.v4.app.Fragment {

    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView rv;
    @BindView(R.id.empty_layout) LinearLayout emptyView;
    @BindView(R.id.empty_button_reload) Button emptyButton;
    @BindView(R.id.empty_text) TextView emptyText;
    @OnClick(R.id.empty_button_reload) public void OnClick(View v){
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
            if (reservationsFrag== null) return;
            ExamsActivity activity = (ExamsActivity) reservationsFrag.getActivity();
            if (activity != null) {
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    View.OnClickListener ocl = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            reservationsFrag.refreshReservations();
                        }
                    };
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG,ocl);
                }
                else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    View.OnClickListener ocl = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            reservationsFrag.refreshReservations();
                        }
                    };
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG,ocl);
                }
                else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    InfoManager.clearSharedPreferences(activity.getApplication());
                    Intent i = new Intent(activity, LauncherActivity.class);
                    activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    activity.finish();
                }
                else if(msg.what == (ClientHelper.Status.FAILED_DELETE).getValue()){
                    activity.createTextSnackBar(R.string.failed_delete, Snackbar.LENGTH_LONG);
                }
                else if(msg.what == (ClientHelper.Status.OK_DELETE).getValue()){
                    activity.createTextSnackBar(R.string.ok_delete, Snackbar.LENGTH_LONG);
                }
                else if(msg.what == ClientHelper.Status.FAILED_GET_IO.getValue()){
                    activity.createTextSnackBar(R.string.failed_get_io, Snackbar.LENGTH_LONG);
                }
                else if(msg.what == ClientHelper.Status.FAILED_GET.getValue()){
                    activity.createTextSnackBar(R.string.failed_get_network, Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.base_swipe_fragment,null);
        ButterKnife.bind(this, v);
        Bundle bundle=getArguments();
        reservations = new LinkedList<>();
        os = InfoManager.getOpenStud(getActivity().getApplication());
        if (os == null) {
            InfoManager.clearSharedPreferences(getActivity().getApplication());
            Intent i = new Intent(getActivity(), LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return v;
        }
        emptyText.setText(getResources().getString(R.string.no_reservations_found));
        List<ExamReservation> reservations_cached  = InfoManager.getActiveReservationsCached(getActivity().getApplication(),os);
        if (reservations_cached != null && !reservations_cached.isEmpty())  {
            reservations.addAll(reservations_cached);
        }

        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        adapter = new ActiveReservationsAdapter(getActivity(), reservations, new ActiveReservationsAdapter.ReservationAdapterListener() {
            @Override
            public void deleteReservationOnClick(final ExamReservation res) {
                Activity activity = getActivity();
                if (activity == null) return;
                createConfirmDeleteDialog(activity, res);
            }

            @Override
            public void downloadReservationOnClick(final ExamReservation res) {
                final Activity activity = getActivity();
                if (activity == null) return;
                if (!ClientHelper.isExternalStorageAvailable() && ClientHelper.isExternalStorageReadOnly()) return;
                boolean result = ClientHelper.requestReadWritePermissions(activity);
                if (!result) {
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getFile(activity,res);
                    }
                }).start();
            }
        });
        rv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshReservations();
            }
        });
        if (firstStart) refreshReservations();
        return v;
    }


    private void getFile(Activity activity, ExamReservation res){
        boolean check = false;
        String namFile = Environment.getExternalStorageDirectory() + "/OpenStud/" + res.getSessionID()+"_"+res.getExamSubject()+"_"+res.getReservationNumber();
        File pdfFile = new File(namFile);
        try {
            if (pdfFile.exists()) {
                openActionViewPDF(activity,pdfFile);
                h.sendEmptyMessage(ClientHelper.Status.FAILED_GET_IO.getValue());
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
        openActionViewPDF(activity,pdfFile);
    }

    private void openActionViewPDF(Activity activity, File pdfFile){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(activity,"com.lithium.leona.openstud.provider",pdfFile);
        intent.setDataAndType(uri,"application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        if (firstStart) {
            firstStart = false;
        }
        else if (getActivity()!= null && (time==null || Duration.between(time,LocalDateTime.now()).toMinutes()>30)) refreshReservations();
    }

    private void  refreshReservations(){
        final Activity activity = getActivity();
        if (activity == null) return;
        setRefreshing(true);
        setButtonReloadStatus(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ExamReservation> update = null;
                boolean isChanged = false;
                try {
                    update = InfoManager.getActiveReservations(activity.getApplication(),os);
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
                    setButtonReloadStatus(true);
                    return;
                }
                updateTimer();
                refreshDataSet(update);
            }
        }).start();
    }

    public synchronized void refreshDataSet(List<ExamReservation> update){
        boolean flag = false;
        if (update != null && !reservations.equals(update)) {
            flag = true;
            reservations.clear();
            reservations.addAll(update);
        }
        final boolean finalFlag = flag;
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (finalFlag) adapter.notifyDataSetChanged();
                swapViews(reservations);
                swipeRefreshLayout.setRefreshing(false);
                emptyButton.setEnabled(true);
            }
        });
    }


    private void setRefreshing(final boolean bool){
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(bool);
            }
        });
    }


    private void setButtonReloadStatus(final boolean bool){
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                emptyButton.setEnabled(bool);
            }
        });
    }

    private void swapViews(final List<ExamReservation> reservations) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (reservations.isEmpty()) {
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

    private void createConfirmDeleteDialog(Activity activity, final ExamReservation res){
        new AlertDialog.Builder(activity)
                .setTitle(getResources().getString(R.string.delete_res_dialog_title))
                .setMessage(getResources().getString(R.string.delete_res_dialog_description, res.getExamSubject()))
                .setPositiveButton(getResources().getString(R.string.delete_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                deleteReservation(res);
                            }
                        }).start();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.delete_abort), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteReservation(ExamReservation res){
        try {
            int ret = os.deleteReservation(res);
            if (ret!=-1) {
                synchronized (this) {
                    reservations.remove(res);
                    adapter.notifyDataSetChanged();
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
