package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.ClassroomAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;
import com.mancj.materialsearchbar.MaterialSearchBar;

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

public class SearchClassroomActivity extends AppCompatActivity implements MaterialSearchBar.OnSearchActionListener {
    @BindView(R.id.searchBar) MaterialSearchBar searchBar;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.nav_view)
    NavigationView nv;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.recyclerView) RecyclerView rv;
    @BindView(R.id.empty_button_reload)
    Button emptyButton;
    @BindView(R.id.empty_text) TextView emptyText;
    @BindView(R.id.empty_layout)
    LinearLayout emptyLayout;
    @OnClick(R.id.empty_button_reload) void onClick(){
        searchClasses(searchBar.getText());
    }

    private DelayedDrawerListener ddl;
    private Openstud os;
    private Student student;
    private List<Classroom> classes = new LinkedList<>();
    private ClassroomAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_classroom);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        searchBar.setPlaceHolder("Ricerca aula");
        //enable searchbar callbacks
        searchBar.setOnSearchActionListener(this);
        //restore last queries from disk
        //searchBar.setLastSuggestions(list);
        //Inflate menu and setup OnMenuItemClickListener
        //searchBar.inflateMenu(R.menu.drawer_view);
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
        setupDrawerListener();
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        adapter = new ClassroomAdapter(mDrawerLayout,this,classes);
        rv.setAdapter(adapter);
        setLoadingEnabled(false, false);
        adapter.notifyDataSetChanged();
    }


    public synchronized void searchClasses(String query){
        new Thread(() -> {
            setLoadingEnabled(true, false);
            List<Classroom> update = null;
            try {
                update = os.getClassRoom(query);
            } catch (OpenstudInvalidResponseException e) {
                e.printStackTrace();
            } catch (OpenstudConnectionException e) {
                e.printStackTrace();
            }
            updateView(update);
        }).start();
    }

    private void updateView(List<Classroom> update) {
        if(update == null || update.isEmpty()) {
            setLoadingEnabled(false,true);
        }
        else if (update.equals(classes)){
            setLoadingEnabled(false, false);
        }
        else {
            classes.clear();
            classes.addAll(update);
            runOnUiThread(() -> adapter.notifyDataSetChanged());
            setLoadingEnabled(false, false);
        }
    }

    private void setLoadingEnabled(boolean loading, boolean isEmpty){
        if (loading) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.GONE);
            });
        }
        else {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (!isEmpty) {
                    rv.setVisibility(View.VISIBLE);
                    emptyLayout.setVisibility(View.GONE);
                }
                else {
                    rv.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
    }

    @Override
    public void onSearchConfirmed(CharSequence text) {
        ClientHelper.hideKeyboard(mDrawerLayout,this);
        if (!text.toString().trim().isEmpty()) searchClasses(text.toString());
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
    public void onButtonClicked(int buttonCode) {
        System.out.println(buttonCode);
        switch (buttonCode){
            case MaterialSearchBar.BUTTON_NAVIGATION:
                mDrawerLayout.openDrawer(Gravity.START);
                break;
            case MaterialSearchBar.BUTTON_BACK:
                searchBar.disableSearch();
                break;
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

}
