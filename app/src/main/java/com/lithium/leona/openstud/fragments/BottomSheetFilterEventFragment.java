package com.lithium.leona.openstud.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.widget.CompoundButtonCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.CalendarActivity;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<List<String>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, String.class));
        String json = jsonAdapter.toJson(names);
        args.putSerializable("filter_elements", json);
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
        Moshi moshi = new Moshi.Builder().build();
        if (bdl != null) {
            JsonAdapter<List<String>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, String.class));
            String json = bdl.getString("filter_elements", null);
            if (json != null) {
                List<String> passedElements = null;
                try {
                    passedElements = jsonAdapter.fromJson(json);
                } catch (JsonDataException | IOException e) {
                    e.printStackTrace();
                }
                if (passedElements!=null) {
                    elements.clear();
                    elements.addAll(passedElements);
                }
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
        InfoManager.removeOldEntriesFilter(context, new LinkedList<>(elements));
        if (context == null || activity == null) return null;
        int i = 0;
        for (String name : elements) {
            CheckBox ckb = new CheckBox(context);
            ckb.setId(i++);
            ckb.setText(name);
            if (!ThemeEngine.isLightTheme(context)) {
                ColorStateList colorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_checked}, // unchecked
                                new int[]{android.R.attr.state_checked}, // checked
                        },
                        new int[]{
                                context.getColor(android.R.color.darker_gray),
                                context.getColor(R.color.redLight),
                        }
                );
                CompoundButtonCompat.setButtonTintList(ckb, colorStateList);
            }
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