package com.lithium.leona.openstud.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.IDNA;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import lithium.openstud.driver.core.ExamDoable;
import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.ExamReservation;
import lithium.openstud.driver.core.Isee;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.OpenstudBuilder;
import lithium.openstud.driver.core.Student;
import lithium.openstud.driver.core.Tax;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

public class InfoManager {
    private static Openstud os;
    private static SharedPreferences pref;
    private static Student student;
    private static Isee isee;
    private static List<Tax> paidTaxes;
    private static List<Tax> unpaidTaxes;
    private static List<ExamDone> examsDone;
    private static List<ExamDoable> examsDoable;
    private static List<ExamReservation> reservations;
    private static void setupSharedPreferences(Context context){
        if (pref!=null) return;
        pref = context.getSharedPreferences("OpenStudPref", 0); // 0 - for private mode
    }

    public static Openstud getOpenStud(Context context){
        setupSharedPreferences(context);
        if (getStudentId(context)== -1 || getPassword(context)==null) return null;
        Gson gson = new Gson();
        String oldObj;
        synchronized (InfoManager.class) {
            if (os!=null) return os;
            os = new OpenstudBuilder().setStudentID(getStudentId(context)).setPassword(getPassword(context)).setRetryCounter(3).forceReadyState().setLogger(Logger.getLogger("OpenStud_client")).build();
            return os;
        }
    }


    public static Openstud getOpenStud(Context context, int studentId, String password) {
        if (studentId == -1 || password== null || password.isEmpty()) return null;
        return new OpenstudBuilder().setStudentID(studentId).setPassword(password).setRetryCounter(3).setLogger(Logger.getLogger("OpenStud_client")).build();
    }

    public static void saveOpenStud(Context context, Openstud openstud, int studentId, String password, boolean save){
        setupSharedPreferences(context);
        setNamePassword(context, studentId, password);
        setSaveFlag(context, save);
        synchronized (InfoManager.class) {
            os = openstud;
        }
    }

