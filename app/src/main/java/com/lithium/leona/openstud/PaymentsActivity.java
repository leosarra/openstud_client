package com.lithium.leona.openstud;

import android.content.Intent;
import android.icu.text.IDNA;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import com.lithium.leona.openstud.fragments.TabFragment;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;


public class PaymentsActivity extends AppCompatActivity {
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    private DelayedDrawerListener ddl;
    private NavigationView nv;
    private Openstud os;
    private Student student;
    private SparseArray<Snackbar> snackBarMap = new SparseArray<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);
        ButterKnife.bind(this);
        os = InfoManager.getOpenStud(this);
        student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(PaymentsActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }
        nv = LayoutHelper.setupNavigationDrawer(this, mDrawerLayout);
        LayoutHelper.setupToolbar(this,toolbar, R.drawable.ic_baseline_arrow_back);
        getSupportActionBar().setTitle(R.string.payments);
        setupListeners();
        View headerLayout = nv.getHeaderView(0);
        TextView navTitle = headerLayout.findViewById(R.id.nav_title);
        TextView navSubtitle = headerLayout.findViewById(R.id.nav_subtitle);
        navTitle.setText(getString(R.string.fullname, student.getFirstName(), student.getLastName()));
        navSubtitle.setText(String.valueOf(student.getStudentID()));

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame,new TabFragment()).commit();
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
                    case R.id.profile_menu: {
                        Intent intent = new Intent(PaymentsActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.exit_menu: {
                        InfoManager.clearSharedPreferences(getApplication());
                        Intent i = new Intent(PaymentsActivity.this, LauncherActivity.class);
                        startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        break;
                    }
                    case R.id.exams_menu: {
                        Intent intent = new Intent(PaymentsActivity.this, ExamsActivity.class);
                        startActivity(intent);
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
        View headerLayout = nv.getHeaderView(0);

        TextView studentId = headerLayout.findViewById(R.id.nav_subtitle);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public synchronized void createTextSnackBar(int string_id, int length) {
        if (snackBarMap.get(string_id,null)!=null) return;
        Snackbar snackbar = ClientHelper.createTextSnackBar(mDrawerLayout,string_id,length);
        snackBarMap.put(string_id,snackbar);
    }

    public synchronized  void createRetrySnackBar(final int string_id, int length, View.OnClickListener listener) {
        if (snackBarMap.get(string_id,null)!=null) return;
        Snackbar snackbar = Snackbar
                .make(mDrawerLayout, getResources().getString(string_id), length).setAction(R.string.retry, listener);
        snackBarMap.put(string_id,snackbar);
        snackbar.addCallback(new Snackbar.Callback(){
            public void onDismissed(Snackbar snackbar, int event) {
                removeKeyFromMap(string_id);
            }
        });
        snackbar.show();
    }

    private synchronized void removeKeyFromMap(int id){
        snackBarMap.remove(id);
    }

}
