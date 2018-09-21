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


import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lithium.leona.openstud.LauncherActivity;
import com.lithium.leona.openstud.PaymentsActivity;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.TaxAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.ExamPassed;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Tax;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class PaymentsFragment extends android.support.v4.app.Fragment {

    private static class PaymentsHandler extends Handler {
        private final WeakReference<PaymentsFragment> frag;

        private PaymentsHandler(PaymentsFragment frag) {
            this.frag = new WeakReference<PaymentsFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            final PaymentsFragment paymentFrag = frag.get();
            if (paymentFrag== null) return;
            PaymentsActivity activity = (PaymentsActivity) paymentFrag.getActivity();
            if (activity != null) {
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    View.OnClickListener ocl = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            paymentFrag.refresh();
                        }
                    };
                    activity.createRetrySnackBar(R.string.connection_error, Snackbar.LENGTH_LONG,ocl);
                }
                else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    View.OnClickListener ocl = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            paymentFrag.refresh();
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

    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView rv;
    @BindView(R.id.empty_layout) LinearLayout emptyView;
    @BindView(R.id.empty_button_reload) Button emptyButton;
    @BindView(R.id.empty_text) TextView emptyText;
    @OnClick(R.id.empty_button_reload) public void OnClick(View v){
        refresh();
    }
    private List<Tax> taxes;
    private TaxAdapter adapter;
    private int mode;
    private Openstud os;
    private PaymentsHandler h = new PaymentsHandler(this);
    private boolean firstStart = true;
    private LocalDateTime lastUpdate;


    public static PaymentsFragment newInstance(int mode) {
        PaymentsFragment frag = new PaymentsFragment();
        Bundle args = new Bundle();
        args.putInt("mode", mode);
        frag.setArguments(args);
        return frag;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.base_swipe_fragment,null);
        ButterKnife.bind(this, v);
        Bundle bundle=getArguments();
        mode = bundle.getInt("mode");
        emptyView.setVisibility(View.GONE);
        os = InfoManager.getOpenStud(getActivity().getApplication());
        if (os == null) {
            InfoManager.clearSharedPreferences(getActivity().getApplication());
            Intent i = new Intent(getActivity(), LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return v;
        }
        taxes = new LinkedList<>();
        List<Tax> taxes_cached = null;
        if (mode == TaxAdapter.Mode.PAID.getValue()) {
            emptyText.setText(getResources().getString(R.string.no_paid_tax_found));
            taxes_cached = InfoManager.getPaidTaxesCached(getActivity().getApplication(),os);
        }
        else if (mode == TaxAdapter.Mode.UNPAID.getValue()) {
            emptyText.setText(getResources().getString(R.string.no_unpaid_tax_found));
            taxes_cached = InfoManager.getUnpaidTaxesCached(getActivity().getApplication(),os);
        }
        if (taxes_cached != null && !taxes_cached.isEmpty())  taxes.addAll(taxes_cached);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        adapter = new TaxAdapter(getActivity(), taxes, mode);
        rv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        refresh();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        if (firstStart) {
            firstStart = false;
        }
        else if (getActivity()!= null && (time==null || Duration.between(time,LocalDateTime.now()).toMinutes()>30)) refresh();
    }

    private void  refresh(){
        final Activity activity = getActivity();
        if (activity == null) return;
        setRefreshing(true);
        setButtonReloadStatus(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Tax> update = null;
                boolean isChanged = false;
                try {
                    if (mode == TaxAdapter.Mode.PAID.getValue())
                        update = InfoManager.getPaidTaxes(activity.getApplication(), os);
                    else if (mode == TaxAdapter.Mode.UNPAID.getValue())
                        update = InfoManager.getUnpaidTaxes(activity.getApplication(), os);

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

    public synchronized void refreshDataSet(List<Tax> update){
        boolean flag = false;
        if (update != null && !taxes.equals(update)) {
            flag = true;
            taxes.clear();
            taxes.addAll(update);
        }
        final boolean finalFlag = flag;
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (finalFlag) adapter.notifyDataSetChanged();
                swapViews(taxes);
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


    private synchronized void updateTimer(){
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer(){
        return lastUpdate;
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

    private void swapViews(final List<Tax> taxes) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (taxes.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

}