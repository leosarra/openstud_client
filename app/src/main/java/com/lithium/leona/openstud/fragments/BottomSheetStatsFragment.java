package com.lithium.leona.openstud.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.StatsActivity;
import com.lithium.leona.openstud.adapters.DropdownExamAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.warkiz.widget.IndicatorSeekBar;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.OpenstudHelper;
import lithium.openstud.driver.core.models.Exam;
import lithium.openstud.driver.core.models.ExamDoable;
import lithium.openstud.driver.core.models.ExamDone;

public class BottomSheetStatsFragment extends BottomSheetDialogFragment {
    @BindView(R.id.exam_name)
    AutoCompleteTextView examName;
    @BindView(R.id.cfu)
    IndicatorSeekBar cfu;
    @BindView(R.id.grade)
    IndicatorSeekBar grade;
    @BindView(R.id.add)
    Button add;
    private List<ExamDoable> examsDoable = new LinkedList<>();

    public BottomSheetStatsFragment() {
        // Required empty public constructor
    }

    public static BottomSheetStatsFragment newInstance(List<ExamDoable> exams) {
        BottomSheetStatsFragment myFragment = new BottomSheetStatsFragment();
        if (exams != null) {
            Bundle args = new Bundle();
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<List<ExamDoable>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDoable.class));
            try {
                args.putString("doable", jsonAdapter.toJson(exams));
                myFragment.setArguments(args);
            } catch (JsonDataException e) {
                e.printStackTrace();
            }
        }
        return myFragment;
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
        Bundle bdl = getArguments();
        Moshi moshi = new Moshi.Builder().build();
        if (bdl != null) {
            String examsJson = bdl.getString("doable", null);
            if (examsJson != null) {
                JsonAdapter<List<ExamDoable>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDoable.class));
                try {
                    List<ExamDoable> examsBundle = jsonAdapter.fromJson(examsJson);
                    if (examsBundle!=null) examsDoable.addAll(examsBundle);
                } catch (JsonDataException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.add_exam_stats, container, false);
        ButterKnife.bind(this, v);
        ClientHelper.setDialogView(v, getDialog(), BottomSheetBehavior.STATE_EXPANDED);
        Context context = getContext();
        if (context == null) {
            dismiss();
            return null;
        }
        int tintColorEnabled = LayoutHelper.getColorByAttr(context, R.attr.colorButtonNav, R.color.redSapienza);
        add.setEnabled(false);
        add.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        DropdownExamAdapter adapter = new DropdownExamAdapter(context, (List<Exam>) (Object) examsDoable);
        filterExamsDoable(getContext());
        examName.setThreshold(2);
        examName.setAdapter(adapter);
        examName.setOnItemClickListener((parent, view, position, id) -> {
            Exam exam = adapter.getItem(position);
            if (exam != null) {
                examName.setText(exam.getDescription());
                cfu.setProgress(exam.getCfu());
            }
        });

        examName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (text.trim().length() == 0) {
                    add.setEnabled(false);
                    add.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
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
                ClientHelper.hideKeyboard(v, context);
                return true;
            }
            return false;
        });
        return null;
    }

    private void filterExamsDoable(Context context) {
        List<ExamDone> fakeExams = InfoManager.getFakeExams(context, InfoManager.getOpenStud(context));
        if (fakeExams == null) return;
        List<ExamDoable> remove = new LinkedList<>();
        for (ExamDone fake : fakeExams) {
            for (ExamDoable doable : examsDoable) {
                if (doable.getDescription().toLowerCase().equals(fake.getDescription().toLowerCase())) {
                    remove.add(doable);
                    break;
                }
            }
        }
        examsDoable.removeAll(remove);
    }
}