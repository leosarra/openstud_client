package com.lithium.leona.openstud.activities;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.fragments.TabFragment;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.mikepenz.materialdrawer.Drawer;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Student;


public class PaymentsActivity extends AppCompatActivity {
    @BindView(R.id.main_layout)
    LinearLayout mainLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    private Openstud os;
    private Student student;
    private Drawer drawer;
    private SparseArray<Snackbar> snackBarMap = new SparseArray<>();
    private int selectedItem = -1;
    private TabFragment tabFrag;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyPaymentsTheme(this);
        setContentView(R.layout.activity_payments);
        ButterKnife.bind(this);
        os = InfoManager.getOpenStud(this);
        student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) ClientHelper.rebirthApp(this);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        drawer = LayoutHelper.applyDrawer(this, toolbar, student);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.payments);
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getInt("tabSelected", -1);
            tabFrag = (TabFragment) getSupportFragmentManager().getFragment(savedInstanceState, "tab");
        } else tabFrag = TabFragment.newInstance(selectedItem);
        fragmentManager.beginTransaction().replace(R.id.content_frame, tabFrag).commit();

    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }


    public synchronized void createTextSnackBar(int string_id, int length) {
        if (snackBarMap.get(string_id, null) != null) return;
        Snackbar snackbar = LayoutHelper.createTextSnackBar(mainLayout, string_id, length);
        snackBarMap.put(string_id, snackbar);
    }

    public synchronized void createActionSnackBar(final int string_id, int length, View.OnClickListener listener) {
        if (snackBarMap.get(string_id, null) != null) return;
        Snackbar snackbar = Snackbar
                .make(mainLayout, getResources().getString(string_id), length).setAction(R.string.retry, listener);
        snackBarMap.put(string_id, snackbar);
        snackbar.addCallback(new Snackbar.Callback() {
            public void onDismissed(Snackbar snackbar, int event) {
                removeKeyFromMap(string_id);
            }
        });
        snackbar.show();
    }

    private synchronized void removeKeyFromMap(int id) {
        snackBarMap.remove(id);
    }

    public void updateSelectTab(int item) {
        selectedItem = item;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tabSelected", selectedItem);
        getSupportFragmentManager().putFragment(outState, "tab", tabFrag);
    }
}
