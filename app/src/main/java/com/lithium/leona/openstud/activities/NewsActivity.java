package com.lithium.leona.openstud.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.NewsAdapter;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;
import com.lithium.leona.openstud.listeners.DelayedDrawerListener;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import lithium.openstud.driver.core.News;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class NewsActivity extends AppCompatActivity {

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
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    private NavigationView nv;
    private DelayedDrawerListener ddl;
    private Openstud os;
    private LocalDateTime lastUpdate;
    private String locale;
    private NewsAdapter adapter;
    private List<News> news;
    private NewsHandler h = new NewsHandler(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applyNewsTheme(this);
        setContentView(R.layout.activity_news);
        ButterKnife.bind(this);
        os = InfoManager.getOpenStud(this);
        Student student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) {
            InfoManager.clearSharedPreferences(getApplication());
            Intent i = new Intent(NewsActivity.this, LauncherActivity.class);
            startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        nv = LayoutHelper.setupNavigationDrawer(this, mDrawerLayout);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.news);
        View headerLayout = nv.getHeaderView(0);

        TextView navTitle = headerLayout.findViewById(R.id.nav_title);
        navTitle.setText(getString(R.string.fullname, student.getFirstName(), student.getLastName()));
        TextView subTitle = headerLayout.findViewById(R.id.nav_subtitle);
        subTitle.setText(student.getStudentID());
        setupDrawerListener();
        locale = getLocale();
        emptyText.setText(getResources().getString(R.string.no_news_found));
        news = new LinkedList<>();
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        adapter = new NewsAdapter(this, news, v -> {
            int itemPosition = rv.getChildLayoutPosition(v);
            if(itemPosition<news.size()) {
                News el = news.get(itemPosition);
                ClientHelper.createCustomTab(this,el.getUrl());
            }
        });
        rv.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh1, R.color.refresh2, R.color.refresh3);
        List<News> news_cached = InfoManager.getNewsCached(this,os,locale);
        if (news_cached != null && !news_cached.isEmpty()) {
            news.addAll(news_cached);
        }
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setOnRefreshListener(this::refreshNews);
        if (savedInstanceState==null) refreshNews();

    }

    private void refreshNews(){
        if (os == null) return;
        setRefreshing(true);
        setButtonReloadStatus(false);
        new Thread(() -> {
            List<News> update = null;
            try {
                update = InfoManager.getNews(this, os, locale);
                if (update == null)
                    h.sendEmptyMessage(ClientHelper.Status.UNEXPECTED_VALUE.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.OK.getValue());

            } catch (OpenstudConnectionException e) {
                h.sendEmptyMessage(ClientHelper.Status.CONNECTION_ERROR.getValue());
                e.printStackTrace();
            } catch (OpenstudInvalidResponseException e) {
                if(e.isRateLimit()) h.sendEmptyMessage(ClientHelper.Status.RATE_LIMIT.getValue());
                else h.sendEmptyMessage(ClientHelper.Status.INVALID_RESPONSE.getValue());
                e.printStackTrace();
            }
            if (update == null) {
                setRefreshing(false);
                setButtonReloadStatus(true);
                swapViews(news);
                return;
            }
            updateTimer();
            refreshDataSet(update);
        }).start();
    }

    public synchronized void refreshDataSet(List<News> update) {
        boolean flag = false;
        if (update != null && !news.equals(update)) {
            flag = true;
            news.clear();
            news.addAll(update);
        }
        final boolean finalFlag = flag;
        runOnUiThread(() -> {
            if (finalFlag) adapter.notifyDataSetChanged();
            swapViews(news);
            swipeRefreshLayout.setRefreshing(false);
            emptyButton.setEnabled(true);
        });
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
                onBackPressed();
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
                ClientHelper.startDrawerActivity(item, NewsActivity.this);
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

    private String getLocale(){
        if (!Locale.getDefault().getLanguage().equals("it")) return "en";
        else return "it";
    }

    private void setRefreshing(final boolean bool) {
        runOnUiThread(() -> swipeRefreshLayout.setRefreshing(bool));
    }

    private void setButtonReloadStatus(final boolean bool) {
        runOnUiThread(() -> emptyButton.setEnabled(bool));
    }

    private synchronized void updateTimer() {
        lastUpdate = LocalDateTime.now();
    }

    private synchronized LocalDateTime getTimer() {
        return lastUpdate;
    }

    private void swapViews(final List<News> news) {
        runOnUiThread(() -> {
            if (news == null || news.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        });
    }

    /*
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putInt("tabSelected", itemId);
        Parcelable recyclerViewState;
        recyclerViewState = rv.getLayoutManager().onSaveInstanceState();
        outState.putParcelable("recyclerState", recyclerViewState);
    }
    */

    protected void onRestart() {
        super.onRestart();
        LocalDateTime time = getTimer();
        if (time == null || Duration.between(time, LocalDateTime.now()).toMinutes() > 60) refreshNews();
    }

    private static class NewsHandler extends Handler {
        private final WeakReference<NewsActivity> activity;

        private NewsHandler(NewsActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final NewsActivity activity = this.activity.get();
            if (activity == null) return;
            View.OnClickListener ocl = v -> activity.refreshNews();
            if (msg.what == ClientHelper.Status.CONNECTION_ERROR.getValue()) {
                LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            } else if (msg.what == ClientHelper.Status.INVALID_RESPONSE.getValue()) {
                LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.connection_error, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            } else if (msg.what == ClientHelper.Status.UNEXPECTED_VALUE.getValue()) {
                LayoutHelper.createTextSnackBar(activity.getWindow().getDecorView(), R.string.invalid_response_error, Snackbar.LENGTH_LONG);
            } else if (msg.what == ClientHelper.Status.RATE_LIMIT.getValue()) {
                LayoutHelper.createActionSnackBar(activity.mDrawerLayout, R.string.rate_limit, R.string.retry, Snackbar.LENGTH_LONG, ocl);
            }
        }
    }
}
