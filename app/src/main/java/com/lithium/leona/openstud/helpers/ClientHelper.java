package com.lithium.leona.openstud.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;

import org.threeten.bp.ZoneId;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.OpenstudHelper;

public class ClientHelper {

    public enum Status {
        OK(0), CONNECTION_ERROR(1), INVALID_RESPONSE(2), INVALID_CREDENTIALS(3), USER_NOT_ENABLED(4), UNEXPECTED_VALUE(5),
        EXPIRED_CREDENTIALS(6), FAILED_DELETE(7), OK_DELETE(8), FAILED_GET(9), FAILED_GET_IO(10), PLACE_RESERVATION_OK(11), PLACE_RESERVATION_CONNECTION(12),
        PLACE_RESERVATION_INVALID_RESPONSE(13), ALREADY_PLACED(14);
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

    public static  ArrayList<Entry> generateMarksPoints(List<ExamDone> exams, int laude){
        LinkedList<ExamDone> temp = new LinkedList<>(exams);
        Collections.reverse(temp);
        ArrayList<Entry> ret = new ArrayList<>();
        for (ExamDone exam : temp){
            int result = exam.getResult();
            if (!(result>=18 && exam.isPassed())) continue;
            if (result>30) result=laude;
            ZoneId zoneId = ZoneId.systemDefault();
            Timestamp timestamp = new Timestamp(exam.getDate().atStartOfDay(zoneId).toEpochSecond());
            ret.add(new Entry(timestamp.getTime()*1000L, result));
        }
        return ret;
    }


    public static ArrayList<Entry> generateWeightPoints(List<ExamDone> exams, int laude) {
        LinkedList<ExamDone> temp = new LinkedList<>(exams);
        Collections.reverse(temp);
        List<ExamDone> placeholder = new LinkedList<>();
        ArrayList<Entry> ret = new ArrayList<>();
        for (ExamDone exam : temp) {
            if (!(exam.getResult() >= 18 && exam.isPassed())) continue;
            placeholder.add(exam);
            float average = (float) OpenstudHelper.computeWeightedAverage(placeholder, laude);
            ZoneId zoneId = ZoneId.systemDefault();
            Timestamp timestamp = new Timestamp(exam.getDate().atStartOfDay(zoneId).toEpochSecond());
            ret.add(new Entry(timestamp.getTime()*1000L, average));
        }
        return ret;
    }

    public static ArrayList<BarEntry> generateMarksBar(List<ExamDone> exams){
        Map<Integer,Integer> map = new HashMap<>();
        for (ExamDone exam : exams) {
            if (!(exam.getResult()>=18 && exam.isPassed())) continue;
            int result = exam.getResult();
            if (result > 30) result = 31;
            if (map.containsKey(result)) {
                map.put(result,map.get(result)+1);
            }
            else map.put(result,0);
        }
        ArrayList<BarEntry> entries = new ArrayList<>();
        SortedSet<Integer> sortedSet = new TreeSet<>(map.keySet());

        for (Integer key : sortedSet){
            entries.add(new BarEntry(key,map.get(key)));
        }
        return entries;
    }


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void hideKeyboard(View v, Context context){
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static Snackbar createTextSnackBar(View v, int string_id, int length){
        Snackbar snackbar = Snackbar
                .make(v, string_id, length);
        snackbar.show();
        return snackbar;
    }

    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }


    public void requestInternetPermissions(Activity activity){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.INTERNET}, 123);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 123);
        }
    }

    public static boolean requestReadWritePermissions(Activity activity){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return true;
        else return false;
    }

}
