package com.example.userssdkandroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.userssdkandroid.R;
import java.util.List;

class AppointmentRowAdapter extends RecyclerView.Adapter<AppointmentRowAdapter.VH> {

    interface OnEdit { void onEdit(String value); }
    interface OnDelete { void onDelete(String value); }

    private final List<String> items;
    private final OnEdit onEdit;
    private final OnDelete onDelete;

    AppointmentRowAdapter(List<String> items, OnEdit onEdit, OnDelete onDelete) {
        this.items = items;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_appt, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String value = items.get(position).trim();
        h.tv.setText(value);
        h.btnEdit.setOnClickListener(v -> onEdit.onEdit(value));
        h.btnDelete.setOnClickListener(v -> onDelete.onDelete(value));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        final Button btnEdit, btnDelete;
        VH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvAppt);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}