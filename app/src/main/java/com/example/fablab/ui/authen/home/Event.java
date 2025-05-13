package com.example.fablab.ui.authen.home;

import java.io.Serializable;

public class Event implements Serializable {
    private String eventId;
    private String title;
    private String description;
    private String eventDate;
    private String startTime;
    private String endTime;
    private String numberOfPeople;
    private String status;
    private String userId;

    // Nepieciešams noklusējuma konstruktors DataSnapshot.getValue(Event.class) izsaukumiem.
    public Event() {
    }

    // Konstruktors ar visiem laukiem
    public Event(String eventId, String title, String description, String eventDate, String startTime, String endTime, String numberOfPeople, String status, String userId) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfPeople = numberOfPeople;
        this.status = status;
        this.userId = userId;
    }

    // getteri un setteri
    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getNumberOfPeople() {
        return numberOfPeople;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }
}
