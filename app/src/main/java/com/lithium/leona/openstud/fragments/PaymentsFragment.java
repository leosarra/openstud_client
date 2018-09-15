package com.lithium.leona.openstud.fragments;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.TaxAdapter;

import org.threeten.bp.LocalDate;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Tax;

public class PaymentsFragment extends android.support.v4.app.Fragment {
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefresh;
    private List<Tax> taxes;
    private RecyclerView rv;
    private TaxAdapter adapter;
    private int mode;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.payments_fragment,null);
        ButterKnife.bind(this, v);
        Bundle bundle=getArguments();
        mode = bundle.getInt("mode");
        rv = (RecyclerView)v.findViewById(R.id.recyclerView);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        taxes = new LinkedList<>();
        adapter = new TaxAdapter(getActivity(), taxes, mode);
        rv.setAdapter(adapter);
        //createListData();
        return v;
    }

    /**
    private void createListData() {
        Tax tax = new Tax();
        tax.setCode("201900010747164");
        tax.setAcademicYear(20128);
        tax.setAmount(21);
        tax.setDescriptionCourse("Lorem ipsum docet");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("ISCRIZIONE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("201900010747164");
        tax.setAcademicYear(20128);
        tax.setAmount(22321);
        tax.setDescriptionCourse("Lorem ipsum docet2");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("IMMATRICOLAZIONE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("201900010747164");
        tax.setAcademicYear(20128);
        tax.setAmount(43321);
        tax.setDescriptionCourse("Lorem ipsum docet3");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("44111111231");
        tax.setAcademicYear(20128);
        tax.setAmount(6321);
        tax.setDescriptionCourse("Lorem ipsum docet3");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("44111111231");
        tax.setAcademicYear(20128);
        tax.setAmount(1321);
        tax.setDescriptionCourse("Ciao4");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("44111111231");
        tax.setAcademicYear(20128);
        tax.setAmount(2321);
        tax.setDescriptionCourse("Ciao4");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("44111111231");
        tax.setAcademicYear(20128);
        tax.setAmount(4321);
        tax.setDescriptionCourse("Ciao4");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("44111111231");
        tax.setAcademicYear(20128);
        tax.setAmount(1321);
        tax.setDescriptionCourse("Ciao4");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        tax = new Tax();
        tax.setCode("44111111231");
        tax.setAcademicYear(20128);
        tax.setAmount(321);
        tax.setDescriptionCourse("Ciao4");
        tax.setPaymentDate(LocalDate.now());
        tax.setPaymentDescriptionList("TASSE");
        taxes.add(tax);

        adapter.notifyDataSetChanged();
    }
    **/
}