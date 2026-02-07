// sdk: com.example.userssdk.SdkConfig
package com.example.userssdk;

import java.util.Locale;

public class SdkConfig {
    private int appointmentMinutes = 30;
    private String appointmentFieldName = "Appointment";
    private Locale locale = Locale.US;
    private boolean allowBackToBack = true;

    public int getAppointmentMinutes() { return appointmentMinutes; }
    public SdkConfig setAppointmentMinutes(int m) { this.appointmentMinutes = m; return this; }

    public String getAppointmentFieldName() { return appointmentFieldName; }
    public SdkConfig setAppointmentFieldName(String n) { this.appointmentFieldName = n; return this; }

    public Locale getLocale() { return locale; }
    public SdkConfig setLocale(Locale l) { this.locale = l; return this; }

    public boolean isAllowBackToBack() { return allowBackToBack; }
    public SdkConfig setAllowBackToBack(boolean v) { this.allowBackToBack = v; return this; }
}