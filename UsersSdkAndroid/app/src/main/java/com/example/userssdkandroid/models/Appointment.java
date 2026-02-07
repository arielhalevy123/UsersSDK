package com.example.userssdkandroid.models;

public class Appointment {
    private String id;
    private String title;
    private String dateTime;
    private boolean isReserved;
    private String reservedByEmail;

    public Appointment(String id, String title, String dateTime) {
        this.id = id;
        this.title = title;
        this.dateTime = dateTime;
        this.isReserved = false;
        this.reservedByEmail = null;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDateTime() {
        return dateTime;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public String getReservedByEmail() {
        return reservedByEmail;
    }

    public void reserve(String email) {
        this.isReserved = true;
        this.reservedByEmail = email;
    }

    public void cancelReservation() {
        this.isReserved = false;
        this.reservedByEmail = null;
    }
}