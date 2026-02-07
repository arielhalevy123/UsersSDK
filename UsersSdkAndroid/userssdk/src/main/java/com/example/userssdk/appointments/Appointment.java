package com.example.userssdk.appointments;

import java.time.LocalDateTime;

public final class Appointment {
    private final LocalDateTime start;
    private final int minutes;

    public Appointment(LocalDateTime start, int minutes) {
        this.start = start;
        this.minutes = minutes;
    }

    public LocalDateTime getStart() { return start; }
    public int getMinutes() { return minutes; }
    public LocalDateTime getEnd() { return start.plusMinutes(minutes); }
}