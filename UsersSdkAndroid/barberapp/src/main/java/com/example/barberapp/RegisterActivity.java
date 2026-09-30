package com.example.barberapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barberapp.barber.BarberHomeActivity;
import com.example.barberapp.customer.CustomerHomeActivity;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.AuthResponse;
import com.example.userssdk.model.UserDTO;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput;
    private CheckBox checkIAmBarber;
    private View labelChooseBarber;
    private Spinner barberSpinner;
    private Button registerBtn;

    private List<UserDTO> barbers = new ArrayList<>();
    private UserDTO selectedBarber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        checkIAmBarber = findViewById(R.id.check_iam_barber);
        labelChooseBarber = findViewById(R.id.label_choose_barber);
        barberSpinner = findViewById(R.id.barber_spinner);
        registerBtn = findViewById(R.id.register_button);

        checkIAmBarber.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int visibility = isChecked ? View.GONE : View.VISIBLE;
            labelChooseBarber.setVisibility(visibility);
            barberSpinner.setVisibility(visibility);
        });

        loadBarbers();

        registerBtn.setOnClickListener(v -> doRegister());
    }

    private void loadBarbers() {
        UsersSdk.get().listAdmins(new UsersSdk.Callback<List<UserDTO>>() {
            @Override
            public void onSuccess(List<UserDTO> result) {
                barbers = result != null ? result : new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (UserDTO u : barbers) {
                    names.add(u.getName() != null ? u.getName() : ("ID " + u.getId()));
                }
                if (barbers.isEmpty()) {
                    names.add("No barbers available");
                }
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(RegisterActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    barberSpinner.setAdapter(adapter);
                    barberSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < barbers.size()) {
                                selectedBarber = barbers.get(position);
                            } else {
                                selectedBarber = null;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedBarber = null;
                        }
                    });
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "Failed to load barbers: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void doRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill name, email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String role;
        Long adminId;

        if (checkIAmBarber.isChecked()) {
            role = "ADMIN";
            adminId = null;
        } else {
            if (selectedBarber == null && (barbers.isEmpty() || barberSpinner.getSelectedItemPosition() >= barbers.size())) {
                Toast.makeText(this, "Please choose your barber", Toast.LENGTH_SHORT).show();
                return;
            }
            role = "USER";
            adminId = selectedBarber != null ? selectedBarber.getId() : null;
        }

        UsersSdk.get().register(name, email, password, role, adminId, null, new UsersSdk.Callback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse result) {
                UserDTO user = result.getUser();
                if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    startActivity(new android.content.Intent(RegisterActivity.this, BarberHomeActivity.class));
                } else {
                    startActivity(new android.content.Intent(RegisterActivity.this, CustomerHomeActivity.class));
                }
                finish();
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(RegisterActivity.this, "Register failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
