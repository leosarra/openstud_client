package com.lithium.leona.openstud.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
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
    private Activity activity;
    private ReservationAdapterListener ral;
    private List<ExamReservation> activeReservations;

    public AvaiableReservationsAdapter(Activity activity, List<ExamReservation> reservations, List<ExamReservation> activeReservations, ReservationAdapterListener ral) {
        this.reservations = reservations;
        this.activity = activity;
        this.ral = ral;
        this.activeReservations = activeReservations;
    }

    private boolean existActiveReservations(ExamReservation res){
        if (activeReservations == null) return true;
        for (ExamReservation active : activeReservations){
            if (res.getExamSubject().equals(active.getExamSubject()) && res.getChannel().equals(active.getChannel())
                    && res.getCourseCode() == active.getCourseCode() && res.getReportID() == active.getReportID()
                    && res.getSessionID() == active.getSessionID()) return true;
        }
        return false;
    }

    @NonNull
    @Override
    public ActiveReservationsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.item_row_avaiable_reservation, parent, false);
        ActiveReservationsHolder holder = new ActiveReservationsHolder(view);
        holder.setActivity(activity);
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
        private Activity activity;

        private void setActivity(Activity activity){
            this.activity = activity;
        }

        public ActiveReservationsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }


        private void setPlaceButtonEnabled(boolean enabled){
            int tintColor;
            if (!enabled)
                tintColor = ContextCompat.getColor(activity, android.R.color.darker_gray);
            else {
                TypedValue tV = new TypedValue();
                Resources.Theme theme = activity.getTheme();
                boolean success = theme.resolveAttribute(R.attr.colorButtonNav, tV, true);
                if (success) tintColor = tV.data;
                else tintColor = ContextCompat.getColor(activity, R.color.redSapienza);
            }
            placeButton.setEnabled(enabled);
            placeButton.setTextColor(tintColor);

            Drawable drawable = ContextCompat.getDrawable(activity,R.drawable.ic_library_add_small);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(),tintColor);
            placeButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }

        @SuppressLint("ResourceType")
        public void setDetails(final ExamReservation res) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
            String infos = activity.getResources().getString(R.string.description_reservation, res.getNote());
            if (!infos.endsWith(".")) infos = infos + ".";
            txtName.setText(res.getExamSubject());
            txtTeacher.setText(activity.getResources().getString(R.string.teacher_reservation, res.getTeacher()));
            txtDate.setText(activity.getResources().getString(R.string.date_exam,res.getExamDate().format(formatter)));
            txtStartDate.setText(activity.getResources().getString(R.string.start_date_reservation,res.getStartDate().format(formatter)));
            txtEndDate.setText(activity.getResources().getString(R.string.end_date_reservation,res.getEndDate().format(formatter)));
            txtDate.setText(activity.getResources().getString(R.string.date_exam,res.getExamDate().format(formatter)));
            txtChannel.setText(activity.getResources().getString(R.string.channel_reservation, res.getChannel()));
            if (res.getNote() == null || res.getNote().isEmpty()) txtInfo.setVisibility(View.GONE);
            else {
                txtInfo.setText(activity.getResources().getString(R.string.description_reservation,res.getNote()));
            }
            if (existActiveReservations(res) || !(Period.between(res.getStartDate(), LocalDate.from(LocalDateTime.now())).getDays()>=0) || Period.between(res.getEndDate(), LocalDate.from(LocalDateTime.now())).getDays()>=1)
                setPlaceButtonEnabled(false);
            else setPlaceButtonEnabled(true);
            placeButton.setOnClickListener(v -> new Thread(() -> {
                if (ral.placeReservation(res)) activity.runOnUiThread(() -> setPlaceButtonEnabled(false));
                else activity.runOnUiThread(() -> setPlaceButtonEnabled(true));
            }).start());
        }
    }

    public interface ReservationAdapterListener {
        boolean placeReservation(ExamReservation res);
    }
}
