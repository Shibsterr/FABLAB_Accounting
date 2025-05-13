package com.example.fablab.ui.authen.home;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

public class EventDecorator implements DayViewDecorator {
    private final CalendarDay date;
    private final String status;

    public EventDecorator(CalendarDay date, String status) {
        this.date = date;
        this.status = status;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(date);
    }

    @Override
    public void decorate(DayViewFacade view) {
        int color;
        switch (status) {
            case "Accepted":
                color = 0xFF00FF00; // Zaļš
                break;
            case "Declined":
                color = 0xFFFF0000; // Sarkans
                break;
            case "Finished":
                color = 0xFF6633FF; //zils
                break;
            default:
                color = 0xFFFFA500; // Oranžš
                break;
        }
        view.addSpan(new DotSpan(10, color));
    }
}