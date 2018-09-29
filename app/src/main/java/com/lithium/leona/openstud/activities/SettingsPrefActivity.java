package com.lithium.leona.openstud.activities;

import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toolbar;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.helpers.LayoutHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import java.util.prefs.Preferences;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsPrefActivity extends AppCompatActivity {
    @BindView(R.id.toolbar) android.support.v7.widget.Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.applySettingsTheme(this);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        LayoutHelper.setupToolbar(this,toolbar,R.drawable.ic_baseline_arrow_back);
        getSupportActionBar().setTitle(R.string.settings);
        // load settings fragment
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            Preference theme = findPreference(getString(R.string.key_theme));
            theme.setOnPreferenceChangeListener((preference, o) -> {
                String newTheme = o.toString();
                Context context = getContext();
                if (context == null) return false;
                if (newTheme.equals("0")) ThemeEngine.setTheme(context,ThemeEngine.Theme.LIGHT);
                else if (newTheme.equals("1")) ThemeEngine.setTheme(context,ThemeEngine.Theme.DARK);
                return true;
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


}