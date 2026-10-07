package com.example.barberapp.customer;

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

import java.util.List;

public class CustomerProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        TextView tvName = findViewById(R.id.tv_name);
        TextView tvEmail = findViewById(R.id.tv_email);
        TextView tvBarberName = findViewById(R.id.tv_barber_name);
        Button btnLogout = findViewById(R.id.btn_logout);

        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO user) {
                if (user != null) {
                    tvName.setText(user.getName() != null ? user.getName() : "");
                    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                    Long adminId = user.getAdminId();
                    if (adminId != null) {
                        UsersSdk.get().listAdmins(new UsersSdk.Callback<List<UserDTO>>() {
                            @Override
                            public void onSuccess(List<UserDTO> admins) {
                                String name = "—";
                                if (admins != null) {
                                    for (UserDTO a : admins) {
                                        if (a.getId() == adminId) {
                                            name = a.getName() != null ? a.getName() : ("ID " + adminId);
                                            break;
                                        }
                                    }
                                }
                                final String barberName = name;
                                runOnUiThread(() -> tvBarberName.setText(barberName));
                            }

                            @Override
                            public void onError(Throwable error) {
                                runOnUiThread(() -> tvBarberName.setText("ID " + adminId));
                            }
                        });
                    } else {
                        tvBarberName.setText("—");
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(CustomerProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            UsersSdk.get().logout();
            startActivity(new Intent(this, BarberAppMainActivity.class));
            finishAffinity();
        });
    }
}
