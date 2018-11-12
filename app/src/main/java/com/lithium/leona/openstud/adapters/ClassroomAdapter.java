package com.lithium.leona.openstud.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.ItemTouchHelperViewHolder;
import com.lithium.leona.openstud.helpers.LayoutHelper;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Classroom;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.ClassesHolder>  {

    private List<Classroom> classes;
    private Context context;
    private View view;
    public ClassroomAdapter(View view, Context context, List<Classroom> classes) {
        this.classes = classes;
        this.context = context;
        this.view = view;
    }

    @NonNull
    @Override
    public ClassesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_classroom_details, parent, false);
        ClassesHolder holder = new ClassesHolder(view);
        holder.setContext(context);
        holder.setView(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ClassesHolder holder, int position) {
        Classroom room = classes.get(position);
        holder.setDetails(room);
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ClassesHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        @BindView(R.id.className)
        TextView txtName;
        @BindView(R.id.whereClassroom)
        TextView txtWhere;
        @BindView(R.id.statusClassroom)
        TextView txtStatus;
        @BindView(R.id.lesson)
        TextView txtLesson;
        @BindView(R.id.nextLesson)
        TextView txtNextLesson;
        @BindView(R.id.open_map)
        Button openMap;
        @BindView(R.id.open_timetable)
        Button openTimetable;
        private Context context;
        private View view;

        private void setContext(Context context) {
            this.context = context;
        }
        private void setView(View view) {
            this.view = view;
        }
        ClassesHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setDetails(Classroom room) {
            txtName.setText(room.getName());
            txtWhere.setText(context.getResources().getString(R.string.position, room.getWhere()));
            String status;
            int tintColor;
            TypedValue tV = new TypedValue();
            Resources.Theme theme = context.getTheme();
            if (room.isOccupied()) {
                boolean success = theme.resolveAttribute(R.attr.nonCertifiedExamColor, tV, true);
                if (success) tintColor = tV.data;
                else tintColor = ContextCompat.getColor(context, R.color.green);
                status = context.getResources().getString(R.string.not_available);
            }
            else {
                boolean success = theme.resolveAttribute(R.attr.certifiedExamColor, tV, true);
                if (success) tintColor = tV.data;
                else tintColor = ContextCompat.getColor(context, R.color.red);
                status = context.getResources().getString(R.string.available);
            }

            String statusPre = context.getResources().getString(R.string.status_classroom);
            Spannable spannable = new SpannableString(statusPre+status);
            spannable.setSpan(new ForegroundColorSpan(tintColor), statusPre.length(), (statusPre + status).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), statusPre.length(), (statusPre + status).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            txtStatus.setText(spannable, TextView.BufferType.SPANNABLE);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
            if (room.getLessonNow() != null)
                txtLesson.setText(context.getResources().getString(R.string.lesson_now, room.getLessonNow().getName()));
            else txtLesson.setVisibility(View.GONE);
            if (room.getNextLesson() != null)
                txtNextLesson.setText(context.getResources().getString(R.string.next_lesson, room.getNextLesson().getStart().format(formatter), room.getNextLesson().getName()));
            else txtNextLesson.setVisibility(View.GONE);

            int tintColorMap;
            if(!room.hasCoordinates()) {
                tintColorMap = ContextCompat.getColor(context, android.R.color.darker_gray);
                openMap.setEnabled(false);
            }
            else {
                TypedValue tv2 = new TypedValue();
                boolean success = theme.resolveAttribute(R.attr.colorButtonNav, tv2, true);
                if (success) tintColorMap = tv2.data;
                else tintColorMap = ContextCompat.getColor(context, R.color.redSapienza);
            }
            openMap.setTextColor(tintColorMap);
            Drawable drawable = ContextCompat.getDrawable(context,R.drawable.ic_map_black_24dp);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(),tintColorMap);
            openMap.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            openMap.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q="+room.getLatitude()+","+room.getLongitude()+"("+room.getName()+")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                LayoutHelper.createTextSnackBar(view, R.string.no_map_app, Snackbar.LENGTH_LONG);
                if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(mapIntent);
                }
                else {
                    LayoutHelper.createTextSnackBar(view, R.string.no_map_app, Snackbar.LENGTH_LONG);
                }
            });
        }

        @Override
        public void onItemSelected() {
        }

        @Override
        public void onItemClear() {
        }
    }

}
