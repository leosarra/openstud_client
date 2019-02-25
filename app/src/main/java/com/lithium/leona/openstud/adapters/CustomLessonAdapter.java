package com.lithium.leona.openstud.adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.CustomCourse;
import com.lithium.leona.openstud.helpers.LayoutHelper;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalTime;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class CustomLessonAdapter extends RecyclerView.Adapter<CustomLessonAdapter.CustomLessonHolder> {

    private List<CustomCourse.CustomLesson> lessons;
    private CustomLessonListener ocl;
    private Context context;
    private int timePickerTheme;

    public CustomLessonAdapter(Context context, List<CustomCourse.CustomLesson> lessons, int timePickerTheme, CustomLessonListener listener) {
        this.lessons = lessons;
        ocl = listener;
        this.context = context;
        this.timePickerTheme = timePickerTheme;
    }

    @NonNull
    @Override
    public CustomLessonHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row_custom_lesson_week_info, parent, false);
        CustomLessonHolder holder = new CustomLessonHolder(context, view, ocl, timePickerTheme);
        holder.setContext(context);
        return holder;
    }


    @Override
    public int getItemCount() {
        return lessons.size();
    }

    @Override
    public void onBindViewHolder(@NonNull CustomLessonHolder holder, int position) {
        CustomCourse.CustomLesson lesson = lessons.get(position);
        holder.setDetails(lesson);
    }


    public interface CustomLessonListener {
        void delete(CustomCourse.CustomLesson lesson, int position);
    }

    static class CustomLessonHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.dayWeek)
        Spinner dayWeek;
        @BindView(R.id.where)
        AppCompatEditText where;
        @BindView(R.id.startLessonTime)
        TextView startLessonTime;
        @BindView(R.id.endLessonTime)
        TextView endLessonTime;

        @BindView(R.id.startLessonLayout)
        ConstraintLayout layoutStartTime;
        @BindView(R.id.endLessonLayout)
        ConstraintLayout layoutEndTime;

        @BindView(R.id.delete)
        ImageButton delete;
        private Context context;
        private CustomLessonListener ocl;
        private int timePickerTheme;

        CustomLessonHolder(Context context, View itemView, CustomLessonListener ocl, int timePickerTheme) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.ocl = ocl;
            this.context = context;
            this.timePickerTheme = timePickerTheme;
        }

        private void setContext(Context context) {
            this.context = context;
        }

        void setDetails(CustomCourse.CustomLesson lesson) {
            dayWeek.getBackground().setColorFilter(LayoutHelper.getColorByAttr(context, R.attr.primaryTextColor, android.R.color.darker_gray), PorterDuff.Mode.SRC_ATOP);
            dayWeek.setSelection(lesson.getDayOfWeek().getValue() - 1);
            startLessonTime.setText(String.format("%02d:%02d", lesson.getStart().getHour(), lesson.getStart().getMinute()));
            endLessonTime.setText(String.format("%02d:%02d", lesson.getEnd().getHour(), lesson.getEnd().getMinute()));
            where.setText(lesson.getWhere());
            delete.setOnClickListener(v -> ocl.delete(lesson, getAdapterPosition()));
            dayWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    lesson.setDayOfWeek(DayOfWeek.of(position + 1));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            where.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    lesson.setWhere(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });



            TimePickerDialog.OnTimeSetListener endSetListener = (view, hourOfDay, minute) -> {
                endLessonTime.setText(String.format("%02d:%02d", hourOfDay, minute));
                lesson.setEnd(LocalTime.of(hourOfDay, minute, 0));
            };

            TimePickerDialog.OnTimeSetListener startSetListener = (view, hourOfDay, minute) -> {
                startLessonTime.setText(String.format("%02d:%02d", hourOfDay, minute));
                lesson.setStart(LocalTime.of(hourOfDay, minute, 0));
                if (lesson.getStart().isAfter(lesson.getEnd())) endSetListener.onTimeSet(null, hourOfDay+1, minute);
            };

            layoutStartTime.setOnClickListener(v -> {
                TimePickerDialog dialog =
                        new TimePickerDialog(context, timePickerTheme,startSetListener, 0, 0, DateFormat.is24HourFormat(context));
                dialog.show();
            });
            layoutEndTime.setOnClickListener(v -> {
                TimePickerDialog dialog =
                        new TimePickerDialog(context, timePickerTheme, endSetListener, 0, 0, DateFormat.is24HourFormat(context));
                dialog.show();
            });
        }

    }

}
