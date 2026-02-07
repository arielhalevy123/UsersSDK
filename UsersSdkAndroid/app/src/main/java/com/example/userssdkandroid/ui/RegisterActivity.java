package com.example.userssdkandroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.AuthResponse;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;
import com.example.userssdkandroid.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput;
    private Spinner roleSpinner;
    private Button registerBtn;

    // חדש:
    private LinearLayout rowAdmin;
    private EditText adminIdInput;
    private Button btnPickAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput     = findViewById(R.id.name_input);
        emailInput    = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        roleSpinner   = findViewById(R.id.role_spinner);
        registerBtn   = findViewById(R.id.register_button);

        // חדש:
        rowAdmin      = findViewById(R.id.row_admin);
        adminIdInput  = findViewById(R.id.admin_id_input);
        btnPickAdmin  = findViewById(R.id.btn_pick_admin);

        // Spinner setup (שים לב: ADMIN/USER בלבד)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.roles_array, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String role = parent.getItemAtPosition(position).toString().toUpperCase(Locale.ROOT);
                rowAdmin.setVisibility("USER".equals(role) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // כפתור בחירת אדמין מהרשימה
        btnPickAdmin.setOnClickListener(v -> pickAdminFromServer());

        registerBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString().toUpperCase(Locale.ROOT);

            Long adminId = null;
            if ("USER".equals(role)) {
                String s = adminIdInput.getText().toString().trim();
                if (s.isEmpty()) {
                    Toast.makeText(this, "Please choose Admin (ID)", Toast.LENGTH_SHORT).show();
                    return;
                }
                try { adminId = Long.parseLong(s); }
                catch (NumberFormatException e) {
                    Toast.makeText(this, "Admin ID must be a number", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            List<CustomFieldDTO> customFields = new ArrayList<>(); // כרגע ריק

            registerBtn.setEnabled(false);
            UsersSdk.get().register(name, email, password, role, adminId, customFields,
                    new UsersSdk.Callback<AuthResponse>() {
                        @Override public void onSuccess(AuthResponse result) {
                            runOnUiThread(() -> {
                                registerBtn.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            });
                        }
                        @Override public void onError(Throwable error) {
                            runOnUiThread(() -> {
                                registerBtn.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
        });
    }

    private void pickAdminFromServer() {
        UsersSdk.get().listAdmins(new UsersSdk.Callback<List<UserDTO>>() {
            @Override public void onSuccess(List<UserDTO> admins) {
                runOnUiThread(() -> {
                    if (admins == null || admins.isEmpty()) {
                        Toast.makeText(RegisterActivity.this, "No admins found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] labels = new String[admins.size()];
                    for (int i = 0; i < admins.size(); i++) {
                        UserDTO a = admins.get(i);
                        labels[i] = String.format(Locale.getDefault(), "%s (%s)  [id=%d]",
                                a.getName(), a.getEmail(), a.getId());
                    }
                    new AlertDialog.Builder(RegisterActivity.this)
                            .setTitle("Choose Admin")
                            .setItems(labels, (d, which) -> {
                                UserDTO chosen = admins.get(which);
                                adminIdInput.setText(String.valueOf(chosen.getId()));
                            })
                            .show();
                });
            }
            @Override public void onError(Throwable error) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "Failed to load admins", Toast.LENGTH_SHORT).show());
            }
        });
    }
}