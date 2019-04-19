package com.lithium.leona.openstud.helpers;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.LinkedList;
import java.util.List;

import lithium.openstud.driver.core.models.Event;
import lithium.openstud.driver.core.models.EventType;

public class WidgetHelper {


    private static boolean isOldEvent(Event event){
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDate zonedDate = now.atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.of("+1")).toLocalDate();
        return zonedDate.isBefore(event.getEventDate());
    }

    public static long getRemainingDays(Event event) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDate zonedDate = now.atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.of("+1")).toLocalDate();
        return ChronoUnit.DAYS.between(zonedDate, event.getEventDate());
    }

    public static List<Event> filterValidExamsEvents(List<Event> events, boolean includeDoable) {
        List<Event> ignored = new LinkedList<>();
        for (Event event: events) {
            if (getRemainingDays(event)>95) ignored.add(event);
            else if (event.getEventType() != EventType.RESERVED && event.getEventType() != EventType.DOABLE) ignored.add(event);
            else if (!includeDoable && event.getEventType() == EventType.DOABLE) ignored.add(event);
            else if (!WidgetHelper.isOldEvent(event)) ignored.add(event);
        }
        List<Event> newEvents = new LinkedList<>(events);
        newEvents.removeAll(ignored);
        return newEvents;
    }

}
