package com.lithium.leona.openstud.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.EventTheatreAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.mikepenz.materialdrawer.Drawer;

import org.threeten.bp.ZoneId;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Event;
import lithium.openstud.driver.core.models.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class EventsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView)
    RecyclerView rv;
    @BindView(R.id.empty_layout)
    LinearLayout emptyView;
    @BindView(R.id.empty_button_reload)
    Button emptyButton;
    @BindView(R.id.empty_text)
    TextView emptyText;
    @BindView(R.id.constraintLayout)
    ConstraintLayout constraintLayout;
    private Drawer drawer;
    private HorizontalCalendar horizontalCalendar;
    private Calendar defaultDate;
    private Openstud os;
    private Student student;
    private List<Event> selectedDateEvents = new LinkedList<>();
    private List<Event> events = new LinkedList<>();
    private EventTheatreAdapter adapter;
    private EventHandler h = new EventHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyClassroomTimetableTheme(this);
        setContentView(R.layout.activity_classroom_timetable);
        ButterKnife.bind(this);
        os = InfoManager.getOpenStud(getApplication());
        student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) ClientHelper.rebirthApp(this);
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_YEAR, 7);
        defaultDate = setTodayLessonFromBundle(savedInstanceState);

        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        emptyText.setText(getResources().getString(R.string.no_events));
        drawer = LayoutHelper.applyDrawer(this, toolbar, student);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        Activity activity = this;
        createCalendar(startDate, endDate);
        adapter = new EventTheatreAdapter(this, selectedDateEvents, new EventTheatreAdapter.EventAdapterListener() {
            @Override
            public void addCalendarOnClick(Event ev) {
                ClientHelper.addEventToCalendar(activity, ev);
            }

            @Override
            public void onItemClick(View v) {
                int itemPosition = rv.getChildLayoutPosition(v);
                if (itemPosition < selectedDateEvents.size()) {
                    Event ev = selectedDateEvents.get(itemPosition);
                    ClientHelper.createCustomTab(activity, ev.getUrl());
                }
            }
        });
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.events);
        rv.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(() -> refreshEvents(horizontalCalendar.getSelectedDate(), true));
        emptyButton.setOnClickListener(v -> {
            if (!swipeRefreshLayout.isRefreshing())
                refreshEvents(horizontalCalendar.getSelectedDate(), true);
        });
        List<Event> cached_events = InfoManager.getEventsUniversityCached(this, os);
        swapViews(true);
        if (cached_events != null && !cached_events.isEmpty()) {
            events.clear();
            events.addAll(cached_events);
            refreshEvents(defaultDate, false);
        }
        if (savedInstanceState == null) refreshEvents(defaultDate, true);
    }

    private void refreshEvents(Calendar date, boolean refresh) {
        new Thread(() -> {
                synchronized (this){
                    try {
                        if (refresh) {
                            setRefreshing(true);
                            List<Event> newEvents = InfoManager.getEventsUniversity(this, os);
                            if (newEvents != null && !newEvents.equals(events)) {
                                events.clear();
                            }
                            setRefreshing(false);
                        }
                    } catch (OpenstudConnectionException e) {
                        e.printStackTrace();
                        h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                    } catch (OpenstudInvalidResponseException e) {
                        e.printStackTrace();
                        h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                    }
                    applyEvents(date);
                }
        }).start();
    }

    private void applyEvents(Calendar date) {
        List<Event> eventDate = new LinkedList<>();
        for (Event ev : events) {
            if (ev.getStart() != null && date.getTimeInMillis() == ev.getStart().toLocalDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                eventDate.add(ev);
        }
        selectedDateEvents.clear();
        selectedDateEvents.addAll(eventDate);
        if (selectedDateEvents.isEmpty()) swapViews(true);
        else swapViews(false);
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void setRefreshing(boolean bool) {
        runOnUiThread(() -> swipeRefreshLayout.setRefreshing(bool));
    }

    private void swapViews(boolean empty) {
        runOnUiThread(() -> {
            if (empty) {
                emptyView.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        });
    }

    private Calendar setTodayLessonFromBundle(Bundle savedInstance) {
        if (savedInstance != null) {
            String jsonDate = savedInstance.getString("currentDate", null);
            Gson gson = new Gson();
            Type typeCalendar = new TypeToken<Calendar>() {
            }.getType();
            return gson.fromJson(jsonDate, typeCalendar);
        } else {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            return today;
        }
    }

    public void onSaveInstanceState(Bundle savedInstance) {
        Gson gson = new Gson();
        Type typeCalendar = new TypeToken<Calendar>() {
        }.getType();
        String jsonCalendar = gson.toJson(horizontalCalendar.getSelectedDate(), typeCalendar);
        savedInstance.putString("currentDate", jsonCalendar);
        super.onSaveInstanceState(savedInstance);
    }


    private void createCalendar(Calendar start, Calendar end) {
        Activity activity = this;
        runOnUiThread(() -> {
            horizontalCalendar = new HorizontalCalendar.Builder(activity, R.id.calendarView)
                    .range(start, end)
                    .datesNumberOnScreen(5)
                    .defaultSelectedDate(defaultDate).build();
            horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
                @Override
                public void onDateSelected(Calendar date, int position) {
                    refreshEvents(date, false);
                }
            });
        });
    }

    private static class EventHandler extends Handler {
        private final WeakReference<EventsActivity> activity;

        private EventHandler(EventsActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final EventsActivity activity = this.activity.get();
            if (activity == null) return;
            View.OnClickListener ocl = v -> activity.refreshEvents(activity.horizontalCalendar.getSelectedDate(), true);
            if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                LayoutHelper.createActionSnackBar(activity.constraintLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                LayoutHelper.createActionSnackBar(activity.constraintLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            }
        }
    }

}
