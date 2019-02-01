package com.lithium.leona.openstud.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.StatsActivity;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.warkiz.widget.IndicatorSeekBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.OpenstudHelper;

public class BottomSheetStatsFragment extends BottomSheetDialogFragment {
    @BindView(R.id.exam_name)
    EditText examName;
    @BindView(R.id.cfu)
    IndicatorSeekBar cfu;
    @BindView(R.id.grade)
    IndicatorSeekBar grade;
    @BindView(R.id.add)
    Button add;

    public BottomSheetStatsFragment() {
        // Required empty public constructor
    }

    @OnClick(R.id.abort)
    public void hide() {
        dismiss();
    }

    @OnClick(R.id.add)
    public void createExam() {
        StatsActivity activity = (StatsActivity) getActivity();
        if (activity != null) {
            activity.addFakeExam(OpenstudHelper.createFakeExamDone(examName.getText().toString().trim(), cfu.getProgress(), grade.getProgress()));
            dismiss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.add_exam_stats, container, false);
        ButterKnife.bind(this, v);
        ClientHelper.setDialogView(v, getDialog(), BottomSheetBehavior.STATE_EXPANDED);
        int tintColorEnabled = LayoutHelper.getColorByAttr(getContext(), R.attr.colorButtonNav, R.color.redSapienza);
        add.setEnabled(false);
        add.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        examName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (text.trim().length() == 0) {
                    add.setEnabled(false);
                    add.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
                } else {
                    add.setEnabled(true);
                    add.setTextColor(tintColorEnabled);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        examName.setOnKeyListener((v1, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                ClientHelper.hideKeyboard(v, getContext());
                return true;
            }
            return false;
        });
        return null;
    }

}