package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.ref.WeakReference;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Isee;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class ProfileActivity extends AppCompatActivity {

    private static class ProfileEventHandler extends Handler {
        private final WeakReference<ProfileActivity> mActivity;

        ProfileEventHandler(ProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ProfileActivity activity = mActivity.get();
            if (activity != null) {
                View.OnClickListener listener = v -> new Thread(() -> activity.refresh(activity.os));
                if (msg.what == ClientHelper.Status.OK.getValue()) {
                    activity.applyInfos(activity.student, activity.isee);
                } else if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                    LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.invalid_response_error, R.string.retry, Snackbar.LENGTH_LONG, listener);
                } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                    LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
                } else if (msg.what == ClientHelper.Status.INVALID_CREDENTIALS.getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                    InfoManager.clearSharedPreferences(activity.getApplication());
                    Intent i = new Intent(activity, LauncherActivity.class);
                    activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    activity.finish();
                }
            }
        }
    }


    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.collapsingToolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.studentId)
    TextView studentId;
    @BindView(R.id.birthDate)
    TextView birthDate;
    @BindView(R.id.birthPlace)
    TextView birthPlace;
    @BindView(R.id.isee)
    TextView isee_field;
    @BindView(R.id.departmentDescription)
    TextView departmentDescription;
    @BindView(R.id.courseDescription)
    TextView courseDescription;
    @BindView(R.id.courseYear)
    TextView courseYear;
    @BindView(R.id.studentStatus)
    TextView studentStatus;
    @BindView(R.id.cfu)
    TextView cfu;
    private DelayedDrawerListener ddl;
    private View headerLayout;
    private NavigationView nv;
    private Student student;
    private Isee isee;
    private Openstud os;
    private ProfileEventHandler h = new ProfileEventHandler(this);
    private LocalDateTime lastUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyProfileTheme(this);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        nv = LayoutHelper.setupNavigationDrawer(this, mDrawerLayout);
        setupDrawerListener();
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        headerLayout = nv.getHeaderView(0);
        os = InfoManager.getOpenStud(getApplication());
        student = InfoManager.getInfoStudentCached(getApplication(), os);
        isee = InfoManager.getIseeCached(getApplication(), os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(ProfileActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        applyInfos(student, isee);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Thread t1 = new Thread(() -> refresh(os));
            t1.start();
        });
        new Thread(() -> refresh(os)).start();
    }

    protected void onRestart() {
        super.onRestart();
        LocalDateTime time = getTimer();
        if (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 60) {
            new Thread(() -> refresh(os)).start();
        }
    }


    private void refresh(Openstud os) {
        setRefreshing(true);
        try {
            student = InfoManager.getInfoStudent(getApplication(), os);
            isee = InfoManager.getIsee(getApplication(), os);
        } catch (OpenstudConnectionException e) {
            h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidResponseException e) {
            h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
            e.printStackTrace();
        } catch (OpenstudInvalidCredentialsException e) {
            if (e.isPasswordExpired())
                h.sendEmptyMessage(ClientHelper.Status.EXPIRED_CREDENTIALS.getValue());
            else h.sendEmptyMessage(ClientHelper.Status.INVALID_CREDENTIALS.getValue());
            e.printStackTrace();
        }
        updateTimer();
        setRefreshing(false);
    }

    private void applyInfos(Student st, Isee isee) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        TextView navTitle = headerLayout.findViewById(R.id.nav_title);
        navTitle.setText(getString(R.string.fullname, student.getFirstName(), student.getLastName()));
        collapsingToolbarLayout.setTitle(st.getFirstName() + " " + st.getLastName());
        TextView navSubtitle = headerLayout.findViewById(R.id.nav_subtitle);
        navSubtitle.setText(String.valueOf(st.getStudentID()));
        studentId.setText(String.valueOf(st.getStudentID()));
        LocalDate date = st.getBirthDate();
        birthDate.setText((st.getBirthDate().format(formatter)));
        birthPlace.setText(st.getBirthPlace());
        if (isee == null) isee_field.setText(getResources().getString(R.string.isee_not_avaiable));
        else isee_field.setText(String.valueOf(isee.getValue()));
        if (st.getCourseName() != null && !st.getCourseName().equals("")) {
            departmentDescription.setText(st.getDepartmentName());
            courseDescription.setText(st.getCourseName());
            if (Locale.getDefault().getLanguage().equals("it"))
                courseYear.setText(getResources().getString(R.string.year_corse_profile, st.getCourseYear() + "°"));
            else {
                String year = st.getCourseYear();
                if (StringUtils.isNumeric(st.getCourseYear()) && Integer.parseInt(year) <= 3)
                    year = year + "rd";
                else year = year + "th";
                courseYear.setText(getResources().getString(R.string.year_corse_profile, year));
            }
            studentStatus.setText(st.getStudentStatus());
            cfu.setText(String.valueOf(st.getCfu()));
        } else {
            courseDescription.setText(getResources().getString(R.string.not_enrolled));
            LinearLayout linearLayout = findViewById(R.id.course_extra_info);
            linearLayout.setVisibility(View.GONE);
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

    private void setupDrawerListener() {
        ddl = new DelayedDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                int item = getItemPressedAndReset();
                if (item == -1) return;
                switch (item) {
                    case R.id.payments_menu: {
                        Intent intent = new Intent(ProfileActivity.this, PaymentsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.exit_menu: {
                        InfoManager.clearSharedPreferences(getApplication());
                        Intent i = new Intent(ProfileActivity.this, LauncherActivity.class);
                        startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        break;
                    }
                    case R.id.exams_menu: {
                        Intent intent = new Intent(ProfileActivity.this, ExamsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.about_menu: {
                        Intent intent = new Intent(ProfileActivity.this, AboutActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.settings_menu: {
                        Intent intent = new Intent(ProfileActivity.this, SettingsPrefActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.stats_menu: {
                        Intent intent = new Intent(ProfileActivity.this, StatsActivity.class);
                        startActivity(intent);
                        break;
                    }
                }
            }

        };
        mDrawerLayout.addDrawerListener(ddl);
        nv.setNavigationItemSelectedListener(
                item -> {
                    mDrawerLayout.closeDrawers();
                    ddl.setItemPressed(item.getItemId());
                    return true;
                });
    }

    private synchronized void updateTimer() {
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer() {
        return lastUpdate;
    }

    private void setRefreshing(boolean enabled) {
        runOnUiThread(() -> swipeRefreshLayout.setEnabled(enabled));
    }
}
