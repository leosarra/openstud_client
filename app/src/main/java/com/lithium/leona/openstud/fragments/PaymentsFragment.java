package com.lithium.leona.openstud.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;

import com.lithium.leona.openstud.LauncherActivity;
import com.lithium.leona.openstud.PaymentsActivity;
import com.lithium.leona.openstud.ProfileActivity;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.TaxAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import org.threeten.bp.LocalDate;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Tax;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class PaymentsFragment extends android.support.v4.app.Fragment {

    private static class PaymentsHandler extends Handler {
        private final WeakReference<PaymentsFragment> frag;

        public PaymentsHandler(PaymentsFragment frag) {
            this.frag = new WeakReference<PaymentsFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            final PaymentsFragment paymentFrag = frag.get();
            if (paymentFrag== null) return;
            PaymentsActivity activity = (PaymentsActivity) paymentFrag.getActivity();
            if (activity != null) {
                if (msg.what == ClientHelper.Status.OK.getValue()) paymentFrag.refreshDataSet();

                else if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    activity.createTextSnackBar(R.string.connection_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    activity.createTextSnackBar(R.string.invalid_response_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    activity.createTextSnackBar(R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                }
                else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue()) {
                    activity.createTextSnackBar(R.string.invalid_password_error, Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView rv;
    private List<Tax> taxes;
    private TaxAdapter adapter;
    private int mode;
    private Openstud os;
    private PaymentsHandler h = new PaymentsHandler(this);
    private boolean firstStart = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.payments_fragment,null);
        ButterKnife.bind(this, v);
        Bundle bundle=getArguments();
        mode = bundle.getInt("mode");
        taxes = new LinkedList<>();
        os = InfoManager.getOpenStud(getActivity().getApplication());
        if (mode == TaxAdapter.Mode.PAID.getValue()) taxes = InfoManager.getPaidTaxesCached(getActivity().getApplication(),os);
        else if (mode == TaxAdapter.Mode.UNPAID.getValue()) taxes = InfoManager.getUnpaidTaxesCached(getActivity().getApplication(),os);
        if (taxes == null)  taxes = new LinkedList<>();
        if (os == null) {
            InfoManager.clearSharedPreferences(getActivity().getApplication());
            Intent i = new Intent(getActivity(), LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        adapter = new TaxAdapter(getActivity(), taxes, mode);
        rv.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
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
        if (firstStart) {
            firstStart = false;
            return;
        }
        else if (isUpdateRecommended()) refresh();
    }

    private synchronized void  refresh(){
        swipeRefreshLayout.setRefreshing(true);
        new Thread(new Runnable() {
            @Override
            public void run() {

                List<Tax> update = null;
                try {
                    if (mode == TaxAdapter.Mode.PAID.getValue()) update = InfoManager.getPaidTaxes(getActivity().getApplication(),os);
                    else if (mode == TaxAdapter.Mode.UNPAID.getValue()) update = InfoManager.getUnpaidTaxes(getActivity().getApplication(),os);
                    h.sendEmptyMessage(ClientHelper.Status.OK.getValue());
                } catch (OpenstudConnectionException e) {
                    h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                    e.printStackTrace();
                } catch (OpenstudInvalidResponseException e) {
                    h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                    e.printStackTrace();
                }
                if (taxes.equals(update)) {
                    setRefreshing(false);
                    return;
                }
                taxes.clear();
                taxes.addAll(update);
                refreshDataSet();
            }
        }).start();
    }


    public void refreshDataSet(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    public void setRefreshing(final boolean flag) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(flag);
            }
        });
    }

    private boolean isUpdateRecommended(){
        if(mode == TaxAdapter.Mode.PAID.getValue()) return InfoManager.isPaidTaxesUpdateRecommended(getActivity(),60);
        else if(mode == TaxAdapter.Mode.UNPAID.getValue()) return InfoManager.isUnpaidTaxesUpdateRecommended(getActivity(),60);
        return false;
    }


}