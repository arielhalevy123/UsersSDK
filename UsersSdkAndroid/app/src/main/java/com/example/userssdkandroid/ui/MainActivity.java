package com.example.userssdkandroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import io.github.arielhalevy123.userssdk.UsersSdk;
import com.example.userssdkandroid.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UsersSdk.init(this, com.example.userssdkandroid.BuildConfig.USERS_SDK_BASE_URL);
        Button registerBtn = findViewById(R.id.register_button);
        Button loginBtn = findViewById(R.id.login_button);

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class))
        );

        loginBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class))
        );
    }
}