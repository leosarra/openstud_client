package com.lithium.leona.openstud.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import com.lithium.leona.openstud.helpers.WidgetHelper;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Event;
import lithium.openstud.driver.core.models.Student;
import lithium.openstud.driver.exceptions.OpenstudConnectionException;
import lithium.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import lithium.openstud.driver.exceptions.OpenstudInvalidResponseException;


public class ExamsWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        boolean includeDoable = ExamsWidgetConfigureActivity.getAvaialableExamSwitchStatus(context, appWidgetId);
        boolean showCountdown = ExamsWidgetConfigureActivity.getCountdownSwitchStatus(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.exams_events_widget);

        Openstud os = InfoManager.getOpenStud(context);
        if (os != null) {
            if (hasAtLeastOneEvent(os,context,includeDoable)) {
                views.setViewVisibility(R.id.content_layout, View.VISIBLE);
                views.setViewVisibility(R.id.no_results_layout, View.GONE);
            }
            else {
                views.setViewVisibility(R.id.content_layout, View.GONE);
                views.setViewVisibility(R.id.no_results_layout, View.VISIBLE);
            }
            views.setViewVisibility(R.id.empty_layout, View.GONE);
            updateView(context, views, appWidgetId, includeDoable, showCountdown);
        }
        else {
            views.setViewVisibility(R.id.content_layout, View.GONE);
            views.setViewVisibility(R.id.empty_layout, View.VISIBLE);
            views.setViewVisibility(R.id.no_results_layout, View.GONE);
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static boolean hasAtLeastOneEvent(Openstud os, Context context, boolean includeDoable){
        List<Event> newEvents = InfoManager.getEventsCached(context, os);
        if (newEvents == null) return true;
        System.out.println("filter "+WidgetHelper.filterValidExamsEvents(newEvents, includeDoable));
        return !WidgetHelper.filterValidExamsEvents(newEvents, includeDoable).isEmpty();
    }

    private static void updateView(Context context, RemoteViews views, int appWidgetId, boolean includeDoable, boolean showCountdown) {
        Intent intent = new Intent(context,  ExamsService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("includeDoable", includeDoable);
        intent.putExtra("showCountdown", showCountdown);
        views.setRemoteAdapter(R.id.list_view, intent);
    }

    @Override
    public synchronized void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Handler mHandler = new Handler();
        new Thread(() -> {
            Openstud os = InfoManager.getOpenStud(context);
            if (os != null) {
                Student student = InfoManager.getInfoStudentCached(context,os);
                if (student!=null) {
                    try {
                        InfoManager.getEvents(context, os, student);
                    } catch (OpenstudConnectionException | OpenstudInvalidResponseException e) {
                        e.printStackTrace();
                    } catch (OpenstudInvalidCredentialsException e) {
                        InfoManager.clearSharedPreferences(context);
                        e.printStackTrace();
                    }
                }
            }
            mHandler.post(() -> {
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            });
        }).start();
    }


    private static void scheduleNextUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ExamsWidget.class);
        intent.setAction("ACTION_SCHEDULED_UPDATE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        // Get a calendar instance for midnight tomorrow.
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        // Schedule one second after midnight, to be sure we are in the right day next time this
        // method is called.  Otherwise, we risk calling onUpdate multiple times within a few
        // milliseconds
        midnight.set(Calendar.SECOND, 1);
        midnight.set(Calendar.MILLISECOND, 0);
        midnight.add(Calendar.DAY_OF_YEAR, 1);
        alarmManager.set(AlarmManager.RTC, midnight.getTimeInMillis(), pendingIntent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            ExamsWidgetConfigureActivity.deletePref(context, appWidgetId);
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

    public void onUpdateCustom(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean cached) {
        // There may be multiple widgets active, so update all of them
        if (!cached) {
            onUpdate(context,appWidgetManager, appWidgetIds);
            return;
        }
        if (appWidgetIds.length>0) scheduleNextUpdate(context);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), ExamsWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            if (Objects.equals(intent.getAction(), "MANUAL_UPDATE"))
                onUpdateCustom(context, appWidgetManager, appWidgetIds, extras.getBoolean("cached", true));
            if (Objects.equals(intent.getAction(), "ACTION_SCHEDULED_UPDATE"))
                onUpdateCustom(context, appWidgetManager, appWidgetIds, false);
        }
    }
}

