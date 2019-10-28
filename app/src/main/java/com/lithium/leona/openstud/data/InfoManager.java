package com.lithium.leona.openstud.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import com.lithium.leona.openstud.BuildConfig;
import com.lithium.leona.openstud.helpers.ClientHelper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import org.threeten.bp.LocalDateTime;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.OpenstudBuilder;
import lithium.openstud.driver.core.OpenstudHelper;
import lithium.openstud.driver.core.models.Event;
import lithium.openstud.driver.core.models.ExamDoable;
import lithium.openstud.driver.core.models.ExamDone;
import lithium.openstud.driver.core.models.ExamReservation;
import lithium.openstud.driver.core.models.Isee;
import lithium.openstud.driver.core.models.Lesson;
import lithium.openstud.driver.core.models.News;
import lithium.openstud.driver.core.models.Student;
import lithium.openstud.driver.core.models.StudentCard;
import lithium.openstud.driver.core.models.Tax;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class InfoManager {
    private static Moshi moshi;
    private static Openstud os;
    private static SharedPreferences pref;
    private static Student student;
    private static Isee isee;
    private static List<Tax> paidTaxes;
    private static List<Tax> unpaidTaxes;
    private static List<ExamDone> examsDone;
    private static List<ExamDoable> examsDoable;
    private static List<ExamReservation> reservations;
    private static List<News> news;
    private static List<ExamDone> fakeExams;
    private static List<Event> events;
    private static List<String> filter;
    private static List<Event> theatre_events;
    private static StudentCard card;

    private static synchronized void setupSharedPreferences(Context context) {
        if (pref != null) return;
        if (moshi == null) moshi = new Moshi.Builder().build();
        pref = context.getSharedPreferences("OpenStudPref", 0); // 0 - for private mode
    }

    public static void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }

    public static Openstud getOpenStud(Context context) {
        setupSharedPreferences(context);
        if (getStudentId(context) == null || getPassword(context) == null) return null;
        synchronized (InfoManager.class) {
            if (os != null) return os;
            OpenstudBuilder osb = new OpenstudBuilder().setStudentID(getStudentId(context)).setPassword(getPassword(context)).setRetryCounter(3).forceReadyState();
            if (BuildConfig.DEBUG) osb.setLogger(Logger.getLogger("OpenStud_client"));
            os = osb.build();
            return os;
        }
    }


    public static Openstud getOpenStudRecovery(Context context, String studentId) {
        setupSharedPreferences(context);
        if (studentId == null) return null;
        OpenstudBuilder osb = new OpenstudBuilder().setStudentID(studentId).setRetryCounter(3);
        if (BuildConfig.DEBUG) osb.setLogger(Logger.getLogger("OpenStud_client"));
        return osb.build();
    }

    public static Openstud getOpenStud(Context context, String studentId, String password) {
        setupSharedPreferences(context);
        if (studentId == null || password == null || password.isEmpty()) return null;
        OpenstudBuilder osb = new OpenstudBuilder().setStudentID(studentId).setPassword(password).setRetryCounter(3);
        if (BuildConfig.DEBUG) osb.setLogger(Logger.getLogger("OpenStud_client"));
        return osb.build();
    }

    public static void saveOpenStud(Context context, Openstud openstud, String studentId, String password, boolean save) {
        setupSharedPreferences(context);
        setNamePassword(context, studentId, password);
        setSaveFlag(context, save);
        synchronized (InfoManager.class) {
            os = openstud;
        }
    }

    public static Student getInfoStudentCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        synchronized (InfoManager.class) {
            if (student != null) return student;
            oldObj = pref.getString("student", "null");
        }
        Student ret = null;
        try {
            JsonAdapter<Student> jsonAdapter = moshi.adapter(Student.class);
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                student = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }


    public static Student getInfoStudent(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<Student> jsonAdapter = moshi.adapter(Student.class);
        Student newStudent = os.getInfoStudent();
        synchronized (InfoManager.class) {
            student = newStudent;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(student);
            prefsEditor.putString("student", json);
            prefsEditor.apply();
        }
        return newStudent;
    }


    public static StudentCard getStudentCardCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<StudentCard> jsonAdapter = moshi.adapter(StudentCard.class);
        synchronized (InfoManager.class) {
            if (card != null) return card;
            oldObj = pref.getString("studentCard", null);
        }
        StudentCard ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                card = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static StudentCard getStudentCard(Context context, Openstud os, Student student) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<StudentCard> jsonAdapter = moshi.adapter(StudentCard.class);
        StudentCard newCard = os.getStudentCard(student, true);
        synchronized (InfoManager.class) {
            card = newCard;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(card);
            prefsEditor.putString("studentCard", json);
            prefsEditor.apply();
        }
        return newCard;
    }

    public static List<Event> getEventsCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<Event>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Event.class));
        synchronized (InfoManager.class) {
            if (events != null) return new LinkedList<>(events);
            oldObj = pref.getString("events", "null");
        }
        List<Event> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                events = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }


    public static List<Event> getEvents(Context context, Openstud os, Student student) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<Event>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Event.class));
        Map<String, List<Lesson>> newTimetable = new HashMap<>();
        if (PreferenceManager.isLessonEnabled(context))
            newTimetable = os.getTimetable(os.getExamsDoable());
        List<lithium.openstud.driver.core.models.Event> newEvents = os.getCalendarEvents(student);
        if (newEvents == null) return null;
        if (newTimetable != null && !newTimetable.isEmpty())
            newEvents.addAll(OpenstudHelper.generateEventsFromTimetable(newTimetable));
        newEvents.addAll(OpenstudHelper.generateEventsFromTimetable(ClientHelper.generateLessonsForCustomCourses(PreferenceManager.getCustomCourses(context))));
        synchronized (InfoManager.class) {
            events = newEvents;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(events);
            prefsEditor.putString("events", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newEvents);
    }


    public static void saveFakeExams(Context context, List<ExamDone> exams) {
        setupSharedPreferences(context);
        JsonAdapter<List<ExamDone>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDone.class));
        String obj;
        synchronized (InfoManager.class) {
            if (exams != null) fakeExams = new LinkedList<>(exams);
            else fakeExams = new LinkedList<>();
            obj = jsonAdapter.toJson(fakeExams);
            SharedPreferences.Editor prefsEditor = pref.edit();
            prefsEditor.putString("fakeExams", obj);
            prefsEditor.apply();
        }
    }

    public static List<ExamDone> getFakeExams(Context context, Openstud os) {
        return _getFakeExams(context, os, true);
    }

    private static synchronized List<ExamDone> _getFakeExams(Context context, Openstud os, boolean removeDuplicates) {
        setupSharedPreferences(context);
        if (os == null) return null;
        if (removeDuplicates) InfoManager.removeDuplicatesFakeExams(context, os);
        String oldObj;
        JsonAdapter<List<ExamDone>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDone.class));
        synchronized (InfoManager.class) {
            if (fakeExams != null) return new LinkedList<>(fakeExams);
            oldObj = pref.getString("fakeExams", null);
            if (oldObj == null) return new LinkedList<>();
        }
        List<ExamDone> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        if (ret == null) return new LinkedList<>();
        return ret;
    }


    public static Isee getIseeCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<Isee> jsonAdapter = moshi.adapter(Isee.class);
        synchronized (InfoManager.class) {
            if (isee != null) return isee;
            oldObj = pref.getString("isee", "null");
        }
        Isee ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                isee = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Isee getIsee(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<Isee> jsonAdapter = moshi.adapter(Isee.class);
        Isee newIsee = os.getCurrentIsee();
        synchronized (InfoManager.class) {
            isee = newIsee;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(isee);
            prefsEditor.putString("isee", json);
            prefsEditor.apply();
        }
        return isee;
    }

    public static List<Tax> getPaidTaxesCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<Tax>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Tax.class));
        synchronized (InfoManager.class) {
            if (paidTaxes != null) return new LinkedList<>(paidTaxes);
            oldObj = pref.getString("paidTaxes", "null");
        }
        List<Tax> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                paidTaxes = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<ExamDone> getExamsDoneCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<ExamDone>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDone.class));
        synchronized (InfoManager.class) {
            if (examsDone != null) return new LinkedList<>(examsDone);
            oldObj = pref.getString("examsDone", "null");
        }
        List<ExamDone> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                examsDone = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<ExamDone> getExamsDone(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<ExamDone>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDone.class));
        List<ExamDone> newExamsDone = os.getExamsDone();
        synchronized (InfoManager.class) {
            examsDone = newExamsDone;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(examsDone);
            prefsEditor.putString("examsDone", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newExamsDone);
    }

    public static List<ExamDoable> getExamsDoableCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<ExamDoable>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDoable.class));
        synchronized (InfoManager.class) {
            if (examsDoable != null) return new LinkedList<>(examsDoable);
            oldObj = pref.getString("examsDoable", "null");
        }
        List<ExamDoable> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                examsDoable = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static List<ExamDoable> getExamsDoable(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<ExamDoable>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamDoable.class));
        List<ExamDoable> newExamsDoable = os.getExamsDoable();
        synchronized (InfoManager.class) {
            examsDoable = newExamsDoable;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(examsDoable);
            prefsEditor.putString("examsDoable", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newExamsDoable);
    }


    public static List<ExamReservation> getActiveReservationsCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<ExamReservation>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamReservation.class));
        synchronized (InfoManager.class) {
            if (reservations != null) return new LinkedList<>(reservations);
            oldObj = pref.getString("reservations", "null");
        }
        List<ExamReservation> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                reservations = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<ExamReservation> getActiveReservations(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<ExamReservation>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, ExamReservation.class));
        List<ExamReservation> newExamsDone = os.getActiveReservations();
        synchronized (InfoManager.class) {
            reservations = newExamsDone;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(reservations);
            prefsEditor.putString("reservations", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newExamsDone);
    }

    public static List<Tax> getPaidTaxes(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<Tax>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Tax.class));
        List<Tax> newPaidTaxes = os.getPaidTaxes();
        synchronized (InfoManager.class) {
            paidTaxes = newPaidTaxes;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(paidTaxes);
            prefsEditor.putString("paidTaxes", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newPaidTaxes);
    }

    public static List<Tax> getUnpaidTaxesCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<Tax>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Tax.class));
        synchronized (InfoManager.class) {
            if (unpaidTaxes != null) return new LinkedList<>(unpaidTaxes);
            oldObj = pref.getString("unpaidTaxes", "null");
        }
        List<Tax> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                unpaidTaxes = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<Tax> getUnpaidTaxes(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<Tax>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Tax.class));
        List<Tax> newUnpaidTaxes = os.getUnpaidTaxes();
        synchronized (InfoManager.class) {
            unpaidTaxes = newUnpaidTaxes;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(newUnpaidTaxes);
            prefsEditor.putString("unpaidTaxes", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newUnpaidTaxes);
    }

    public static List<News> getNews(Context context, Openstud os, String locale) throws OpenstudConnectionException, OpenstudInvalidResponseException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<News>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, News.class));
        List<News> newNews = os.getNews(locale, true, null, 0, null, null);
        synchronized (InfoManager.class) {
            news = newNews;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(newNews);
            prefsEditor.putString("news", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newNews);
    }

    public static List<News> getNewsCached(Context context, Openstud os, String locale) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<News>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, News.class));
        synchronized (InfoManager.class) {
            if (news != null) {
                if (!news.isEmpty() && !news.get(0).getLocale().equals(locale)) return null;
                return new LinkedList<>(news);
            }
            oldObj = pref.getString("news", "null");
        }
        List<News> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                news = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        if (ret != null && !ret.isEmpty() && !ret.get(0).getLocale().equals(locale)) return null;
        return ret;
    }


    public static List<Event> getEventsUniversity(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException {
        setupSharedPreferences(context);
        if (os == null) return null;
        JsonAdapter<List<Event>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Event.class));
        List<Event> newEvents = os.getNewsletterEvents();
        synchronized (InfoManager.class) {
            theatre_events = newEvents;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = jsonAdapter.toJson(newEvents);
            prefsEditor.putString("eventsUniversity", json);
            prefsEditor.apply();
        }
        return new LinkedList<>(newEvents);
    }

    public static List<Event> getEventsUniversityCached(Context context, Openstud os) {
        setupSharedPreferences(context);
        if (os == null) return null;
        String oldObj;
        JsonAdapter<List<Event>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, Event.class));
        synchronized (InfoManager.class) {
            if (theatre_events != null) {
                return new LinkedList<>(theatre_events);
            }
            oldObj = pref.getString("eventsUniversity", "null");
        }
        List<Event> ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
            synchronized (InfoManager.class) {
                theatre_events = ret;
            }
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static LocalDateTime getLastExamsWidgetUpdateTime(Context context) {
        setupSharedPreferences(context);
        String oldObj;
        JsonAdapter<LocalDateTime> jsonAdapter = moshi.adapter(LocalDateTime.class);
        synchronized (InfoManager.class) {
            oldObj = pref.getString("lastUpdateWidget", "null");
        }
        LocalDateTime ret = null;
        try {
            ret = jsonAdapter.fromJson(oldObj);
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void setLastExamsWidgetUpdateTime(Context context, LocalDateTime time) {
        setupSharedPreferences(context);
        if (time == null) return;
        JsonAdapter<LocalDateTime> jsonAdapter = moshi.adapter(LocalDateTime.class);
        String json = jsonAdapter.toJson(time);
        synchronized (InfoManager.class) {
            pref.edit().putString("lastUpdateWidget", json).apply();
        }
    }

    public static boolean hasLogin(Context context) {
        setupSharedPreferences(context);
        return getStudentId(context) != null && getPassword(context) != null;
    }

    public static synchronized void clearSharedPreferences(Context context) {
        setupSharedPreferences(context);
        pref.edit().clear().commit();
        os = null;
        student = null;
        paidTaxes = null;
        unpaidTaxes = null;
        examsDone = null;
        examsDoable = null;
        reservations = null;
        news = null;
        fakeExams = null;
        events = null;
        filter = null;
        theatre_events = null;
        card = null;
        clearCookies();
    }

    public static synchronized String getStudentId(Context context) {
        setupSharedPreferences(context);
        String id = null;
        try {
            id = pref.getString("studentId", null);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return id;
    }

    private static synchronized String getPassword(Context context) {
        setupSharedPreferences(context);
        return pref.getString("password", null);
    }

    private static synchronized void setNamePassword(Context context, String id, String password) {
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("studentId", id);
        editor.putString("password", password);
        editor.apply();
    }

    private static synchronized void setSaveFlag(Context context, boolean saveFlag) {
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("remember", saveFlag);
        editor.apply();
    }

    public static synchronized void setSortType(Context context, int type) {
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("sort", type);
        editor.apply();
    }

    public static synchronized int getSortType(Context context) {
        setupSharedPreferences(context);
        return pref.getInt("sort", 0);
    }


    public static synchronized void setReservationUpdateFlag(Context context, boolean updateFlag) {
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("updateReservations", updateFlag);
        editor.apply();
    }

    public static synchronized boolean getReservationUpdateFlag(Context context) {
        setupSharedPreferences(context);
        return pref.getBoolean("updateReservations", false);
    }

    public static synchronized boolean getSaveFlag(Context context) {
        setupSharedPreferences(context);
        return pref.getBoolean("remember", false);
    }


    public static synchronized boolean filterContains(Context context, String name) {
        List<String> tmp_filter = getExceptionFilter(context);
        return tmp_filter.contains(name);
    }

    public static synchronized void addExceptionToFilter(Context context, String name) {
        List<String> tmp_filter = getExceptionFilter(context);
        if (!tmp_filter.contains(name)) tmp_filter.add(name);
        updateFilter(tmp_filter);
    }

    public static synchronized void removeExceptionFromFilter(Context context, String name) {
        List<String> tmp_filter = getExceptionFilter(context);
        tmp_filter.remove(name);
        updateFilter(new LinkedList<>(tmp_filter));
    }

    public static synchronized void removeOldEntriesFilter(Context context, List<String> names) {
        List<String> tmp_filter = getExceptionFilter(context);
        List<String> to_remove = new LinkedList<>();
        for (String exception : tmp_filter) {
            if (!names.contains(exception)) to_remove.add(exception);
        }
        tmp_filter.removeAll(to_remove);
        updateFilter(tmp_filter);
    }

    private static void updateFilter(List<String> update_filter) {
        JsonAdapter<List<String>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, String.class));
        SharedPreferences.Editor editor = pref.edit();
        String toJson = jsonAdapter.toJson(update_filter);
        editor.putString("filter_calendar", toJson);
        editor.apply();
        if (filter == null) filter = new LinkedList<>();
        filter.clear();
        filter.addAll(update_filter);
    }

    private static List<String> getExceptionFilter(Context context) {
        setupSharedPreferences(context);
        if (filter != null) return new LinkedList<>(filter);
        synchronized (InfoManager.class) {
            JsonAdapter<List<String>> jsonAdapter = moshi.adapter(Types.newParameterizedType(List.class, String.class));
            String json = pref.getString("filter_calendar", null);
            if (json == null) {
                filter = new LinkedList<>();
                SharedPreferences.Editor editor = pref.edit();
                String toJson = jsonAdapter.toJson(filter);
                editor.putString("filter_calendar", toJson);
                editor.apply();
            } else {
                try {
                    filter = jsonAdapter.fromJson(json);
                } catch (JsonDataException | IOException e) {
                    e.printStackTrace();
                }
            }
            if (filter == null) filter = new LinkedList<>();
            return new LinkedList<>(filter);
        }
    }

    private static void removeDuplicatesFakeExams(Context context, Openstud os) {
        List<ExamDone> fake = InfoManager._getFakeExams(context, os, false);
        List<ExamDone> done = InfoManager.getExamsDoneCached(context, os);
        List<ExamDone> remove = new LinkedList<>();
        if (fake != null && done != null) {
            for (ExamDone ex : fake) {
                for (ExamDone ex2 : done) {
                    if (ex.getDescription().toLowerCase().trim().equals(ex2.getDescription().toLowerCase().trim())) {
                        remove.add(ex);
                        break;
                    }
                }
            }
            fake.removeAll(remove);
            InfoManager.saveFakeExams(context, fake);
        }
    }
}
