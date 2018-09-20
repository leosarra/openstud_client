package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lithium.leona.openstud.ExamsActivity;
import com.lithium.leona.openstud.LauncherActivity;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.ActiveReservationsAdapter;
import com.lithium.leona.openstud.adapters.ExamDoneAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.ExamPassed;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class ReservationsFragment extends android.support.v4.app.Fragment {

    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView rv;
    @BindView(R.id.empty_layout) LinearLayout emptyView;


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

        List<ExamReservation> reservations_cached  = InfoManager.getActiveReservationsCached(getActivity().getApplication(),os);
        if (reservations_cached != null && !reservations_cached.isEmpty())  {
            reservations.addAll(reservations_cached);
        }

        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        adapter = new ActiveReservationsAdapter(getActivity(), reservations);
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
                updateTimer();
                if (update==null || reservations.equals(update)) {
                    setRefreshing(false);
                    return;
                }
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
                if (finalFlag) {
                    adapter.notifyDataSetChanged();
                    if (reservations.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    }
                    else {
                        emptyView.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                    }
                }
                swipeRefreshLayout.setRefreshing(false);
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


    private synchronized void updateTimer(){
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer(){
        return lastUpdate;
    }
}
