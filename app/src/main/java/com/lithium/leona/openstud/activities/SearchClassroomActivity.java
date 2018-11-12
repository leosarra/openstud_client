package com.lithium.leona.openstud.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.ClassroomAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.lithium.leona.openstud.listeners.ClickListener;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lithium.openstud.driver.core.Classroom;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class SearchClassroomActivity extends AppCompatActivity implements MaterialSearchBar.OnSearchActionListener, MaterialSearchBar.OnClickListener {


    @BindView(R.id.searchBar)
    MaterialSearchBar searchBar;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.nav_view)
    NavigationView nv;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.recyclerView)
    RecyclerView rv;
    @BindView(R.id.empty_button_reload)
    Button emptyButton;
    @BindView(R.id.empty_text)
    TextView emptyText;
    @BindView(R.id.empty_layout)
    LinearLayout emptyLayout;
    @BindView(R.id.frame)
    FrameLayout contentFrame;
    private DelayedDrawerListener ddl;
    private Openstud os;
    private Student student;
    private List<Classroom> classes = new LinkedList<>();
    private ClassroomAdapter adapter;
    private SearchClassroomHandler h = new SearchClassroomHandler(this);

    @OnClick(R.id.empty_button_reload)
    void onClickReloadButton() {
        searchClassrooms(searchBar.getText());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applySearchClassroomTheme(this);
        setContentView(R.layout.activity_search_classroom);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        searchBar.setPlaceHolder(getResources().getString(R.string.search_classroom));
        //restore last queries from disk
        List oldSuggestions = PreferenceManager.getSuggestions(this);
        if (oldSuggestions != null) searchBar.setLastSuggestions(oldSuggestions);
        os = InfoManager.getOpenStud(getApplication());
        emptyText.setText(getResources().getString(R.string.no_classrooms_found));
        student = InfoManager.getInfoStudentCached(getApplication(), os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(SearchClassroomActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        View headerLayout = nv.getHeaderView(0);
        TextView navTitle = headerLayout.findViewById(R.id.nav_title);
        navTitle.setText(getString(R.string.fullname, student.getFirstName(), student.getLastName()));
        TextView subTitle = headerLayout.findViewById(R.id.nav_subtitle);
        subTitle.setText(student.getStudentID());
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        adapter = new ClassroomAdapter(mDrawerLayout, this, classes);
        rv.setAdapter(adapter);
        setLoadingEnabled(false, false);
        adapter.notifyDataSetChanged();
        setupDrawerListener();
        setupContentListeners();
    }

    public synchronized void searchClassrooms(String query) {
        new Thread(() -> {
            setLoadingEnabled(true, false);
            List<Classroom> update = null;
            try {
                update = os.getClassRoom(query);
            } catch (OpenstudInvalidResponseException e) {
                e.printStackTrace();
                h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
            } catch (OpenstudConnectionException e) {
                e.printStackTrace();
                h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
            }
            h.sendEmptyMessage(ClientHelper.Status.OK.getValue());
            updateView(update);
        }).start();
    }

    private void updateView(List<Classroom> update) {
        if (update == null || update.isEmpty()) {
            setLoadingEnabled(false, true);
        } else if (update.equals(classes)) {
            setLoadingEnabled(false, false);
        } else {
            classes.clear();
            classes.addAll(update);
            runOnUiThread(() -> adapter.notifyDataSetChanged());
            setLoadingEnabled(false, false);
        }
    }

    private void setLoadingEnabled(boolean loading, boolean isEmpty) {
        if (loading) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.GONE);
            });
        } else {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (!isEmpty) {
                    rv.setVisibility(View.VISIBLE);
                    emptyLayout.setVisibility(View.GONE);
                } else {
                    rv.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupContentListeners() {
        searchBar.setOnSearchActionListener(this);
        searchBar.setOnClickListener(this);
        View.OnTouchListener otl = this::handleTouchEvent;
        emptyLayout.setOnTouchListener(otl);
        progressBar.setOnTouchListener(otl);
        contentFrame.setOnTouchListener(otl);
        rv.setOnTouchListener(otl);
        findViewById(R.id.mt_editText).setOnClickListener(this);
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
    }

    @Override
    public void onSearchConfirmed(CharSequence text) {
        ClientHelper.hideKeyboard(mDrawerLayout, this);
        if (!text.toString().trim().isEmpty()) searchClassrooms(text.toString());
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
    public void onClick(View v) {
        System.out.println(searchBar.isSearchEnabled());
        if (searchBar.isSearchEnabled() && !searchBar.isSuggestionsVisible()) {
            searchBar.showSuggestionsList();
        } else searchBar.enableSearch();
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        System.out.println(buttonCode);
        switch (buttonCode) {
            case MaterialSearchBar.BUTTON_NAVIGATION:
                mDrawerLayout.openDrawer(Gravity.START);
                break;
            case MaterialSearchBar.BUTTON_BACK:
                searchBar.disableSearch();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //save last queries to disk
        PreferenceManager.saveSuggestions(this, searchBar.getLastSuggestions());
    }

    private void setupDrawerListener() {
        ddl = new DelayedDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                int item = getItemPressedAndReset();
                if (item == -1) return;
                switch (item) {
                    case R.id.payments_menu: {
                        Intent intent = new Intent(SearchClassroomActivity.this, PaymentsActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.calendar_menu: {
                        Intent intent = new Intent(SearchClassroomActivity.this, CalendarActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.exit_menu: {
                        InfoManager.clearSharedPreferences(getApplication());
                        Intent i = new Intent(SearchClassroomActivity.this, LauncherActivity.class);
                        startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        break;
                    }
                    case R.id.exams_menu: {
                        Intent intent = new Intent(SearchClassroomActivity.this, ExamsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.about_menu: {
                        Intent intent = new Intent(SearchClassroomActivity.this, AboutActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.settings_menu: {
                        Intent intent = new Intent(SearchClassroomActivity.this, SettingsPrefActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.stats_menu: {
                        Intent intent = new Intent(SearchClassroomActivity.this, StatsActivity.class);
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

    private boolean handleTouchEvent(View view, MotionEvent event) {
        GestureDetector gd = new GestureDetector(SearchClassroomActivity.this, new ClickListener());
        if (searchBar.isSearchEnabled() && searchBar.getText().trim().isEmpty())
            searchBar.disableSearch();
        else if (searchBar.isSearchEnabled()) searchBar.hideSuggestionsList();
        return gd.onTouchEvent(event);
    }

    private static class SearchClassroomHandler extends Handler {
        private final WeakReference<SearchClassroomActivity> activity;

        private SearchClassroomHandler(SearchClassroomActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final SearchClassroomActivity activity = this.activity.get();
            if (activity == null) return;
            View.OnClickListener ocl = v -> activity.searchClassrooms(activity.searchBar.getText());
            if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            } else if (msg.what == ClientHelper.Status.USER_NOT_ENABLED.getValue()) {
                LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.user_not_enabled_error, Snackbar.LENGTH_LONG);
            } else if (msg.what == (ClientHelper.Status.INVALID_CREDENTIALS).getValue() || msg.what == ClientHelper.Status.EXPIRED_CREDENTIALS.getValue()) {
                InfoManager.clearSharedPreferences(activity.getApplication());
                Intent i = new Intent(activity, LauncherActivity.class);
                i.putExtra("error", msg.what);
                activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                activity.finish();
            } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                LayoutHelper.createTextSnackBar(activity.mDrawerLayout, R.string.invalid_response_error, Snackbar.LENGTH_LONG);
            }
        }
    }

}
