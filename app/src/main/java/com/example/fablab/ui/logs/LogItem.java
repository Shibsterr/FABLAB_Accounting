package com.example.fablab.ui.logs;

public class LogItem {
    private String dateTime;
    private String user;
    private String email;
    private String title;
    private String summary;

    public LogItem(String dateTime, String user, String email, String title, String summary) {
        this.dateTime = dateTime;
        this.user = user;
        this.email = email;
        this.title = title;
        this.summary = summary;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }
}
