package com.lithium.leona.openstud;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.fragments.ExamsDoneFragment;
import com.lithium.leona.openstud.fragments.ReservationsFragment;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExamsActivity extends AppCompatActivity {
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    private TextView mTextMessage;
    private NavigationView nv;
    private DelayedDrawerListener ddl;
    private View headerLayout;
    private ExamsDoneFragment examsCompleted;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_completed:
                    switchToExamsCompletedFragment();
                    return true;
                case R.id.navigation_reservations:
                    switchToExamsReservationsFragment();
                    return true;
                case R.id.navigation_search:
                    switchToExamsSearchFragment();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exams);
        ButterKnife.bind(this);
        nv = LayoutHelper.setupNavigationDrawer(this, mDrawerLayout);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_menu);
        getSupportActionBar().setTitle(R.string.exams);
        headerLayout = nv.getHeaderView(0);
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        setupListeners();
        switchToExamsCompletedFragment();
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

    @Override
    public void onBackPressed() {
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupListeners(){
        ddl = new DelayedDrawerListener(){
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                int item = getItemPressedAndReset();
                if (item == -1) return;
                switch (item) {
                    case R.id.payments_menu: {
                        Intent intent = new Intent(ExamsActivity.this, PaymentsActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.profile_menu: {
                        Intent intent = new Intent(ExamsActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case R.id.exit_menu: {
                        InfoManager.clearSharedPreferences(getApplication());
                        Intent i = new Intent(ExamsActivity.this, LauncherActivity.class);
                        startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        break;
                    }
                }
            }

        };
        mDrawerLayout.addDrawerListener(ddl);
        nv.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        mDrawerLayout.closeDrawers();
                        ddl.setItemPressed(item.getItemId());
                        return true;
                    }
                });
    }

    private void switchToExamsCompletedFragment(){
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag("completed") != null) {
            manager.beginTransaction().show(manager.findFragmentByTag("completed")).commit();
        }
        else manager.beginTransaction().replace(R.id.content_frame, new ExamsDoneFragment(),"completed").commit();
        if (manager.findFragmentByTag("reservations") !=null ){
            manager.beginTransaction().hide(manager.findFragmentByTag("reservations"));
        }
        if (manager.findFragmentByTag("search") !=null ){
            manager.beginTransaction().hide(manager.findFragmentByTag("search"));
        }
    }

    private void switchToExamsReservationsFragment(){
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag("reservations") != null) {
            manager.beginTransaction().show(manager.findFragmentByTag("reservations")).commit();
        }
        else manager.beginTransaction().replace(R.id.content_frame, new ReservationsFragment(),"reservations").commit();
        if (manager.findFragmentByTag("completed") !=null ){
            manager.beginTransaction().hide(manager.findFragmentByTag("completed"));
        }
        if (manager.findFragmentByTag("search") !=null ){
            manager.beginTransaction().hide(manager.findFragmentByTag("search"));
        }
    }

    private void switchToExamsSearchFragment(){
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag("search") != null) {
            manager.beginTransaction().show(manager.findFragmentByTag("search")).commit();
        }
        else manager.beginTransaction().replace(R.id.content_frame, new ExamsDoneFragment(),"search").commit();
        if (manager.findFragmentByTag("completed") !=null ){
            manager.beginTransaction().hide(manager.findFragmentByTag("completed"));
        }
        if (manager.findFragmentByTag("reservations") !=null ){
            manager.beginTransaction().hide(manager.findFragmentByTag("reservations"));
        }
    }


    public synchronized void createTextSnackBar(int string_id, int length) {
        Snackbar snackbar = ClientHelper.createTextSnackBar(mDrawerLayout,string_id,length);
        snackbar.show();
    }

    public synchronized  void createRetrySnackBar(final int string_id, int length, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar
                .make(mDrawerLayout, getResources().getString(string_id), length).setAction(R.string.retry, listener);
        snackbar.show();
    }
}
