package com.example.userssdk.ui.calendar;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

public final class UsersSdkCalendar {
    private UsersSdkCalendar() {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Fragment newUserCalendarFragment() {
        return new UserCalendarFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Fragment newAdminCalendarFragment() {
        return new AdminCalendarFragment();
    }
}