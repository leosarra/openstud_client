package com.lithium.leona.openstud.activities;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import org.threeten.bp.ZoneId;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.EventType;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class CalendarActivity extends AppCompatActivity {
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.app_bar_layout) AppBarLayout appBarLayout;
    @BindView(R.id.compactcalendar_view) CompactCalendarView compactCalendarView;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.date_picker_arrow) ImageView arrow;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @OnClick(R.id.content) void onClick1(){
        System.out.println("clicked");
        if (isExpanded) {
            animateExpansion();
        }
    }
    private DelayedDrawerListener ddl;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN);
    private boolean isExpanded = false;
    private Openstud os;
    private Student student;
    private List<lithium.openstud.driver.core.Event> events = new LinkedList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_menu);
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
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                setSubtitle(dateFormat.format(firstDayOfNewMonth));
            }
        });
        // Set current date to today
        setCurrentDate(new Date());
        RelativeLayout datePickerButton = findViewById(R.id.date_picker_button);
        datePickerButton.setOnClickListener(v -> animateExpansion());
        setupDrawerListener();
        swipeRefreshLayout.setNestedScrollingEnabled(true);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        if (savedInstanceState == null) new Thread(() -> getEvents()).start();
        else if (savedInstanceState.getBoolean("loadOnStart", true)) new Thread(() -> updateCalendar(InfoManager.getEventsCached(this,os))).start();
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(this::getEvents).start());

    }


    private void getEvents(){
        runOnUiThread(() -> swipeRefreshLayout.setRefreshing(true));
        try {
            List<lithium.openstud.driver.core.Event> newEvents = InfoManager.getEvents(this,os, student);
            if (newEvents != null && !events.equals(newEvents)) {
                events.clear();
                events.addAll(newEvents);
                updateCalendar(events);
            }
        } catch (OpenstudConnectionException e) {
            e.printStackTrace();
        } catch (OpenstudInvalidResponseException e) {
            e.printStackTrace();
        } catch (OpenstudInvalidCredentialsException e) {
            e.printStackTrace();
        }
        runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
    }


    private synchronized void updateCalendar(List<lithium.openstud.driver.core.Event> newEvents){
        if (newEvents == null) return;
        compactCalendarView.removeAllEvents();
        List<Event> calendarEvents = new LinkedList<>();
        List<Long> withLesson = new LinkedList<>();
        for (lithium.openstud.driver.core.Event event: newEvents){
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
        compactCalendarView.addEvents(calendarEvents);
        System.out.println(calendarEvents);
    }

    private void setCurrentDate(Date date) {
        setSubtitle(dateFormat.format(date));
        if (compactCalendarView != null) {
            compactCalendarView.setCurrentDate(date);
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
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
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

    void animateExpansion(){
        float rotation = isExpanded ? 0 : 180;
        ViewCompat.animate(arrow).rotation(rotation).start();

        isExpanded = !isExpanded;
        appBarLayout.setExpanded(isExpanded, true);
    }

    public void onSaveInstanceState(Bundle savedInstance) {
        savedInstance.putBoolean("loadOnStart",false);
        super.onSaveInstanceState(savedInstance);
    }
}