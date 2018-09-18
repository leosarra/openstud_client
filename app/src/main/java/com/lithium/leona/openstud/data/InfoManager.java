package com.lithium.leona.openstud.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import lithium.openstud.driver.core.ExamPassed;
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
    private static List<ExamPassed> examsDone;
    private static void setupSharedPreferences(Context context){
        if (pref!=null) return;
        pref = context.getSharedPreferences("OpenStudPref", 0); // 0 - for private mode
    }

    public static Openstud getOpenStud(Context context){
        setupSharedPreferences(context);
        if (os!=null) return os;
        if (getStudentId(context)== -1 || getPassword(context)==null) return null;
        os = new OpenstudBuilder().setStudentID(getStudentId(context)).setPassword(getPassword(context)).setRetryCounter(2).build();
        return os;
    }


    public static Openstud getOpenStud(Context context, int studentId, String password, boolean save) {
        if (studentId == -1 || password== null || password.isEmpty()) return null;
        setupSharedPreferences(context);
        setNamePassword(context, studentId, password);
        setSaveFlag(context, save);
        os = new OpenstudBuilder().setStudentID(studentId).setPassword(password).build();
        return os;
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

    public static List<ExamPassed> getExamsDoneCached(Context context, Openstud os) {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        String oldObj;
        Gson gson = new Gson();
        synchronized (InfoManager.class) {
            if (examsDone!=null) return examsDone;
            oldObj =  pref.getString("examsDone", "null");
        }
        Type listType = new TypeToken<List<ExamPassed>>(){}.getType();
        return gson.fromJson(oldObj,listType);
    }

    public static List<ExamPassed> getExamsDone(Context context, Openstud os) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (os==null) return null;
        if (!hasLogin(context)) return null;
        Gson gson = new Gson();
        List<ExamPassed> newExamsDone = os.getExamsPassed();
        synchronized (InfoManager.class){
            examsDone = newExamsDone;
            SharedPreferences.Editor prefsEditor = pref.edit();
            Type listType = new TypeToken<List<Tax>>(){}.getType();
            String json = gson.toJson(paidTaxes);
            prefsEditor.putString("examsDone", json);
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
            String json = gson.toJson(paidTaxes);
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
            String json = gson.toJson(newUnpaidTaxes);
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
    }

    private static synchronized int getStudentId(Context context){
        return pref.getInt("studentId",-1);
    }

    private static synchronized String getPassword(Context context){
        return pref.getString("password",null);
    }

    private static synchronized void setNamePassword(Context context, int id, String password){
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("studentId", id);
        editor.putString("password", password);
        editor.apply();
    }

    private static synchronized void setSaveFlag(Context context, boolean saveFlag){
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("remember", saveFlag);
        editor.apply();
    }

    public static synchronized boolean getSaveFlag(Context context) {
        setupSharedPreferences(context);
        return pref.getBoolean("remember",false);
    }

}
