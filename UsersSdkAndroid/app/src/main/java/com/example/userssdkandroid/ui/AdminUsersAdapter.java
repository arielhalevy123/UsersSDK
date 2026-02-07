package com.example.userssdkandroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;
import com.example.userssdkandroid.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.UserVH> {

    interface Listener {
        void onAddAppointment(UserDTO user);
        void onEditFields(UserDTO user);
        void onEditUser(UserDTO user);
        void onEditAppointment(UserDTO user, String oldValue);
        void onDeleteAppointment(UserDTO user, String value);
    }

    private final Listener listener;
    private final List<UserDTO> data = new ArrayList<>();

    AdminUsersAdapter(Listener l) { this.listener = l; }

    void submit(List<UserDTO> users) {
        data.clear();
        if (users != null) data.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH h, int pos) {
        UserDTO u = data.get(pos);
        h.tvName.setText(String.format(Locale.US, "Name: %s (id=%d)", u.getName(), u.getId()));
        h.tvEmail.setText("Email: " + u.getEmail());

        // כפתורי ניהול
        h.btnAdd.setOnClickListener(v -> listener.onAddAppointment(u));
        h.btnEditFields.setOnClickListener(v -> listener.onEditFields(u));
        h.btnEditUser.setOnClickListener(v -> listener.onEditUser(u));

        // רשימת תורים
        List<String> appts = parseAppointments(u);
        AppointmentRowAdapter child = new AppointmentRowAdapter(appts,
                value -> listener.onEditAppointment(u, value),
                value -> listener.onDeleteAppointment(u, value));
        h.rv.setLayoutManager(new LinearLayoutManager(h.itemView.getContext()));
        h.rv.setAdapter(child);
    }

    @Override public int getItemCount() { return data.size(); }

    static class UserVH extends RecyclerView.ViewHolder {
        final TextView tvName, tvEmail;
        final Button btnAdd, btnEditFields, btnEditUser;
        final RecyclerView rv;
        UserVH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnAdd = itemView.findViewById(R.id.btnAddAppt);
            btnEditFields = itemView.findViewById(R.id.btnEditFields);
            btnEditUser = itemView.findViewById(R.id.btnEditUser);
            rv = itemView.findViewById(R.id.rvAppointments);
        }
    }

    private List<String> parseAppointments(UserDTO u) {
        List<String> out = new ArrayList<>();
        List<CustomFieldDTO> fields = u.getCustomFields();
        if (fields == null) return out;
        for (CustomFieldDTO f : fields) {
            String n = f.getFieldName(), v = f.getFieldValue();
            if (n == null || v == null) continue;
            if (!n.toLowerCase().startsWith("appointment")) continue;
            out.addAll(Arrays.asList(v.split(";")));
        }
        // ניקוי רווחים וריקים
        List<String> clean = new ArrayList<>();
        for (String s : out) {
            String t = s.trim();
            if (!t.isEmpty()) clean.add(t);
        }
        return clean;
    }
}