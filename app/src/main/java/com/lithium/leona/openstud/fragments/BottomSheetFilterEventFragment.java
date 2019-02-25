package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.CalendarActivity;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BottomSheetFilterEventFragment extends BottomSheetDialogFragment {
    @BindView(R.id.list)
    LinearLayout linearLayout;
    private boolean refreshNeeded = false;
    private List<String> elements = new LinkedList<>();

    public BottomSheetFilterEventFragment() {
        // Required empty public constructor
    }

    public static BottomSheetFilterEventFragment newInstance(List<String> names) {
        BottomSheetFilterEventFragment myFragment = new BottomSheetFilterEventFragment();
        Bundle args = new Bundle();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        String json = gson.toJson(names, listType);
        args.putSerializable("elements", json);
        myFragment.setArguments(args);
        return myFragment;
    }

    @OnClick(R.id.close)
    void onClick() {
        dismiss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bdl = getArguments();
        if (bdl != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            String json = bdl.getString("elements", null);
            if (json != null) {
                List<String> passedElements = gson.fromJson(json, listType);
                elements.clear();
                elements.addAll(passedElements);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.filter_calendar, container, false);
        ButterKnife.bind(this, v);
        ClientHelper.setDialogView(v, getDialog(), BottomSheetBehavior.STATE_EXPANDED);
        Context context = getContext();
        Activity activity = getActivity();
        InfoManager.removeOldEntriesFilter(context, elements);
        if (context == null || activity == null) return null;
        int i = 0;
        for (String name : elements) {
            CheckBox ckb = new CheckBox(context);
            ckb.setId(i++);
            ckb.setText(name);
            if (!ThemeEngine.isLightTheme(context)) ckb.setButtonTintList(ContextCompat.getColorStateList(context,R.color.redLight));
            ckb.setTextColor(ThemeEngine.getPrimaryTextColor(activity));
            ckb.setPadding(0, 0, 0, 10);
            ckb.setChecked(!InfoManager.filterContains(context, name));
            linearLayout.addView(ckb);
            ckb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                refreshNeeded = true;
                if (isChecked) InfoManager.removeExceptionFromFilter(context, name);
                else InfoManager.addExceptionToFilter(context, name);
            });
        }
        return null;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        CalendarActivity activity = (CalendarActivity) getActivity();
        if (activity == null) return;
        activity.refreshAfterDismiss = refreshNeeded;
        activity.onDismiss(dialog);
    }


}