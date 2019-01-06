package com.lithium.leona.openstud.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.EventAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.fragments.BottomSheetFilterEventFragment;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import org.apache.commons.lang3.tuple.Pair;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.EventType;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class CalendarActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener, DialogInterface.OnDismissListener {


    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
    public boolean refreshAfterDismiss = false;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.app_bar_layout)
    AppBarLayout appBarLayout;
    @BindView(R.id.compactcalendar_view)
    CompactCalendarView compactCalendarView;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.date_picker_arrow)
    ImageView arrow;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.lessons_rv)
    RecyclerView lessons_rv;
    @BindView(R.id.exams_doable_rv)
    RecyclerView exams_rv;
    @BindView(R.id.reservations_rv)
    RecyclerView reservations_rv;
    @BindView(R.id.empty_layout)
    LinearLayout emptyView;
    @BindView(R.id.empty_text)
    TextView emptyText;
    @BindView(R.id.empty_container)
    LinearLayout emptyContainer;
    @BindView(R.id.empty_button_reload)
    Button emptyButton;
    private DelayedDrawerListener ddl;
    private boolean isExpanded = false;
    private CalendarEventHandler h = new CalendarEventHandler(this);
    private Openstud os;
    private Student student;
    private EventAdapter adapter_lessons;
    private EventAdapter adapter_exams;
    private EventAdapter adapter_reservations;
    private List<lithium.openstud.driver.core.Event> events = new LinkedList<>();
    private List<lithium.openstud.driver.core.Event> lessons = new LinkedList<>();
    private List<lithium.openstud.driver.core.Event> exams = new LinkedList<>();
    private List<lithium.openstud.driver.core.Event> reservations = new LinkedList<>();
    private Date currentDate;
    private boolean lessonOptionsEnabled;
    private boolean lessonsEnabled;
    private boolean firstStart = true;
    private LocalDateTime lastUpdate;

    @OnClick(R.id.empty_button_reload)
    void onEmptyButton() {
        if (swipeRefreshLayout.isRefreshing()) return;
        refreshEvents();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyCalendarTheme(this);
        setContentView(R.layout.activity_calendar);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        setTitle(getResources().getString(R.string.calendar));
        os = InfoManager.getOpenStud(getApplication());
        student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(CalendarActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        lessonOptionsEnabled = PreferenceManager.isLessonOptionEnabled(this);
        lessonsEnabled = PreferenceManager.isLessonEnabled(this);
        appBarLayout.addOnOffsetChangedListener(this);
        View headerLayout = navigationView.getHeaderView(0);
        TextView navTitle = headerLayout.findViewById(R.id.nav_title);
        navTitle.setText(getString(R.string.fullname, student.getFirstName(), student.getLastName()));
        TextView subTitle = headerLayout.findViewById(R.id.nav_subtitle);
        subTitle.setText(student.getStudentID());
        compactCalendarView.setLocale(TimeZone.getDefault(), Locale.getDefault());
        compactCalendarView.setShouldDrawDaysHeader(true);
        compactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                setSubtitle(dateFormat.format(dateClicked));
                synchronized (this) {
                    currentDate = dateClicked;
                    getEventsByDate(events, dateClicked);
                }
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                setSubtitle(dateFormat.format(firstDayOfNewMonth));
                synchronized (this) {
                    currentDate = firstDayOfNewMonth;
                    getEventsByDate(events, firstDayOfNewMonth);
                }
            }
        });
        setupRecyclerLayouts();
        List<lithium.openstud.driver.core.Event> events_cached = InfoManager.getEventsCached(this, os);

        if (events_cached != null && !events_cached.isEmpty()) events.addAll(events_cached);
        // Set current date to today
        if (savedInstanceState == null) currentDate = ClientHelper.getDateWithoutTime();
        else currentDate = (Date) savedInstanceState.getSerializable("currentDate");
        setCurrentDate(currentDate);
        updateCalendar(events);
        RelativeLayout datePickerButton = findViewById(R.id.date_picker_button);
        datePickerButton.setOnClickListener(v -> animateExpansion());
        setupDrawerListener();
        emptyText.setText(getResources().getString(R.string.no_events));
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        if (savedInstanceState == null) refreshEvents();
        swipeRefreshLayout.setOnRefreshListener(this::refreshEvents);

    }

    private void setupRecyclerLayouts() {

        lessons_rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        lessons_rv.setLayoutManager(llm);
        Activity activity = this;
        EventAdapter.EventAdapterListener eal = new EventAdapter.EventAdapterListener() {
            @Override
            public void addCalendarOnClick(lithium.openstud.driver.core.Event ev) {
                ClientHelper.addEventToCalendar(activity, ev);
            }

            @Override
            public void placeReservation(lithium.openstud.driver.core.Event ev, ExamReservation res) {
                confirmReservation(res);
            }

            @Override
            public void deleteReservation(lithium.openstud.driver.core.Event ev, ExamReservation res) {
                ClientHelper.createConfirmDeleteReservationDialog(activity, res, () -> CalendarActivity.this.deleteReservation(res));
            }
        };

        adapter_lessons = new EventAdapter(this, lessons, eal);
        lessons_rv.setAdapter(adapter_lessons);

        exams_rv.setHasFixedSize(true);
        LinearLayoutManager llm2 = new LinearLayoutManager(this);
        exams_rv.setLayoutManager(llm2);
        adapter_exams = new EventAdapter(this, exams, eal);
        exams_rv.setAdapter(adapter_exams);

        reservations_rv.setHasFixedSize(true);
        LinearLayoutManager llm3 = new LinearLayoutManager(this);
        reservations_rv.setLayoutManager(llm3);
        adapter_reservations = new EventAdapter(this, reservations, eal);
        reservations_rv.setAdapter(adapter_reservations);
    }

    private void refreshEvents() {

        runOnUiThread(() -> {
            swipeRefreshLayout.setRefreshing(true);
            emptyButton.setEnabled(false);
        });
        new Thread(() -> {
            try {
                List<lithium.openstud.driver.core.Event> newEvents = InfoManager.getEvents(this, os, student);
                updateEventList(newEvents);
                h.sendEmptyMessage(ClientHelper.Status.OK.getValue());
                showCalendarNotification();
                updateTimer();
            } catch (OpenstudConnectionException e) {
                h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidResponseException e) {
                if (e.isRateLimit()) h.sendEmptyMessage(ClientHelper.Status.RATE_LIMIT.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidCredentialsException e) {
                if (e.isPasswordExpired())
                    h.sendEmptyMessage(ClientHelper.Status.EXPIRED_CREDENTIALS.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
                e.printStackTrace();
            } finally {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    emptyButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void updateCalendar(List<lithium.openstud.driver.core.Event> events) {
        if (events == null) return;
        List<Event> calendarEvents = new LinkedList<>();
        List<Long> withLesson = new LinkedList<>();
        for (lithium.openstud.driver.core.Event event : events) {
            if (event.getEventType() == EventType.LESSON && InfoManager.filterContains(this, event.getDescription()))
                continue;
            ZoneId zoneId = ZoneId.systemDefault();
            Timestamp timestamp = new Timestamp(event.getStart().toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli());
            Event ev;
            if (event.getEventType() == EventType.DOABLE) {
                ev = new Event(Color.YELLOW, timestamp.getTime());
            } else if (event.getEventType() == EventType.LESSON) {
                if (withLesson.contains(timestamp.getTime())) continue;
                withLesson.add(timestamp.getTime());
                ev = new Event(Color.GREEN, timestamp.getTime());
            } else {
                ev = new Event(Color.RED, timestamp.getTime());
            }
            calendarEvents.add(ev);
        }
        runOnUiThread(() -> {
            compactCalendarView.removeAllEvents();
            compactCalendarView.addEvents(calendarEvents);
        });

        synchronized (this) {
            getEventsByDate(events, currentDate);
        }
    }

    private void getEventsByDate(List<lithium.openstud.driver.core.Event> events, Date date) {
        List<lithium.openstud.driver.core.Event> newLessons = new LinkedList<>();
        List<lithium.openstud.driver.core.Event> newReservations = new LinkedList<>();
        List<lithium.openstud.driver.core.Event> newDoable = new LinkedList<>();
        ZoneId zoneId = ZoneId.systemDefault();
        for (lithium.openstud.driver.core.Event event : events) {
            if (event.getEventType() == EventType.LESSON && InfoManager.filterContains(this, event.getDescription()))
                continue;
            Instant instant = Instant.ofEpochMilli(date.getTime());
            instant.atZone(zoneId);
            if (instant.toEpochMilli() != event.getStart().toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli())
                continue;
            if (event.getEventType() == EventType.DOABLE) {
                newDoable.add(event);
            } else if (event.getEventType() == EventType.LESSON) {
                newLessons.add(event);
            } else {
                newReservations.add(event);
            }
        }
        lessons.clear();
        reservations.clear();
        exams.clear();
        exams.addAll(newDoable);
        reservations.addAll(newReservations);
        lessons.addAll(newLessons);
        ClientHelper.orderByStartTime(lessons, true);
        runOnUiThread(() -> {
            adapter_reservations.notifyDataSetChanged();
            adapter_exams.notifyDataSetChanged();
            adapter_lessons.notifyDataSetChanged();
        });
        if (exams.isEmpty() && reservations.isEmpty() && lessons.isEmpty()) {
            invalidateOptionsMenu();
            runOnUiThread(() -> {
                emptyContainer.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.VISIBLE);
                findViewById(R.id.lessons_container).setVisibility(View.GONE);
                findViewById(R.id.exams_container).setVisibility(View.GONE);
                findViewById(R.id.reservations_container).setVisibility(View.GONE);
            });

        } else {
            invalidateOptionsMenu();
            runOnUiThread(() -> {
                emptyContainer.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                if (!lessons.isEmpty())
                    findViewById(R.id.lessons_container).setVisibility(View.VISIBLE);
                else findViewById(R.id.lessons_container).setVisibility(View.GONE);
                if (!exams.isEmpty())
                    findViewById(R.id.exams_container).setVisibility(View.VISIBLE);
                else findViewById(R.id.exams_container).setVisibility(View.GONE);
                if (!reservations.isEmpty())
                    findViewById(R.id.reservations_container).setVisibility(View.VISIBLE);
                else findViewById(R.id.reservations_container).setVisibility(View.GONE);
            });

        }

    }

    private void setCurrentDate(Date date) {
        setSubtitle(dateFormat.format(date));
        if (compactCalendarView != null) {
            runOnUiThread(() -> compactCalendarView.setCurrentDate(date));

        }
    }

    @Override
    public void setTitle(CharSequence title) {
        TextView tvTitle = findViewById(R.id.title);

        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    private void setSubtitle(String subtitle) {
        TextView datePickerTextView = findViewById(R.id.date_picker_text_view);

        if (datePickerTextView != null) {
            datePickerTextView.setText(subtitle);
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter:
                showFilterDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerListener() {
        ddl = new DelayedDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                int item = getItemPressedAndReset();
                if (item == -1) return;
                switch (item) {
                    case R.id.payments_menu: {
                        Intent intent = new Intent(CalendarActivity.this, PaymentsActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.exams_menu: {
                        Intent intent = new Intent(CalendarActivity.this, ExamsActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.profile_menu: {
                        Intent intent = new Intent(CalendarActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.exit_menu: {
                        InfoManager.clearSharedPreferences(getApplication());
                        Intent i = new Intent(CalendarActivity.this, LauncherActivity.class);
                        startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        break;
                    }

                    case R.id.classrooms_menu: {
                        Intent intent = new Intent(CalendarActivity.this, SearchClassroomActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.about_menu: {
                        Intent intent = new Intent(CalendarActivity.this, AboutActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.settings_menu: {
                        Intent intent = new Intent(CalendarActivity.this, SettingsPrefActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.stats_menu: {
                        Intent intent = new Intent(CalendarActivity.this, StatsActivity.class);
                        startActivity(intent);
                        break;
                    }
                }
            }

        };
        mDrawerLayout.addDrawerListener(ddl);
        navigationView.setNavigationItemSelectedListener(
                item -> {
                    mDrawerLayout.closeDrawers();
                    ddl.setItemPressed(item.getItemId());
                    return true;
                });
    }

    void animateExpansion() {
        float rotation = isExpanded ? 0 : 180;
        ViewCompat.animate(arrow).rotation(rotation).start();

        isExpanded = !isExpanded;
        appBarLayout.setExpanded(isExpanded, true);
    }

    public void onSaveInstanceState(Bundle savedInstance) {
        savedInstance.putBoolean("loadOnStart", false);
        savedInstance.putSerializable("currentDate", currentDate);
        super.onSaveInstanceState(savedInstance);
    }

    private boolean confirmReservation(ExamReservation res) {
        try {
            Pair<Integer, String> pair = os.insertReservation(res);
            InfoManager.setReservationUpdateFlag(this, true);
            if (pair == null) {
                h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                return false;
            } else if (pair.getRight() == null && pair.getLeft() == -1) {
                h.sendEmptyMessage(ClientHelper.Status.ALREADY_PLACED.getValue());
                return true;
            }
            if (pair.getRight() != null) ClientHelper.createCustomTab(this, pair.getRight());
            else {
                refreshEvents();
                h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_OK.getValue());
                return true;
            }
        } catch (OpenstudInvalidResponseException e) {
            e.printStackTrace();
            h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_INVALID_RESPONSE.getValue());
        } catch (OpenstudConnectionException e) {
            e.printStackTrace();
            h.sendEmptyMessage(ClientHelper.Status.PLACE_RESERVATION_CONNECTION.getValue());
        } catch (OpenstudInvalidCredentialsException e) {
            e.printStackTrace();
            h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
        }
        return false;
    }

    private void deleteReservation(ExamReservation res) {
        try {
            int ret = os.deleteReservation(res);
            if (ret != -1) {
                synchronized (this) {
                    refreshEvents();
                }
                h.sendEmptyMessage(ClientHelper.Status.OK_DELETE.getValue());
            } else h.sendEmptyMessage(ClientHelper.Status.FAILED_DELETE.getValue());
        } catch (OpenstudInvalidResponseException | OpenstudConnectionException e) {
            h.sendEmptyMessage(ClientHelper.Status.FAILED_DELETE.getValue());
        } catch (OpenstudInvalidCredentialsException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        if (Math.abs(offset) < appBarLayout.getTotalScrollRange() / 2) {
            if (!isExpanded) ViewCompat.animate(arrow).rotation(180).start();
            isExpanded = true;
        } else {
            if (isExpanded) ViewCompat.animate(arrow).rotation(0).start();
            isExpanded = false;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calendar_bar, menu);
        Drawable drawable = menu.findItem(R.id.filter).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, android.R.color.white));
        menu.findItem(R.id.filter).setIcon(drawable);
        return true;
    }

    private synchronized List<String> generateListEventsNames() {
        List<String> names = new LinkedList<>();
        for (lithium.openstud.driver.core.Event event : events) {
            if (!names.contains(event.getDescription()) && event.getEventType() == EventType.LESSON)
                names.add(event.getDescription());
        }
        return names;
    }

    private void showFilterDialog() {
        BottomSheetFilterEventFragment filterFrag = BottomSheetFilterEventFragment.newInstance(generateListEventsNames());
        filterFrag.show(getSupportFragmentManager(), filterFrag.getTag());
    }

    private synchronized void updateEventList(List<lithium.openstud.driver.core.Event> newEvents) {
        if (newEvents != null && events != null && !events.equals(newEvents)) {
            events.clear();
            events.addAll(newEvents);
            updateCalendar(events);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        synchronized (this) {
            if (refreshAfterDismiss) updateCalendar(events);
        }
    }

    public void onResume() {
        super.onResume();
        LocalDateTime time = getTimer();
        if (firstStart) firstStart = false;
        else if (lessonsEnabled != PreferenceManager.isLessonEnabled(this)) {
            refreshEvents();
            lessonsEnabled = !lessonsEnabled;
        } else if (lessonOptionsEnabled != PreferenceManager.isLessonOptionEnabled(this)) {
            if (adapter_lessons != null) adapter_lessons.notifyDataSetChanged();
            lessonOptionsEnabled = !lessonOptionsEnabled;
        } else if (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 240)
            refreshEvents();
    }

    private synchronized void updateTimer() {
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer() {
        return lastUpdate;
    }

    private void showCalendarNotification() {
        if (com.lithium.leona.openstud.data.PreferenceManager.getCalendarNotificationEnabled(this)) {
            LayoutHelper.createActionSnackBar(mDrawerLayout, R.string.lesson_notification, R.string.edit, 4000, v -> {
                InfoManager.clearSharedPreferences(getApplication());
                Intent i = new Intent(CalendarActivity.this, SettingsPrefActivity.class);
                startActivity(i);
            });
            com.lithium.leona.openstud.data.PreferenceManager.setCalendarNotificationEnabled(this, false);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (events == null || events.isEmpty()) {
            MenuItem item = menu.findItem(R.id.filter);
            item.setVisible(false);
        } else {
            boolean enable = false;
            for (lithium.openstud.driver.core.Event event : events) {
                if (event.getEventType() == EventType.LESSON) {
                    enable = true;
                    break;
                }
            }
            MenuItem item = menu.findItem(R.id.filter);
            item.setVisible(enable);
        }
        return true;
    }

    private static class CalendarEventHandler extends Handler {
        private final WeakReference<CalendarActivity> mActivity;

        CalendarEventHandler(CalendarActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CalendarActivity activity = mActivity.get();
            if (activity != null) {
                View.OnClickListener listener = v -> activity.refreshEvents();
                if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.invalid_response_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.RATE_LIMIT.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.rate_limit, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.INVALID_CREDENTIALS.getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    InfoManager.clearSharedPreferences(activity.getApplication());
                    Intent i = new Intent(activity, LauncherActivity.class);
                    i.putExtra("error", msg.what);
                    activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    activity.finish();
                } else if (msg.what == ClientHelper.Status.PLACE_RESERVATION_OK.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.reservation_ok, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.PLACE_RESERVATION_INVALID_RESPONSE.getValue() || msg.what == ClientHelper.Status.PLACE_RESERVATION_CONNECTION.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.reservation_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.ALREADY_PLACED.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.already_placed_reservation, Snackbar.LENGTH_LONG);
                } else if (msg.what == (ClientHelper.Status.FAILED_DELETE).getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.failed_delete, Snackbar.LENGTH_LONG);
                } else if (msg.what == (ClientHelper.Status.OK_DELETE).getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.ok_delete, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.FAILED_GET.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.failed_get_network, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.CLOSED_RESERVATION.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.closed_reservation, Snackbar.LENGTH_LONG);
                }

            }
        }
    }

}