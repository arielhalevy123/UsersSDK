package com.example.barberapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import io.github.arielhalevy123.userssdk.UsersSdk;

public class BarberAppMainActivity extends AppCompatActivity {

    private static final String DEFAULT_BASE_URL = "http://192.168.1.122:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_main);

        UsersSdk.init(this, DEFAULT_BASE_URL);

        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}
