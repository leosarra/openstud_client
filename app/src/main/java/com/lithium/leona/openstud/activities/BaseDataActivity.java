package com.lithium.leona.openstud.activities;

import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import androidx.appcompat.app.AppCompatActivity;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Student;

abstract class BaseDataActivity extends AppCompatActivity {
    Student student;
    Openstud os;

    public BaseDataActivity() {
        super();
    }

    public boolean initData() {
        os = InfoManager.getOpenStud(getApplication());
        student = InfoManager.getInfoStudentCached(this, os);
        if (os == null || student == null) {
            ClientHelper.rebirthApp(this);
            return false;
        }
        return true;
    }
}
