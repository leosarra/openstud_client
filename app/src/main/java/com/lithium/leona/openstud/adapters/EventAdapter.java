package com.lithium.leona.openstud.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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
import lithium.openstud.driver.core.Event;
import lithium.openstud.driver.core.ExamReservation;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventHolder> {

    private List<Event> events;
    private Context context;
    private ReservationAdapterListener ral;

    public EventAdapter(Context context, List<Event> events, ReservationAdapterListener ral) {
        this.events = events;
        this.context = context;
        this.ral = ral;
    }

    @NonNull
    @Override
    public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_event, parent, false);
        EventHolder holder = new EventHolder(view);
        holder.setContext(context);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull EventHolder holder, int position) {
        Event res = events.get(position);
        holder.setDetails(res);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)
        TextView txtName;
        private Context context;

        private void setContext(Context context) {
            this.context = context;
        }

        EventHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @SuppressLint("ResourceType")
        void setDetails(final Event ev) {
            txtName.setText(ev.getDescription());
        }
    }

    public interface ReservationAdapterListener {


        void addCalendarOnClick(ExamReservation res);
    }
}
