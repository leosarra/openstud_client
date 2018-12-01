package com.lithium.leona.openstud;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.icu.text.IDNA;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.RemoteViews;

import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lithium.openstud.driver.core.ExamDone;
import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.OpenstudHelper;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

/**
 * Implementation of App Widget functionality.
 */
public class GradesWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, boolean cached) {
        Handler mHandler = new Handler();
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.grades_widget);
        Openstud os = InfoManager.getOpenStud(context);
        if (os!=null) {
            views.setViewVisibility(R.id.content_layout, View.VISIBLE);
            views.setViewVisibility(R.id.empty_layout, View.GONE);
            updateStats(context, mHandler, appWidgetManager, appWidgetId, views, os, cached);
        } else {
            views.setViewVisibility(R.id.content_layout, View.GONE);
            views.setViewVisibility(R.id.empty_layout, View.VISIBLE);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        // Instruct the widget manager to update the widget
    }

    private static void updateStats(Context context, Handler handler, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views, Openstud os, boolean cached){
        new Thread(() -> {
            List<ExamDone> ret;
            System.out.println("HO VALORE"+cached);
            try {
                if(cached) ret = InfoManager.getExamsDoneCached(context,os);
                else ret = InfoManager.getExamsDone(context, os);
                updateView(context,handler,appWidgetManager,appWidgetId,views,ret);
            } catch (OpenstudConnectionException | OpenstudInvalidResponseException e) {
                e.printStackTrace();
            } catch (OpenstudInvalidCredentialsException e) {
                InfoManager.clearSharedPreferences(context);
                e.printStackTrace();
            } finally {
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }).start();
    }

    private static void updateView(Context context, Handler handler, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views, List<ExamDone> exams) {
        if (ClientHelper.hasPassedExams(exams)) {
            System.out.println("ciao");
            NumberFormat numFormat = NumberFormat.getInstance();
            numFormat.setMaximumFractionDigits(2);
            numFormat.setMinimumFractionDigits(1);
            handler.post(() -> {
                views.setTextViewText(R.id.totalCFU, String.valueOf(OpenstudHelper.getSumCFU(exams)));
                views.setTextViewText(R.id.weightedValue, String.valueOf(numFormat.format(OpenstudHelper.computeWeightedAverage(exams, PreferenceManager.getLaudeValue(context)))));
                views.setTextViewText(R.id.baseFinalGrade, String.valueOf(OpenstudHelper.computeBaseGraduation(exams, PreferenceManager.getLaudeValue(context))));
                appWidgetManager.updateAppWidget(appWidgetId, views);
            });

        } else {
            handler.post(() -> {
                views.setTextViewText(R.id.totalCFU, "--");
                views.setTextViewText(R.id.weightedValue, "--");
                views.setTextViewText(R.id.baseFinalGrade, "--");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            });
        }
    }


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    public void onUpdateCustom(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean cached) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, cached);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
        Bundle extras = intent.getExtras();
        if(extras!=null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), GradesWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            if(Objects.equals(intent.getAction(), "CUSTOM")) onUpdateCustom(context, appWidgetManager, appWidgetIds,extras.getBoolean("cached", true));
        }
    }
}

