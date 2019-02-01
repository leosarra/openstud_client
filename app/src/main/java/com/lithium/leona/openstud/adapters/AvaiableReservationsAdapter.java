package com.lithium.leona.openstud.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.models.ExamReservation;

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

    private boolean existActiveReservations(ExamReservation res) {
        if (activeReservations == null) return true;
        for (ExamReservation active : activeReservations) {
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

    public interface ReservationAdapterListener {
        boolean placeReservation(ExamReservation res);
    }

    class ActiveReservationsHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.nameExam)
        TextView txtName;
        @BindView(R.id.nameTeacher)
        TextView txtTeacher;
        @BindView(R.id.dateExam)
        TextView txtDate;
        @BindView(R.id.channelReservation)
        TextView txtChannel;
        @BindView(R.id.infosExtra)
        TextView txtInfo;
        @BindView(R.id.startDate)
        TextView txtStartDate;
        @BindView(R.id.endDate)
        TextView txtEndDate;
        @BindView(R.id.academicYear)
        TextView txtYear;
        @BindView(R.id.place_reservation)
        Button placeButton;
        private Activity activity;

        ActiveReservationsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        private void setActivity(Activity activity) {
            this.activity = activity;
        }

        private void setPlaceButtonEnabled(boolean enabled, boolean alreadyExist) {
            int tintColor;
            Drawable placeDrawable;
            if (alreadyExist) {
                placeDrawable = LayoutHelper.getDrawableWithColorAttr(activity,R.drawable.ic_check_black_24dp,R.attr.certifiedExamColor, android.R.color.darker_gray);
                placeButton.setText(activity.getResources().getString(R.string.already_placed_button));
                tintColor = LayoutHelper.getColorByAttr(activity, R.attr.certifiedExamColor, android.R.color.darker_gray);
            } else if (!enabled) {
                placeDrawable = LayoutHelper.getDrawableWithColorId(activity, R.drawable.ic_library_add_small, android.R.color.darker_gray);
                placeButton.setText(activity.getResources().getString(R.string.place_reservation));
                tintColor = ContextCompat.getColor(activity, android.R.color.darker_gray);
            }
            else {
                placeDrawable = LayoutHelper.getDrawableWithColorAttr(activity,R.drawable.ic_library_add_small,R.attr.colorButtonNav, android.R.color.darker_gray);
                placeButton.setText(activity.getResources().getString(R.string.place_reservation));
                tintColor = LayoutHelper.getColorByAttr(activity, R.attr.colorButtonNav, android.R.color.darker_gray);
            }
            placeButton.setEnabled(enabled);
            placeButton.setTextColor(tintColor);
            if (placeDrawable != null) placeButton.setCompoundDrawablesWithIntrinsicBounds(placeDrawable, null, null, null);
        }

        @SuppressLint("ResourceType")
        void setDetails(final ExamReservation res) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
            String infos = activity.getResources().getString(R.string.description_reservation, res.getNote());
            if (!infos.endsWith(".")) infos = infos + ".";
            txtName.setText(res.getExamSubject());
            txtTeacher.setText(activity.getResources().getString(R.string.teacher_reservation, res.getTeacher()));
            txtDate.setText(activity.getResources().getString(R.string.date_exam, res.getExamDate().format(formatter)));
            txtStartDate.setText(activity.getResources().getString(R.string.start_date_reservation, res.getStartDate().format(formatter)));
            txtEndDate.setText(activity.getResources().getString(R.string.end_date_reservation, res.getEndDate().format(formatter)));
            txtDate.setText(activity.getResources().getString(R.string.date_exam, res.getExamDate().format(formatter)));
            txtChannel.setText(activity.getResources().getString(R.string.channel_reservation, res.getChannel()));
            if(res.getYearCourse()!= null) {
                txtYear.setVisibility(View.VISIBLE);
                txtYear.setText(activity.getResources().getString(R.string.accademic_year_pay, res.getYearCourse()));
            }
            else txtYear.setVisibility(View.GONE);
            if (res.getNote() == null || res.getNote().trim().isEmpty())
                txtInfo.setVisibility(View.GONE);
            else {
                txtInfo.setText(infos);
            }
            if (existActiveReservations(res)) {
                setPlaceButtonEnabled(false, true);
            } else if (!ClientHelper.canPlaceReservation(res)) setPlaceButtonEnabled(false, false);
            else setPlaceButtonEnabled(true, false);
            placeButton.setOnClickListener(v -> new Thread(() -> {
                if (ral.placeReservation(res))
                    activity.runOnUiThread(() -> setPlaceButtonEnabled(false, true));
                else activity.runOnUiThread(() -> setPlaceButtonEnabled(true, false));
            }).start());
        }
    }
}
