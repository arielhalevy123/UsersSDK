package com.example.barberapp.customer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barberapp.R;
import io.github.arielhalevy123.userssdk.UsersSdk;
import io.github.arielhalevy123.userssdk.model.UserDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.O)
public class BookAppointmentActivity extends AppCompatActivity {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    private List<UserDTO> barbers = new ArrayList<>();
    private UserDTO selectedBarber;
    private String selectedDateTimeStr;

    private Spinner barberSpinner;
    private TextView tvSelectedDatetime;
    private Button btnConfirmBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        barberSpinner = findViewById(R.id.barber_spinner);
        Spinner serviceSpinner = findViewById(R.id.service_spinner);
        Button btnPickDatetime = findViewById(R.id.btn_pick_datetime);
        tvSelectedDatetime = findViewById(R.id.tv_selected_datetime);
        btnConfirmBooking = findViewById(R.id.btn_confirm_booking);

        String[] services = {
                getString(R.string.service_haircut),
                getString(R.string.service_beard),
                getString(R.string.service_combo)
        };
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, services);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceSpinner.setAdapter(serviceAdapter);

        loadBarbers();

        btnPickDatetime.setOnClickListener(v -> showDateTimePicker());
        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(BookAppointmentActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    barberSpinner.setAdapter(adapter);
                    barberSpinner.setSelection(0);
                    if (!barbers.isEmpty()) selectedBarber = barbers.get(0);
                    barberSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                            if (position >= 0 && position < barbers.size()) selectedBarber = barbers.get(position);
                        }
                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> Toast.makeText(BookAppointmentActivity.this, "Failed to load barbers", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showDateTimePicker() {
        LocalDate today = LocalDate.now();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    TimePickerDialog tp = new TimePickerDialog(this,
                            (v, hourOfDay, minute) -> {
                                selectedDateTimeStr = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                                        year, month + 1, dayOfMonth, hourOfDay, minute);
                                tvSelectedDatetime.setText(selectedDateTimeStr);
                                btnConfirmBooking.setEnabled(true);
                            },
                            LocalTime.now().getHour(), LocalTime.now().getMinute(), true);
                    tp.show();
                },
                today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth());
        dp.show();
    }

    private void confirmBooking() {
        if (selectedDateTimeStr == null || selectedDateTimeStr.isEmpty()) {
            Toast.makeText(this, "Please pick date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO user) {
                UsersSdk.appointments().add(user, selectedDateTimeStr, new UsersSdk.Callback<UserDTO>() {
                    @Override
                    public void onSuccess(UserDTO updated) {
                        Toast.makeText(BookAppointmentActivity.this, "Booked: " + selectedDateTimeStr, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Throwable error) {
                        Toast.makeText(BookAppointmentActivity.this, "Booking failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(BookAppointmentActivity.this, "Error loading user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
