package com.lithium.leona.openstud.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.lithium.leona.openstud.R;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
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

    class ActiveReservationsHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.nameExam)
        TextView txtName;
        @BindView(R.id.nameTeacher)
        TextView txtTeacher;
        @BindView(R.id.dateExam)
        TextView txtDate;
        @BindView(R.id.reservationNumber)
        TextView txtNumber;
        @BindView(R.id.ssdExam)
        TextView txtSSD;
        @BindView(R.id.cfuExam)
        TextView txtCFU;
        @BindView(R.id.reservationInfo)
        TextView txtInfo;
        @BindView(R.id.get_reservation)
        Button getButton;
        @BindView(R.id.reservation_options)
        ImageView options;
        private Context context;

        private void setContext(Context context) {
            this.context = context;
        }

        ActiveReservationsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @SuppressLint("ResourceType")
        void setDetails(final ExamReservation res) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
            String infos = context.getResources().getString(R.string.description_reservation, res.getNote());
            if (!infos.endsWith(".")) infos = infos + ".";
            txtName.setText(res.getExamSubject());
            txtTeacher.setText(context.getResources().getString(R.string.teacher_reservation, res.getTeacher()));
            txtDate.setText(context.getResources().getString(R.string.date_exam, res.getExamDate().format(formatter)));
            txtNumber.setText(context.getResources().getString(R.string.number_reservation, String.valueOf(res.getReservationNumber())));
            txtSSD.setText(context.getResources().getString(R.string.ssd_exams, res.getSsd()));
            txtCFU.setText(context.getResources().getString(R.string.cfu_exams, String.valueOf(res.getCfu())));
            txtInfo.setText(infos);

            Context wrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
            PopupMenu popup = new PopupMenu(wrapper, options);
            popup.inflate(R.menu.reservation_menu);

            options.setOnClickListener(v -> {
                popup.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.delete_menu:
                            ral.deleteReservationOnClick(res);
                            break;
                        case R.id.calendar_menu:
                            ral.addCalendarOnClick(res);
                            break;
                    }
                    return false;
                });
                //displaying the popup
                popup.show();
            });
            getButton.setOnClickListener(v -> ral.downloadReservationOnClick(res));
        }
    }

    public interface ReservationAdapterListener {
        void deleteReservationOnClick(ExamReservation res);

        void downloadReservationOnClick(ExamReservation res);

        void addCalendarOnClick(ExamReservation res);
    }
}
