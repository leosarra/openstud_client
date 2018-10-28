package com.lithium.leona.openstud.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.lithium.leona.openstud.R;

import org.threeten.bp.ZoneId;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.OpenstudHelper;

public class ClientHelper {

    public enum Status {
        OK(0), CONNECTION_ERROR(1), INVALID_RESPONSE(2), INVALID_CREDENTIALS(3), USER_NOT_ENABLED(4), UNEXPECTED_VALUE(5),
        EXPIRED_CREDENTIALS(6), FAILED_DELETE(7), OK_DELETE(8), FAILED_GET(9), FAILED_GET_IO(10), PLACE_RESERVATION_OK(11), PLACE_RESERVATION_CONNECTION(12),
        PLACE_RESERVATION_INVALID_RESPONSE(13), ALREADY_PLACED(14), CLOSED_RESERVATION(15), FAIL_LOGIN(16), ENABLE_BUTTONS(17), RECOVERY_OK(18), INVALID_ANSWER(19),
        INVALID_STUDENT_ID(20), NO_RECOVERY(21), CONNECTION_ERROR_RECOVERY(22);
        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    public enum Sort {
        Date(0), Mark(1);
        private int value;

        Sort(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Sort getSort(int type) {
            if (type == 0) return Date;
            else if (type == 1) return Mark;
            return null;
        }

    }

    public static ArrayList<Entry> generateMarksPoints(List<ExamDone> exams, int laude) {
        LinkedList<ExamDone> temp = new LinkedList<>(exams);
        Collections.reverse(temp);
        ArrayList<Entry> ret = new ArrayList<>();
        for (ExamDone exam : temp) {
            if (exam.getDate() == null) continue;
            int result = exam.getResult();
            if (!(result >= 18 && exam.isPassed())) continue;
            if (result > 30) result = laude;
            ZoneId zoneId = ZoneId.systemDefault();
            Timestamp timestamp = new Timestamp(exam.getDate().atStartOfDay(zoneId).toEpochSecond());
            ret.add(new Entry(timestamp.getTime() * 1000L, result));
        }
        return ret;
    }


    public static ArrayList<Entry> generateWeightPoints(List<ExamDone> exams, int laude) {
        LinkedList<ExamDone> temp = new LinkedList<>(exams);
        Collections.reverse(temp);
        List<ExamDone> placeholder = new LinkedList<>();
        ArrayList<Entry> ret = new ArrayList<>();
        for (ExamDone exam : temp) {
            if (exam.getDate() == null) continue;
            if (!(exam.getResult() >= 18 && exam.isPassed())) continue;
            placeholder.add(exam);
            float average = (float) OpenstudHelper.computeWeightedAverage(placeholder, laude);
            ZoneId zoneId = ZoneId.systemDefault();
            Timestamp timestamp = new Timestamp(exam.getDate().atStartOfDay(zoneId).toEpochSecond());
            ret.add(new Entry(timestamp.getTime() * 1000L, average));
        }
        return ret;
    }

    public static ArrayList<BarEntry> generateMarksBar(List<ExamDone> exams) {
        @SuppressLint("UseSparseArrays") Map<Integer, Integer> map = new HashMap<>();
        for (ExamDone exam : exams) {
            if (!(exam.getResult() >= 18 && exam.isPassed())) continue;
            int result = exam.getResult();
            if (result > 30) result = 31;
            if (map.containsKey(result)) {
                map.put(result, map.get(result) + 1);
            } else map.put(result, 1);
        }
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (Integer key : map.keySet()) {
            entries.add(new BarEntry(key, map.get(key)));
        }
        return entries;
    }

    public static void createCustomTab(Context context, String url) {
        Bitmap closeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_arrow_back);
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(context, R.color.redSapienza));
        builder.setCloseButtonIcon(closeIcon);
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }


    public static boolean hasPassedExams(List<ExamDone> exams) {
        for (ExamDone exam : exams) {
            if (exam.getResult() >= 18 && exam.isPassed()) return true;
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void hideKeyboard(View v, Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }


    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }


    public void requestInternetPermissions(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.INTERNET}, 123);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 123);
        }
    }

    public static boolean requestReadWritePermissions(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

}
