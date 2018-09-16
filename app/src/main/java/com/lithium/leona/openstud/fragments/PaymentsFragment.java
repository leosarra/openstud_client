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
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView rv;
    private List<Tax> taxes;
    private TaxAdapter adapter;
    private int mode;
    private Openstud os;

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



    private synchronized void  refresh(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                List<Tax> update = null;
                try {
                    if (mode == TaxAdapter.Mode.PAID.getValue()) update = InfoManager.getPaidTaxes(getActivity().getApplication(),os);
                    else if (mode == TaxAdapter.Mode.UNPAID.getValue()) update = InfoManager.getUnpaidTaxes(getActivity().getApplication(),os);
                } catch (OpenstudConnectionException e) {
                    e.printStackTrace();
                } catch (OpenstudInvalidResponseException e) {
                    e.printStackTrace();
                } finally {
                    swipeRefreshLayout.setRefreshing(false);
                }
                System.out.println(update);
                taxes.clear();
                taxes.addAll(update);
                System.out.println(taxes);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

}