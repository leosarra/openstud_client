package com.lithium.leona.openstud.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.fragments.ExamDoableFragment;
import com.lithium.leona.openstud.fragments.ExamsDoneFragment;
import com.lithium.leona.openstud.fragments.ReservationsFragment;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.mikepenz.materialdrawer.Drawer;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Student;

public class ExamsActivity extends AppCompatActivity {
    @BindView(R.id.container)
    ConstraintLayout mainLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    private Fragment active;
    private ExamsDoneFragment fragDone;
    private ExamDoableFragment fragDoable;
    private ReservationsFragment fragRes;
    private Drawer drawer;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentManager fm = getSupportFragmentManager();
            switch (item.getItemId()) {
                case R.id.navigation_completed:
                    switchToExamsCompletedFragment();
                    active = fm.findFragmentByTag("completed");
                    return true;
                case R.id.navigation_reservations:
                    switchToExamsReservationsFragment();
                    active = fm.findFragmentByTag("reservations");
                    return true;
                case R.id.navigation_search:
                    switchToExamsSearchFragment();
                    active = fm.findFragmentByTag("doable");
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyExamTheme(this);
        setContentView(R.layout.activity_exams);
        ButterKnife.bind(this);
        Openstud os = InfoManager.getOpenStud(getApplication());
        Student student = InfoManager.getInfoStudentCached(getApplication(), os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(ExamsActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_menu);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.exams);
        drawer = LayoutHelper.applyDrawer(this, toolbar, student);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            fragDone = (ExamsDoneFragment) fm.getFragment(savedInstanceState, "completed");
            fragRes = (ReservationsFragment) fm.getFragment(savedInstanceState, "reservations");
            fragDoable = (ExamDoableFragment) fm.getFragment(savedInstanceState, "doable");
            active = fm.getFragment(savedInstanceState, "active");
            if (active != null) fm.beginTransaction().show(active).commit();
            else {
                fm.beginTransaction().show(fragDone).commit();
                active = fragDone;
            }
        } else {
            fragRes = new ReservationsFragment();
            fragDone = new ExamsDoneFragment();
            fragDoable = new ExamDoableFragment();
            fm.beginTransaction().add(R.id.content_frame, fragRes, "reservations").hide(fragRes).commit();
            fm.beginTransaction().add(R.id.content_frame, fragDoable, "doable").hide(fragDoable).commit();
            fm.beginTransaction().add(R.id.content_frame, fragDone, "completed").commit();
            active = fragDone;
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        Drawable drawable = menu.findItem(R.id.sort).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, android.R.color.white));
        menu.findItem(R.id.sort).setIcon(drawable);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (active == null) return true;
        if (active != fragDone) {
            MenuItem item = menu.findItem(R.id.sort);
            item.setVisible(false);
        } else {
            MenuItem item = menu.findItem(R.id.sort);
            item.setVisible(true);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                showSortDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) drawer.closeDrawer();
        else super.onBackPressed();

    }

    private void showSortDialog() {
        Context context = this;
        int themeId = ThemeEngine.getAlertDialogTheme(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, themeId));
        builder.setTitle(getResources().getString(R.string.sort_by));
        builder.setSingleChoiceItems(R.array.sort, InfoManager.getSortType(context), (dialogInterface, i) -> {
            InfoManager.setSortType(context, i);
            fragDone.sortList(ClientHelper.Sort.getSort(i));
            dialogInterface.dismiss();
        });
        builder.show();
    }


    private void switchToExamsCompletedFragment() {
        FragmentManager manager = getSupportFragmentManager();
        if (fragDone != null && fragDone != active) {
            if (active != null) manager.beginTransaction().show(fragDone).hide(active).commit();
            else manager.beginTransaction().show(fragDone).commit();
        }
        invalidateOptionsMenu();
    }

    private void switchToExamsReservationsFragment() {
        FragmentManager manager = getSupportFragmentManager();
        if (fragRes != null && fragRes != active) {
            if (active != null) manager.beginTransaction().show(fragRes).hide(active).commit();
            else manager.beginTransaction().show(fragRes).commit();
        }
        invalidateOptionsMenu();
    }

    private void switchToExamsSearchFragment() {
        FragmentManager manager = getSupportFragmentManager();
        if (fragDoable != null && fragDoable != active) {
            if (active != null) manager.beginTransaction().show(fragDoable).hide(active).commit();
            else manager.beginTransaction().show(fragDoable).commit();
        }
        invalidateOptionsMenu();
    }


    public void createTextSnackBar(int string_id, int length) {
        LayoutHelper.createTextSnackBar(mainLayout, string_id, length);
    }

    public void createRetrySnackBar(final int string_id, int length, View.OnClickListener listener) {
        LayoutHelper.createActionSnackBar(mainLayout, string_id, R.string.retry, length, listener);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putInt("tabSelected", itemId);
        getSupportFragmentManager().putFragment(outState, "completed", fragDone);
        getSupportFragmentManager().putFragment(outState, "reservations", fragRes);
        getSupportFragmentManager().putFragment(outState, "doable", fragDoable);
        if (active != null) getSupportFragmentManager().putFragment(outState, "active", active);
    }
}
