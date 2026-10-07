// sdk: io.github.arielhalevy123.userssdk.SdkConfig
package io.github.arielhalevy123.userssdk;

import java.util.Locale;

public class SdkConfig {
    private int appointmentMinutes = 30;
    private String appointmentFieldName = "Appointment";
    private Locale locale = Locale.US;
    private boolean allowBackToBack = true;
    private boolean httpLogging = false;

    /**
     * Logs full HTTP requests and responses to Logcat. Off by default because bodies contain
     * passwords and tokens; enable only in debug builds. Must be set before {@code UsersSdk.init}.
     */
    public boolean isHttpLogging() { return httpLogging; }
    public SdkConfig setHttpLogging(boolean v) { this.httpLogging = v; return this; }

    public int getAppointmentMinutes() { return appointmentMinutes; }
    public SdkConfig setAppointmentMinutes(int m) { this.appointmentMinutes = m; return this; }

    public String getAppointmentFieldName() { return appointmentFieldName; }
    public SdkConfig setAppointmentFieldName(String n) { this.appointmentFieldName = n; return this; }

    public Locale getLocale() { return locale; }
    public SdkConfig setLocale(Locale l) { this.locale = l; return this; }

    public boolean isAllowBackToBack() { return allowBackToBack; }
    public SdkConfig setAllowBackToBack(boolean v) { this.allowBackToBack = v; return this; }
}