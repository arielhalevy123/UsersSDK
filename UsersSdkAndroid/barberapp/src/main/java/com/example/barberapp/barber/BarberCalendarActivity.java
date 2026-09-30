package com.example.barberapp.barber;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.barberapp.R;
import com.example.userssdk.ui.calendar.AdminCalendarFragment;

@RequiresApi(api = Build.VERSION_CODES.O)
public class BarberCalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_calendar);

        if (savedInstanceState == null) {
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.replace(R.id.calendar_container, new AdminCalendarFragment());
            tx.commit();
        }
    }
}
