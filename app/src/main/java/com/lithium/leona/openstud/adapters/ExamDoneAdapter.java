package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lithium.leona.openstud.R;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Exam;
import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.PaymentDescription;
import lithium.openstud.driver.core.Tax;

public class ExamDoneAdapter extends RecyclerView.Adapter<ExamDoneAdapter.ExamDoneHolder> {

    private List<ExamDone> exams;
    private Context context;
    private int mode;

    public ExamDoneAdapter(Context context, List<ExamDone> exams, int mode) {
        this.exams = exams;
        this.context = context;
        this.mode = mode;
    }

    @NonNull
    @Override
    public ExamDoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_exam_completed, parent, false);
        ExamDoneHolder holder = new ExamDoneHolder(view);
        holder.setContext(context);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ExamDoneHolder holder, int position) {
        ExamDone exam = exams.get(position);
        holder.setDetails(exam);
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    public static class ExamDoneHolder extends RecyclerView.ViewHolder  {
        @BindView(R.id.examName) TextView txtName;
        @BindView(R.id.ssdExam) TextView txtSSD;
        @BindView(R.id.cfuExam) TextView txtCFU;
        @BindView(R.id.resultExam) TextView txtResult;
        private int mode;
        private Context context;

        private void setMode(int mode){
            this.mode = mode;
        }

        private void setContext(Context context){
            this.context = context;
        }

        public ExamDoneHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void setDetails(ExamDone exam) {
            txtName.setText(exam.getDescription());
            String result = exam.getNominalResult();
            if(result.equals("30 e lode"))txtResult.setText("30L / 30");
            else txtResult.setText(StringUtils.capitalize(result));
            if(exam.getResult()>=18 || result.equals("idoneo") || exam.isPassed()) {
                if (exam.isCertified()) txtResult.setTextColor(ContextCompat.getColor(context, R.color.green));
                else txtResult.setTextColor(ContextCompat.getColor(context, R.color.yellowLight));
            }
            else txtResult.setTextColor(ContextCompat.getColor(context,R.color.redLight));
            txtCFU.setText(context.getResources().getString(R.string.cfu_exams,String.valueOf(exam.getCfu())));
            txtSSD.setText(context.getResources().getString(R.string.ssd_exams,exam.getSsd()));
        }
    }
}
