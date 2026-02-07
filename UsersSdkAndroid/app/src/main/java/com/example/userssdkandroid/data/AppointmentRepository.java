package com.example.userssdkandroid.data;

import com.example.userssdkandroid.models.Appointment;
import java.util.ArrayList;
import java.util.List;

public class AppointmentRepository {
    private static final List<Appointment> appointments = new ArrayList<>();

    static {
        appointments.add(new Appointment("1", "Meeting", "09:00"));
        appointments.add(new Appointment("2", "Meeting", "10:00"));
        appointments.add(new Appointment("3", "Meeting", "11:00"));
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void addAppointment(String id, String title, String time) {
        appointments.add(new Appointment(id, title, time));
    }
}