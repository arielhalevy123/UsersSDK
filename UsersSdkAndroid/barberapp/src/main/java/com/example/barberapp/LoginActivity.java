package com.example.barberapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barberapp.barber.BarberHomeActivity;
import com.example.barberapp.customer.CustomerHomeActivity;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.AuthResponse;
import com.example.userssdk.model.UserDTO;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            UsersSdk.get().logout();
            UsersSdk.get().login(email, password, new UsersSdk.Callback<AuthResponse>() {
                @Override
                public void onSuccess(AuthResponse result) {
                    UserDTO user = result.getUser();
                    if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                        startActivity(new Intent(LoginActivity.this, BarberHomeActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, CustomerHomeActivity.class));
                    }
                    finish();
                }

                @Override
                public void onError(Throwable error) {
                    Toast.makeText(LoginActivity.this, "Login failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
