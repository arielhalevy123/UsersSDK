package com.example.barberapp.customer;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberapp.R;
import io.github.arielhalevy123.userssdk.UsersSdk;
import io.github.arielhalevy123.userssdk.model.UserDTO;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyAppointmentsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView tvEmpty;
    private List<String> appointments = new ArrayList<>();
    private AppointmentsAdapter adapter;
    private UserDTO currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);

        tvEmpty = findViewById(R.id.tv_empty);
        recycler = findViewById(R.id.recycler_appointments);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentsAdapter(appointments, this::onCancelAppointment);
        recycler.setAdapter(adapter);

        loadAppointments();
    }

    private void loadAppointments() {
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO user) {
                currentUser = user;
                if (user != null) {
                    UsersSdk.appointments().listValues(user, new UsersSdk.Callback<List<String>>() {
                        @Override
                        public void onSuccess(List<String> result) {
                            appointments.clear();
                            if (result != null) appointments.addAll(result);
                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                        }

                        @Override
                        public void onError(Throwable error) {
                            runOnUiThread(() -> Toast.makeText(MyAppointmentsActivity.this, "Failed to load", Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(MyAppointmentsActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCancelAppointment(String value) {
        if (currentUser == null) return;
        UsersSdk.appointments().delete(currentUser, value, new UsersSdk.Callback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO updated) {
                currentUser = updated;
                appointments.remove(value);
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(MyAppointmentsActivity.this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> Toast.makeText(MyAppointmentsActivity.this, "Cancel failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.VH> {
        private final List<String> items;
        private final OnCancelListener onCancel;

        interface OnCancelListener {
            void onCancel(String value);
        }

        AppointmentsAdapter(List<String> items, OnCancelListener onCancel) {
            this.items = items;
            this.onCancel = onCancel;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String value = items.get(position);
            holder.tvDatetime.setText(value);
            holder.btnCancel.setOnClickListener(v -> onCancel.onCancel(value));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvDatetime;
            final Button btnCancel;

            VH(View itemView) {
                super(itemView);
                tvDatetime = itemView.findViewById(R.id.tv_datetime);
                btnCancel = itemView.findViewById(R.id.btn_cancel);
            }
        }
    }
}
