package com.lithium.leona.openstud.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsPrefActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    android.support.v7.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applySettingsTheme(this);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this, toolbar, R.drawable.ic_baseline_arrow_back);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.settings);
        // load settings fragment
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        private int alertDialogTheme;
        ThemeEngine.Theme oldTheme;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            SettingsPrefActivity activity = (SettingsPrefActivity) getActivity();
            addPreferencesFromResource(R.xml.pref_main);
            alertDialogTheme = ThemeEngine.getAlertDialogTheme(activity);
            oldTheme = ThemeEngine.getTheme(activity);
            Preference theme = findPreference(getString(R.string.key_theme));
            theme.setOnPreferenceChangeListener((preference, o) -> {
                String newTheme = o.toString();
                Context context = getContext();
                if (context == null) return false;
                int id = Integer.parseInt(newTheme);
                if (ThemeEngine.Theme.getTheme(id) == oldTheme) return false;
                ThemeEngine.setTheme(context, ThemeEngine.Theme.getTheme(id));
                oldTheme = ThemeEngine.Theme.getTheme(id);
                activity.createRestartDialog(alertDialogTheme);
                return true;
            });
            Preference delete = findPreference(getString(R.string.key_delete));
            delete.setOnPreferenceClickListener(preference -> {
                if (activity == null) return false;
                boolean result = ClientHelper.requestReadWritePermissions(activity);
                if (!result) return false;
                String directory = Environment.getExternalStorageDirectory() + "/OpenStud";
                File dirs = new File(directory);
                try {
                    FileUtils.deleteDirectory(dirs);
                    LayoutHelper.createTextSnackBar(getView(), R.string.success_delete_pdf, Snackbar.LENGTH_LONG);
                } catch (IOException e) {
                    e.printStackTrace();
                    LayoutHelper.createTextSnackBar(getView(), R.string.failed_delete_pdf, Snackbar.LENGTH_LONG);
                }
                return true;
            });
            Preference enableLesson = findPreference(getString(R.string.key_enable_lesson));
            enableLesson.setOnPreferenceChangeListener((preference, newValue) -> {
                Boolean enable = (Boolean) newValue;
                if (enable) activity.createCalendarNotification(alertDialogTheme);
                PreferenceManager.setCalendarNotificationEnabled(getContext(), false);
                return true;
            });
            Preference laude = findPreference(getString(R.string.key_default_laude));
            laude.setOnPreferenceChangeListener((preference, newValue) -> {
                String newLaude = (String) newValue;
                boolean valid = true;
                try {
                    int value = Integer.parseInt(newLaude);
                    if (value > 34 || value < 30) valid = false;
                } catch (NumberFormatException e) {
                    valid = false;
                }
                if (valid) PreferenceManager.setStatsNotificationEnabled(getContext(), false);
                return valid;
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void createRestartDialog(int styleId) {
        new AlertDialog.Builder(new ContextThemeWrapper(this, styleId))
                .setTitle(getResources().getString(R.string.restart_required))
                .setMessage(getResources().getString(R.string.restart_required_description))
                .setPositiveButton(getResources().getString(R.string.restart_ok), (dialog, which) -> {
                    Intent i = new Intent(SettingsPrefActivity.this, LauncherActivity.class);
                    startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                })
                .setNegativeButton(getResources().getString(R.string.restart_cancel), (dialogInterface, i) -> {
                })
                .show();
    }

    private void createCalendarNotification(int styleId) {
        new AlertDialog.Builder(new ContextThemeWrapper(this, styleId))
                .setTitle(getResources().getString(R.string.experimental_feature))
                .setMessage(getResources().getString(R.string.calendar_feature_description))
                .setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                })
                .show();
    }


}