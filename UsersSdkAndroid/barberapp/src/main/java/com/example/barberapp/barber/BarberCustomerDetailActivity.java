package com.example.barberapp.barber;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberapp.R;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.appointments.AppointmentUtils;
import com.example.userssdk.model.UserDTO;
import com.example.userssdk.ui.appointments.UsersSdkAddAppointmentFab;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class BarberCustomerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private long userId;
    private UserDTO customer;
    private TextView tvCustomerName, tvEmpty;
    private RecyclerView recycler;
    private List<String> appointments = new ArrayList<>();
    private RecyclerView.Adapter<?> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_customer_detail);

        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        if (userId < 0) {
            Toast.makeText(this, "Invalid customer", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvEmpty = findViewById(R.id.tv_empty);
        recycler = findViewById(R.id.recycler_appointments);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView tv = holder.itemView.findViewById(R.id.tv_datetime);
                Button btn = holder.itemView.findViewById(R.id.btn_cancel);
                if (tv != null) tv.setText(appointments.get(position));
                if (btn != null) btn.setVisibility(View.GONE);
            }

            @Override
            public int getItemCount() {
                return appointments.size();
            }
        };
        recycler.setAdapter(adapter);

        UsersSdkAddAppointmentFab fab = findViewById(R.id.fab_add_appointment);

        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override
            public void onSuccess(List<UserDTO> users) {
                for (UserDTO u : users) {
                    if (u.getId() == userId) {
                        customer = u;
                        break;
                    }
                }
                if (customer == null) {
                    Toast.makeText(BarberCustomerDetailActivity.this, "Customer not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                runOnUiThread(() -> {
                    tvCustomerName.setText(customer.getName() != null ? customer.getName() : ("ID " + customer.getId()));
                    List<String> list = AppointmentUtils.readValues(customer);
                    appointments.clear();
                    if (list != null) appointments.addAll(list);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);

                    fab.setTargetUser(customer);
                    fab.setListener(new UsersSdkAddAppointmentFab.Listener() {
                        @Override
                        public void onAdded(String newTime, UserDTO updated) {
                            customer = updated;
                            appointments.clear();
                            List<String> list = AppointmentUtils.readValues(updated);
                            if (list != null) appointments.addAll(list);
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                });
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(BarberCustomerDetailActivity.this, "Failed to load", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
