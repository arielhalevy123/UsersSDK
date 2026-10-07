package com.example.barberapp.barber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barberapp.BarberAppMainActivity;
import com.example.barberapp.R;
import io.github.arielhalevy123.userssdk.UsersSdk;
import io.github.arielhalevy123.userssdk.model.UserDTO;

public class BarberProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_profile);

        TextView tvName = findViewById(R.id.tv_name);
        TextView tvEmail = findViewById(R.id.tv_email);
        Button btnLogout = findViewById(R.id.btn_logout);

        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO user) {
                if (user != null) {
                    tvName.setText(user.getName() != null ? user.getName() : "");
                    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                }
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(BarberProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            UsersSdk.get().logout();
            startActivity(new Intent(this, BarberAppMainActivity.class));
            finishAffinity();
        });
    }
}
