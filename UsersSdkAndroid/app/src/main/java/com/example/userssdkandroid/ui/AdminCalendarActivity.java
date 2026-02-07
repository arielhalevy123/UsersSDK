package com.example.userssdkandroid.ui;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.userssdkandroid.R;
import com.example.userssdk.ui.calendar.UsersSdkCalendar;

public class AdminCalendarActivity extends AppCompatActivity {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_calendar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.calendarContainer, UsersSdkCalendar.newAdminCalendarFragment())
                    .commit();
        }
    }
}