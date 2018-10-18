package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.StatsActivity;
import com.lithium.leona.openstud.helpers.ItemTouchHelperAdapter;
import com.lithium.leona.openstud.helpers.ItemTouchHelperViewHolder;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.ExamDone;

public class FakeExamAdapter extends RecyclerView.Adapter<FakeExamAdapter.ExamDoneHolder> implements ItemTouchHelperAdapter {

    private List<ExamDone> exams;
    private StatsActivity activity;

    public FakeExamAdapter(StatsActivity activity, List<ExamDone> exams) {
        this.exams = exams;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ExamDoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.item_row_exam_fake, parent, false);
        ExamDoneHolder holder = new ExamDoneHolder(view);
        holder.setContext(activity);
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

    static class ExamDoneHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        @BindView(R.id.examName)
        TextView txtName;
        @BindView(R.id.cfuExam)
        TextView txtCFU;
        @BindView(R.id.resultExam)
        TextView txtResult;
        private Context context;

        private void setContext(Context context) {
            this.context = context;
        }

        ExamDoneHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setDetails(ExamDone exam) {
            txtName.setText(exam.getDescription());
            String result = String.valueOf(exam.getResult());
            String cfu = String.valueOf(exam.getCfu());
            txtResult.setText(context.getResources().getString(R.string.grade_stats, result));
            txtCFU.setText(context.getResources().getString(R.string.cfu_exams, cfu));
        }

        @Override
        public void onItemSelected() {
        }

        @Override
        public void onItemClear() {
        }
    }


    @Override

    public void onItemDismiss(int position) {
        activity.removeFakeExam(position);
    }


    @Override

    public void onItemMove(int fromPosition, int toPosition) {
        ExamDone prev = exams.remove(fromPosition);
        exams.add(toPosition > fromPosition ? toPosition - 1 : toPosition, prev);
        notifyItemMoved(fromPosition, toPosition);

    }

}