    public static Student getInfoStudentCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (student!=null) return student;
            oldObj =  pref.getString("student", "null");
        }
        return gson.fromJson(oldObj,Student.class);
    }


    public static Student getInfoStudent(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        Student newStudent = os.getInfoStudent();
        synchronized (InfoManager.class){
            student = newStudent;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = gson.toJson(student);
            prefsEditor.putString("student", json);
            prefsEditor.commit();
        }
        return student;
    }

    public static Isee getIseeCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (isee!=null) return isee;
            oldObj =  pref.getString("isee", "null");
        }
        return gson.fromJson(oldObj,Isee.class);
    }

    public static Isee getIsee(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        Isee newIsee = os.getCurrentIsee();
        synchronized (InfoManager.class){
            isee = newIsee;
            SharedPreferences.Editor prefsEditor = pref.edit();
            String json = gson.toJson(isee);
            prefsEditor.putString("isee", json);
            prefsEditor.commit();
        }
        return isee;
    }

    public static List<Tax> getPaidTaxesCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (paidTaxes!=null) return paidTaxes;
            oldObj =  pref.getString("paidTaxes", "null");
        }
        Type listType = new TypeToken<List<Tax>>(){}.getType();
        return gson.fromJson(oldObj,listType);
    }

    public static List<ExamDone> getExamsDoneCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (examsDone!=null) return examsDone;
            oldObj =  pref.getString("examsDone", "null");
        }
        Type listType = new TypeToken<List<ExamDone>>(){}.getType();
        return gson.fromJson(oldObj,listType);
    }

    public static List<ExamDone> getExamsDone(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        List<ExamDone> newExamsDone = os.getExamsDone();
        synchronized (InfoManager.class){
            examsDone = newExamsDone;
            SharedPreferences.Editor prefsEditor = pref.edit();
            Type listType = new TypeToken<List<ExamDone>>(){}.getType();
            String json = gson.toJson(examsDone,listType);
            prefsEditor.putString("examsDone", json);
            prefsEditor.commit();
        }
        return newExamsDone;
    }

    public static List<ExamDoable> getExamsDoableCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (examsDoable!=null) return examsDoable;
            oldObj =  pref.getString("examsDoable", "null");
        }
        Type listType = new TypeToken<List<ExamDoable>>(){}.getType();
        return gson.fromJson(oldObj,listType);
    }

    public static List<ExamDoable> getExamsDoable(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        List<ExamDoable> newExamsDoable = os.getExamsDoable();
        synchronized (InfoManager.class){
            examsDoable = newExamsDoable;
            SharedPreferences.Editor prefsEditor = pref.edit();
            Type listType = new TypeToken<List<ExamDoable>>(){}.getType();
            String json = gson.toJson(examsDoable,listType);
            prefsEditor.putString("examsDoable", json);
            prefsEditor.commit();
        }
        return newExamsDoable;
    }


    public static List<ExamReservation> getActiveReservationsCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (reservations!=null) return reservations;
            oldObj =  pref.getString("reservations", "null");
        }
        Type listType = new TypeToken<List<ExamReservation>>(){}.getType();
        return gson.fromJson(oldObj,listType);
    }

    public static List<ExamReservation> getActiveReservations(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        List<ExamReservation> newExamsDone = os.getActiveReservations();
        synchronized (InfoManager.class){
            reservations = newExamsDone;
            SharedPreferences.Editor prefsEditor = pref.edit();
            Type listType = new TypeToken<List<ExamReservation>>(){}.getType();
            String json = gson.toJson(reservations,listType);
            prefsEditor.putString("reservations", json);
            prefsEditor.commit();
        }
        return newExamsDone;
    }

    public static List<Tax> getPaidTaxes(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        List<Tax> newPaidTaxes = os.getPaidTaxes();
        synchronized (InfoManager.class){
            paidTaxes = newPaidTaxes;
            SharedPreferences.Editor prefsEditor = pref.edit();
            Type listType = new TypeToken<List<Tax>>(){}.getType();
            String json = gson.toJson(paidTaxes, listType);
            prefsEditor.putString("paidTaxes", json);
            prefsEditor.commit();
        }
        return newPaidTaxes;
    }

    public static List<Tax> getUnpaidTaxesCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (unpaidTaxes!=null) return unpaidTaxes;
            oldObj =  pref.getString("unpaidTaxes", "null");
        }
        Type listType = new TypeToken<List<Tax>>(){}.getType();
        return gson.fromJson(oldObj,listType);
    }

    public static List<Tax> getUnpaidTaxes(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        List<Tax> newUnpaidTaxes = os.getUnpaidTaxes();
        synchronized (InfoManager.class){
            unpaidTaxes = newUnpaidTaxes;
            SharedPreferences.Editor prefsEditor = pref.edit();
            Type listType = new TypeToken<List<Tax>>(){}.getType();
            String json = gson.toJson(newUnpaidTaxes, listType);
            prefsEditor.putString("unpaidTaxes", json);
            prefsEditor.commit();
        }
        return newUnpaidTaxes;
    }


    public static boolean hasLogin(Context context){
        setupSharedPreferences(context);
        if (getStudentId(context)!=-1 && getPassword(context)!=null)
            return true;
        return false;
    }

    public static synchronized void clearSharedPreferences(Context context){
        setupSharedPreferences(context);
        pref.edit().clear().commit();
        os = null;
        student= null;
        isee = null;
        paidTaxes = null;
        unpaidTaxes = null;
        examsDone = null;
        reservations = null;
    }

    private static synchronized int getStudentId(Context context){
        return pref.getInt("studentId",-1);
    }

    private static synchronized String getPassword(Context context){
        return pref.getString("password",null);
    }

    private static synchronized void setNamePassword(Context context, int id, String password){
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("studentId", id);
        editor.putString("password", password);
        editor.apply();
    }

    private static synchronized void setSaveFlag(Context context, boolean saveFlag){
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("remember", saveFlag);
        editor.apply();
    }

    public static synchronized void setSortType(Context context, int type){
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("sort", type);
        editor.apply();
    }

    public static synchronized int getSortType(Context context){
        setupSharedPreferences(context);
        return pref.getInt("sort",0);
    }


    public static synchronized void setReservationUpdateFlag(Context context, boolean updateFlag){
        setupSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("updateReservations", updateFlag);
        editor.apply();
    }

    public static synchronized boolean getReservationUpdateFlag(Context context) {
        setupSharedPreferences(context);
        return pref.getBoolean("updateReservations",false);
    }

    public static synchronized boolean getSaveFlag(Context context) {
        setupSharedPreferences(context);
        return pref.getBoolean("remember",false);
    }

}
