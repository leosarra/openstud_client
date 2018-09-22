package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Period;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.ExamReservation;

public class AvaiableReservationsAdapter extends RecyclerView.Adapter<AvaiableReservationsAdapter.ActiveReservationsHolder> {

    private List<ExamReservation> reservations;
    private Context context;
    private ReservationAdapterListener ral;

    public AvaiableReservationsAdapter(Context context, List<ExamReservation> reservations, ReservationAdapterListener ral) {
        this.reservations = reservations;
        this.context = context;
        this.ral = ral;
    }

    @NonNull
    @Override
    public ActiveReservationsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_avaiable_reservation, parent, false);
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
        @BindView(R.id.channelReservation) TextView txtChannel;
        @BindView(R.id.infosExtra) TextView txtInfo;
        @BindView(R.id.startDate) TextView txtStartDate;
        @BindView(R.id.endDate) TextView txtEndDate;
        @BindView(R.id.place_reservation) Button placeButton;
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
            txtStartDate.setText(context.getResources().getString(R.string.start_date_reservation,res.getStartDate().format(formatter)));
            txtEndDate.setText(context.getResources().getString(R.string.end_date_reservation,res.getEndDate().format(formatter)));
            txtDate.setText(context.getResources().getString(R.string.date_exam,res.getExamDate().format(formatter)));
            txtChannel.setText(context.getResources().getString(R.string.channel_reservation, res.getChannel()));
            if (res.getNote() == null || res.getNote().isEmpty()) txtInfo.setVisibility(View.GONE);
            else {
                txtInfo.setText(context.getResources().getString(R.string.description_reservation,res.getNote()));
            }
            int tintColor;
            if (!(Period.between(res.getStartDate(), LocalDate.from(LocalDateTime.now())).getDays()>=0) || Period.between(res.getEndDate(), LocalDate.from(LocalDateTime.now())).getDays()>=1){
                tintColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                placeButton.setEnabled(false);
            }
            else tintColor = ContextCompat.getColor(context, R.color.redSapienza);
            placeButton.setTextColor(tintColor);
            Drawable drawable = ContextCompat.getDrawable(context,R.drawable.ic_library_add_small);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(),tintColor);
            placeButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            placeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ral.placeReservation(res);
                }
            });
        }
    }

    public interface ReservationAdapterListener {
        void placeReservation(ExamReservation res);
    }
}
