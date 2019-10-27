package com.lithium.leona.openstud.activities;

import androidx.appcompat.app.AppCompatActivity;

import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.lithium.leona.openstud.helpers.ThemeEngine;

import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Student;

abstract class BaseDataActivity extends AppCompatActivity {
    Student student;
    Openstud os;
    ThemeEngine.Theme currentTheme;
    public BaseDataActivity() {
        super();
    }

    public boolean initData() {
        currentTheme = ThemeEngine.resolveTheme(this, PreferenceManager.getTheme(this));
        os = InfoManager.getOpenStud(this);
        student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) {
            ClientHelper.rebirthApp(this, null);
            return false;
        }
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        ThemeEngine.Theme newTheme = ThemeEngine.resolveTheme(this, PreferenceManager.getTheme(this));
        if (newTheme != currentTheme) this.recreate();
    }
}
