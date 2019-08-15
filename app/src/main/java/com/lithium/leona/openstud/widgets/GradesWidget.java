package com.lithium.leona.openstud.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.RemoteViews;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.data.PreferenceManager;
import com.lithium.leona.openstud.helpers.ClientHelper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.OpenstudHelper;
import lithium.openstud.driver.core.models.ExamDone;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;

/**
 * Implementation of App Widget functionality.
 */
public class GradesWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.grades_widget);
        Openstud os = InfoManager.getOpenStud(context);
        if (os != null) {
            views.setViewVisibility(R.id.content_layout, View.VISIBLE);
            views.setViewVisibility(R.id.empty_layout, View.GONE);
            updateStats(context, appWidgetManager, appWidgetId, views, os);
        } else {
            views.setViewVisibility(R.id.content_layout, View.GONE);
            views.setViewVisibility(R.id.empty_layout, View.VISIBLE);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        // Instruct the widget manager to update the widget
    }

    private static void updateStats(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views, Openstud os) {
        List<ExamDone> exams = InfoManager.getExamsDoneCached(context, os);
        List<ExamDone> fake = InfoManager.getFakeExams(context, os);
        if (exams != null && fake != null) exams.addAll(fake);
        updateView(context, appWidgetManager, appWidgetId, views, exams);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void updateView(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views, List<ExamDone> exams) {
        if (ClientHelper.hasPassedExams(exams)) {
            NumberFormat numFormat = NumberFormat.getInstance();
            numFormat.setMaximumFractionDigits(2);
            numFormat.setMinimumFractionDigits(1);
            views.setTextViewText(R.id.totalCFU, String.valueOf(OpenstudHelper.getSumCFU(exams)));
            views.setTextViewText(R.id.weightedValue, String.valueOf(numFormat.format(OpenstudHelper.computeWeightedAverage(exams, PreferenceManager.getLaudeValue(context)))));
            views.setTextViewText(R.id.baseFinalGrade, String.valueOf(OpenstudHelper.computeBaseGraduation(exams, PreferenceManager.getLaudeValue(context), PreferenceManager.isMinMaxExamIgnoredInBaseGraduation(context))));
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } else {
            views.setTextViewText(R.id.totalCFU, "--");
            views.setTextViewText(R.id.weightedValue, "--");
            views.setTextViewText(R.id.baseFinalGrade, "--");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Handler mHandler = new Handler();
        new Thread(() -> {
            Openstud os = InfoManager.getOpenStud(context);
            if (os != null) {
                try {
                    InfoManager.getExamsDone(context, os);
                } catch (OpenstudConnectionException | OpenstudInvalidResponseException e) {
                    e.printStackTrace();
                } catch (OpenstudInvalidCredentialsException e) {
                    InfoManager.clearSharedPreferences(context);
                    e.printStackTrace();
                }
            }
            mHandler.post(() -> {
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            });
        }).start();

    }

    public void onUpdateCustom(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
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
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), GradesWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            if (Objects.equals(intent.getAction(), "MANUAL_UPDATE"))
                onUpdateCustom(context, appWidgetManager, appWidgetIds);
        }
    }
}

