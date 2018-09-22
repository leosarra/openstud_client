package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.lithium.leona.openstud.R;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Period;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.ExamPassed;
import lithium.openstud.driver.core.ExamReservation;

public class ActiveReservationsAdapter extends RecyclerView.Adapter<ActiveReservationsAdapter.ActiveReservationsHolder> {

    private List<ExamReservation> reservations;
    private Context context;
    private ReservationAdapterListener ral;

    public ActiveReservationsAdapter(Context context, List<ExamReservation> reservations, ReservationAdapterListener ral) {
        this.reservations = reservations;
        this.context = context;
        this.ral = ral;
    }

    @NonNull
    @Override
    public ActiveReservationsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_active_reservation, parent, false);
        ActiveReservationsHolder holder = new ActiveReservationsHolder(view);
        holder.setContext(context);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveReservationsHolder holder, int position) {
        ExamReservation res = reservations.get(position);
        holder.setDetails(res);
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public class ActiveReservationsHolder extends RecyclerView.ViewHolder  {
        @BindView(R.id.nameExam) TextView txtName;
        @BindView(R.id.nameTeacher) TextView txtTeacher;
        @BindView(R.id.dateExam) TextView txtDate;
        @BindView(R.id.reservationNumber) TextView txtNumber;
        @BindView(R.id.ssdExam) TextView txtSSD;
        @BindView(R.id.cfuExam) TextView txtCFU;
        @BindView(R.id.reservationInfo) TextView txtInfo;
        @BindView(R.id.delete_reservation) Button deleteButton;
        @BindView(R.id.get_reservation) Button getButton;
        private Context context;

        private void setContext(Context context){
            this.context = context;
        }

        public ActiveReservationsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void setDetails(final ExamReservation res) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
            String infos = context.getResources().getString(R.string.description_reservation, res.getNote());
            if (!infos.endsWith(".")) infos = infos + ".";
            txtName.setText(res.getExamSubject());
            txtTeacher.setText(context.getResources().getString(R.string.teacher_reservation, res.getTeacher()));
            txtDate.setText(context.getResources().getString(R.string.date_exam,res.getExamDate().format(formatter)));
            txtNumber.setText(context.getResources().getString(R.string.number_reservation, String.valueOf(res.getReservationNumber())));
            txtSSD.setText(context.getResources().getString(R.string.ssd_exams, res.getSsd()));
            txtCFU.setText(context.getResources().getString(R.string.cfu_exams, String.valueOf(res.getCfu())));
            txtInfo.setText(infos);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ral.deleteReservationOnClick(res);
                }
            });
            getButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ral.downloadReservationOnClick(res);
                }
            });
            int tintColor;
            if (Period.between(res.getEndDate(), LocalDate.from(LocalDateTime.now())).getDays()>1) {
                tintColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                deleteButton.setEnabled(false);
            }
            else tintColor = ContextCompat.getColor(context, R.color.redSapienza);
            deleteButton.setTextColor(tintColor);
            Drawable drawable = ContextCompat.getDrawable(context,R.drawable.ic_delete_small);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(),tintColor);
            deleteButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
    }

    public interface ReservationAdapterListener {
        void deleteReservationOnClick(ExamReservation res);

        void downloadReservationOnClick(ExamReservation res);
    }
}
