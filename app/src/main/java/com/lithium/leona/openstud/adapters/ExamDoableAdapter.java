package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.lithium.leona.openstud.R;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.ExamDoable;
import lithium.openstud.driver.core.ExamPassed;
import lithium.openstud.driver.core.ExamReservation;

public class ExamDoableAdapter extends RecyclerView.Adapter<ExamDoableAdapter.ExamDoableHolder> {

    private List<ExamDoable> exams;
    private Context context;
    private ExamDoableAdapterListener edal;

    public ExamDoableAdapter(Context context, List<ExamDoable> exams, ExamDoableAdapterListener edal) {
        this.exams = exams;
        this.context = context;
        this.edal = edal;
    }

    @NonNull
    @Override
    public ExamDoableHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_exam_doable, parent, false);
        ExamDoableHolder holder = new ExamDoableHolder(view);
        holder.setContext(context);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ExamDoableHolder holder, int position) {
        ExamDoable exam = exams.get(position);
        holder.setDetails(exam);
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    public class ExamDoableHolder extends RecyclerView.ViewHolder  {
        @BindView(R.id.examName) TextView txtName;
        @BindView(R.id.ssdExam) TextView txtSSD;
        @BindView(R.id.cfuExam) TextView txtCFU;
        @BindView(R.id.show_sessions) Button showSessions;
        private Context context;

        private void setContext(Context context){
            this.context = context;
        }

        public ExamDoableHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void setDetails(final ExamDoable exam) {
            txtName.setText(exam.getDescription());
            txtCFU.setText(context.getResources().getString(R.string.cfu_exams,String.valueOf(exam.getCfu())));
            txtSSD.setText(context.getResources().getString(R.string.ssd_exams,exam.getSsd()));
            showSessions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    edal.showSessionsOnClick(exam);
                }
            });

        }
    }

    public interface ExamDoableAdapterListener {
        void showSessionsOnClick(ExamDoable exam);
    }
}
