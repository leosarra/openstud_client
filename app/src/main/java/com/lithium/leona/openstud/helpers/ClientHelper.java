package com.lithium.leona.openstud.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContract;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.activities.AboutActivity;
import com.lithium.leona.openstud.activities.CalendarActivity;
import com.lithium.leona.openstud.activities.ExamsActivity;
import com.lithium.leona.openstud.activities.LauncherActivity;
import com.lithium.leona.openstud.activities.NewsActivity;
import com.lithium.leona.openstud.activities.PaymentsActivity;
import com.lithium.leona.openstud.activities.ProfileActivity;
import com.lithium.leona.openstud.activities.SearchClassroomActivity;
import com.lithium.leona.openstud.activities.SettingsPrefActivity;
import com.lithium.leona.openstud.activities.StatsActivity;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.widgets.GradesWidget;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import lithium.openstud.driver.core.Event;
import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.OpenstudHelper;

public class ClientHelper {

    public static Date getDateWithoutTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static List<Event> orderByStartTime(List<Event> events, boolean ascending) {
        Collections.sort(events, (o1, o2) -> {
            if (o1.getStart() == null && o2.getStart() == null) return 0;
            if (ascending)
                if (o1.getStart() == null) return 1;
                else if (o2.getStart() == null) return -1;
                else return o1.getStart().compareTo(o2.getStart());
            else {
                if (o1.getStart() == null) return -1;
                else if (o2.getStart() == null) return 1;
                else return o2.getStart().compareTo(o1.getStart());
            }
        });
        return events;
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

    public static void updateGradesWidget(Activity activity, boolean cached) {
        Intent intent = new Intent(activity, GradesWidget.class);
        intent.setAction("MANUAL_UPDATE");
        intent.putExtra("cached", cached);
        int[] ids = AppWidgetManager.getInstance(activity.getApplication()).getAppWidgetIds(new ComponentName(activity.getApplication(), GradesWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        activity.sendBroadcast(intent);
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
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            //No browser that supports custom tabs
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    public static void createConfirmDeleteReservationDialog(Activity activity, final ExamReservation res, Runnable action) {
        if (activity == null) return;
        int themeId = ThemeEngine.getAlertDialogTheme(activity);
        activity.runOnUiThread(() -> new AlertDialog.Builder(new ContextThemeWrapper(activity, themeId))
                .setTitle(activity.getResources().getString(R.string.delete_res_dialog_title))
                .setMessage(activity.getResources().getString(R.string.delete_res_dialog_description, res.getExamSubject()))
                .setPositiveButton(activity.getResources().getString(R.string.delete_ok), (dialog, which) -> new Thread(action).start())
                .setNegativeButton(activity.getResources().getString(R.string.delete_abort), (dialog, which) -> {
                })
                .show());
    }

    public static boolean hasPassedExams(List<ExamDone> exams) {
        if (exams == null || exams.isEmpty()) return false;
        for (ExamDone exam : exams) {
            if (exam.getResult() >= 18 && exam.isPassed()) return true;
        }
        return false;
    }

    public static void addReservationToCalendar(Activity activity, final ExamReservation res) {
        ZoneId zoneId = ZoneId.systemDefault();
        Timestamp timestamp = new Timestamp(res.getExamDate().atStartOfDay(zoneId).toEpochSecond());
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        String title;
        if (Locale.getDefault().getLanguage().equals("it"))
            title = "Esame: " + res.getExamSubject();
        else title = "Exam: " + res.getExamSubject();
        intent.putExtra(CalendarContract.Events.TITLE, title);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                timestamp.getTime() * 1000L);
        intent.putExtra(CalendarContract.Events.ALL_DAY, true);
        activity.startActivity(intent);
    }

    public static void startDrawerActivity(int item, Activity activity){
        switch (item) {
            case R.id.payments_menu: {
                if (activity instanceof PaymentsActivity) break;
                Intent intent = new Intent(activity, PaymentsActivity.class);
                activity.startActivity(intent);
                break;
            }

            case R.id.calendar_menu: {
                if (activity instanceof CalendarActivity) break;
                Intent intent = new Intent(activity, CalendarActivity.class);
                activity.startActivity(intent);
                break;
            }

            case R.id.profile_menu: {
                if (activity instanceof ProfileActivity) break;
                Intent intent = new Intent(activity, ProfileActivity.class);
                activity.startActivity(intent);
                break;
            }

            case R.id.exit_menu: {
                InfoManager.clearSharedPreferences(activity.getApplication());
                Intent i = new Intent(activity, LauncherActivity.class);
                activity.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                break;
            }

            case R.id.classrooms_menu: {
                if (activity instanceof SearchClassroomActivity) break;
                Intent intent = new Intent(activity, SearchClassroomActivity.class);
                activity.startActivity(intent);
                break;
            }

            case R.id.about_menu: {
                Intent intent = new Intent(activity, AboutActivity.class);
                activity.startActivity(intent);
                break;
            }
            case R.id.settings_menu: {
                Intent intent = new Intent(activity, SettingsPrefActivity.class);
                activity.startActivity(intent);
                break;
            }
            case R.id.stats_menu: {
                if (activity instanceof StatsActivity) break;
                Intent intent = new Intent(activity, StatsActivity.class);
                activity.startActivity(intent);
                break;
            }
            case R.id.exams_menu: {
                if (activity instanceof ExamsActivity) break;
                Intent intent = new Intent(activity, ExamsActivity.class);
                activity.startActivity(intent);
                break;
            }

            case R.id.news_menu: {
                if (activity instanceof NewsActivity) break;
                Intent intent = new Intent(activity, NewsActivity.class);
                activity.startActivity(intent);
                break;
            }
        }
    }
    public static void addEventToCalendar(Activity activity, final Event ev) {
        switch (ev.getEventType()) {
            case LESSON: {
                ZoneId zoneId = ZoneId.systemDefault();
                Timestamp timestampStart = new Timestamp(ev.getStart().atZone(zoneId).toEpochSecond());
                Timestamp timestampEnd = new Timestamp(ev.getEnd().atZone(zoneId).toEpochSecond());
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setType("vnd.android.cursor.item/event");
                String title = ev.getDescription();
                intent.putExtra(CalendarContract.Events.TITLE, title);
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        timestampStart.getTime() * 1000L);
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        timestampEnd.getTime() * 1000L);
                intent.putExtra(CalendarContract.Events.ALL_DAY, false);
                activity.startActivity(intent);
                break;
            }
            case DOABLE:
            case RESERVED: {
                ZoneId zoneId = ZoneId.systemDefault();
                Timestamp timestamp = new Timestamp(ev.getExamDate().atStartOfDay(zoneId).toEpochSecond());
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setType("vnd.android.cursor.item/event");
                String title;
                if (Locale.getDefault().getLanguage().equals("it"))
                    title = "Esame: " + ev.getReservation().getExamSubject();
                else title = "Exam: " + ev.getReservation().getExamSubject();
                intent.putExtra(CalendarContract.Events.TITLE, title);
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        timestamp.getTime() * 1000L);
                intent.putExtra(CalendarContract.Events.ALL_DAY, true);
                activity.startActivity(intent);
            }
        }

    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File file : fileOrDirectory.listFiles()) {
                deleteRecursive(file);
            }
        }
        fileOrDirectory.delete();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean canPlaceReservation(ExamReservation res) {
        return (ChronoUnit.DAYS.between(res.getStartDate(), LocalDate.from(LocalDateTime.now())) >= 0 && ChronoUnit.DAYS.between(res.getEndDate(), LocalDate.from(LocalDateTime.now())) <= 0);
    }

    public static boolean canDeleteReservation(ExamReservation res) {
        return !(ChronoUnit.DAYS.between(res.getEndDate(), LocalDate.from(LocalDateTime.now())) >= 1);
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

    public static boolean requestReadWritePermissions(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                    .requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
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


    public enum Status {
        OK(0), CONNECTION_ERROR(1), INVALID_RESPONSE(2), INVALID_CREDENTIALS(3), USER_NOT_ENABLED(4), UNEXPECTED_VALUE(5),
        EXPIRED_CREDENTIALS(6), FAILED_DELETE(7), OK_DELETE(8), FAILED_GET(9), FAILED_GET_IO(10), PLACE_RESERVATION_OK(11), PLACE_RESERVATION_CONNECTION(12),
        PLACE_RESERVATION_INVALID_RESPONSE(13), ALREADY_PLACED(14), CLOSED_RESERVATION(15), FAIL_LOGIN(16), ENABLE_BUTTONS(17), RECOVERY_OK(18), INVALID_ANSWER(19),
        INVALID_STUDENT_ID(20), NO_RECOVERY(21), CONNECTION_ERROR_RECOVERY(22), RATE_LIMIT(23);
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

        public static Sort getSort(int type) {
            if (type == 0) return Date;
            else if (type == 1) return Mark;
            return null;
        }

        public int getValue() {
            return value;
        }

    }

}
