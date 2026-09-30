package com.example.barberapp.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barberapp.R;

public class CustomerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        findViewById(R.id.btn_book).setOnClickListener(v ->
                startActivity(new Intent(this, BookAppointmentActivity.class)));

        findViewById(R.id.btn_my_appointments).setOnClickListener(v ->
                startActivity(new Intent(this, MyAppointmentsActivity.class)));

        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(this, CustomerProfileActivity.class)));
    }
}
