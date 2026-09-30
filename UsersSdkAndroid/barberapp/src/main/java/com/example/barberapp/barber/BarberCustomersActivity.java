package com.example.barberapp.barber;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberapp.R;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.UserDTO;

import java.util.ArrayList;
import java.util.List;

public class BarberCustomersActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView tvEmpty;
    private final List<UserDTO> customers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_customers);

        tvEmpty = findViewById(R.id.tv_empty);
        recycler = findViewById(R.id.recycler_customers);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new CustomersAdapter(customers, customer -> {
            startActivity(new Intent(this, BarberCustomerDetailActivity.class)
                    .putExtra(BarberCustomerDetailActivity.EXTRA_USER_ID, customer.getId()));
        }));

        loadCustomers();
    }

    private void loadCustomers() {
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override
            public void onSuccess(List<UserDTO> result) {
                customers.clear();
                if (result != null) customers.addAll(result);
                runOnUiThread(() -> {
                    recycler.getAdapter().notifyDataSetChanged();
                    tvEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }
        });
    }

    private static class CustomersAdapter extends RecyclerView.Adapter<CustomersAdapter.VH> {
        private final List<UserDTO> items;
        private final OnCustomerClick onCustomerClick;

        interface OnCustomerClick {
            void onClick(UserDTO customer);
        }

        CustomersAdapter(List<UserDTO> items, OnCustomerClick onCustomerClick) {
            this.items = items;
            this.onCustomerClick = onCustomerClick;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            UserDTO u = items.get(position);
            holder.tvName.setText(u.getName() != null ? u.getName() : "—");
            holder.tvEmail.setText(u.getEmail() != null ? u.getEmail() : "—");
            holder.itemView.setOnClickListener(v -> onCustomerClick.onClick(u));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvName, tvEmail;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_customer_name);
                tvEmail = itemView.findViewById(R.id.tv_customer_email);
            }
        }
    }
}
