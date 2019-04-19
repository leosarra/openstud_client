package com.lithium.leona.openstud.widgets;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.content.ContextCompat;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.data.InfoManager;
import com.lithium.leona.openstud.helpers.WidgetHelper;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.LinkedList;
import java.util.List;

import lithium.openstud.driver.core.Openstud;
import lithium.openstud.driver.core.models.Event;

public class ExamsFactory implements RemoteViewsService.RemoteViewsFactory {

    private List<Event> events = new LinkedList<>();
    private boolean includeDoable;
    private boolean showCountdown;
    private Context context;

    ExamsFactory(Context applicationContext, Intent intent) {
        context = applicationContext;
        includeDoable = intent.getBooleanExtra("includeDoable", true);
        showCountdown = intent.getBooleanExtra("showCountdown", true);
        populateListItems();
    }

    private void populateListItems() {
        events.clear();
        Openstud os = InfoManager.getOpenStud(context);
        if (os == null) return;
        List<Event> newEvents = InfoManager.getEventsCached(context, os);
        if (newEvents == null) return;
        events.addAll(WidgetHelper.mergeExamEvents(WidgetHelper.filterValidExamsEvents(newEvents, includeDoable)));
    }


    @Override
    public RemoteViews getViewAt(int position) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.item_exam_widget);
        Event event = events.get(position);
        remoteViews.setTextViewText(R.id.title, event.getTitle());
        remoteViews.setTextViewText(R.id.countdown, "-" + WidgetHelper.getRemainingDays(event));
        remoteViews.setTextViewText(R.id.teacherName, event.getTeacher());
        String finalString = context.getResources().getString(R.string.cfu_date_widget, String.valueOf(event.getReservation().getCfu()), event.getEventDate().format(formatter));
        SpannableString sb = new SpannableString(finalString);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.redSapienzaLight)), 0, finalString.indexOf("•"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray)), finalString.indexOf("•") + 1, finalString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        remoteViews.setTextViewText(R.id.cfu_date, sb);
        if (!showCountdown) remoteViews.setViewVisibility(R.id.countdown, View.GONE);
        else remoteViews.setViewVisibility(R.id.countdown, View.VISIBLE);
        return remoteViews;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public boolean hasStableIds() {
        return true;
    }


    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return events.size();
    }

}
