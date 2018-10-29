package com.lithium.leona.openstud.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.LoginActivity;
import com.lithium.leona.openstud.helpers.ClientHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BottomSheetRecoveryFragment extends BottomSheetDialogFragment {
    public BottomSheetRecoveryFragment() {
        // Required empty public constructor
    }

    String questionText;
    String studentID;
    @BindView(R.id.questionInput)
    EditText questionInput;

    @BindView(R.id.question)
    TextView question;

    @BindView(R.id.confirm)
    Button confirm;

    @OnClick(R.id.abort)
    public void hide() {
        dismiss();
    }

    @OnClick(R.id.confirm)
    public void add() {
        LoginActivity activity = (LoginActivity) getActivity();
        if (activity != null) {
            new Thread(() -> activity.sendRecoveryRequest(questionInput.getText().toString(), studentID)).start();
            dismiss();
        }
    }

    public static BottomSheetRecoveryFragment newInstance(String studentID, String question) {
        BottomSheetRecoveryFragment myFragment = new BottomSheetRecoveryFragment();
        Bundle args = new Bundle();
        String questionText = question;
        if (!questionText.endsWith(":") && !questionText.endsWith("?")) {
            questionText = questionText + ":";
        }
        args.putString("question", questionText);
        args.putString("id", studentID);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bdl = getArguments();
        if (bdl != null) {
            questionText = bdl.getString("question", null);
            studentID = bdl.getString("id", null);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.recovery_password, container, false);
        ButterKnife.bind(this, v);

        if (questionText == null || studentID == null) dismiss();
        else question.setText(questionText);

        int tintColorEnabled;
        TypedValue tV = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        boolean success = theme.resolveAttribute(R.attr.colorButtonNav, tV, true);
        if (success) tintColorEnabled = tV.data;
        else tintColorEnabled = ContextCompat.getColor(getContext(), R.color.redSapienza);
        confirm.setEnabled(false);
        confirm.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        questionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 4) {
                    confirm.setEnabled(false);
                    confirm.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
                } else {
                    confirm.setEnabled(true);
                    confirm.setTextColor(tintColorEnabled);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        questionInput.setOnKeyListener((v1, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                ClientHelper.hideKeyboard(v, getContext());
                return true;
            }
            return false;
        });
        return v;
    }

}