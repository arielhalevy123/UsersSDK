package com.example.barberapp.barber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barberapp.R;

public class BarberHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_home);

        findViewById(R.id.btn_my_customers).setOnClickListener(v ->
                startActivity(new Intent(this, BarberCustomersActivity.class)));

        findViewById(R.id.btn_calendar).setOnClickListener(v ->
                startActivity(new Intent(this, BarberCalendarActivity.class)));

        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(this, BarberProfileActivity.class)));
    }
}
