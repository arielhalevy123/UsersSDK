package com.example.userssdkandroid.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.arielhalevy123.userssdk.UsersSdk;
import io.github.arielhalevy123.userssdk.appointments.AppointmentUtils;
import io.github.arielhalevy123.userssdk.model.CustomFieldDTO;
import io.github.arielhalevy123.userssdk.model.UserDTO;
import com.example.userssdkandroid.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ProfileActivity extends AppCompatActivity {

    private CircularProgressIndicator progress;
    private TextView avatar, tvName, tvEmail, tvRole, tvUserId, tvAdminId, tvNextAppt, tvApptCount;
    private View rowAdminId;
    private RecyclerView rvFields;
    private MaterialButton btnLogout;

    private final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("הפרופיל שלי");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        progress     = findViewById(R.id.progress);
        avatar       = findViewById(R.id.avatarText);
        tvName       = findViewById(R.id.tvName);
        tvEmail      = findViewById(R.id.tvEmail);
        tvRole       = findViewById(R.id.tvRole);
        tvUserId     = findViewById(R.id.tvUserId);
        tvAdminId    = findViewById(R.id.tvAdminId);
        rowAdminId   = findViewById(R.id.rowAdminId);
        tvNextAppt   = findViewById(R.id.tvNextAppt);
        tvApptCount  = findViewById(R.id.tvApptCount);
        rvFields     = findViewById(R.id.rvFields);
        btnLogout    = findViewById(R.id.btnLogout);

        rvFields.setLayoutManager(new LinearLayoutManager(this));
        rvFields.setAdapter(new CustomFieldsAdapter(Collections.emptyList()));
        rvFields.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        btnLogout.setOnClickListener(v -> {
            UsersSdk.get().logout();
            Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();
            finish();
        });

        loadProfile();
    }

    private void loadProfile() {
        progress.setVisibility(View.VISIBLE);
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO user) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    bindUser(user);
                });
            }
            @Override public void onError(Throwable error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this, "נכשלה טעינת הפרופיל", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void bindUser(UserDTO u) {
        String name  = safe(u.getName());
        String email = safe(u.getEmail());
        String role  = safe(u.getRole());
        Long id      = u.getId();               // auto-boxing
        Long adminId = u.getAdminId();          // ← מגיע ישר מ-/me

        // אווטאר עם האות הראשונה של השם
        avatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.getDefault()));

        tvName.setText(name);
        tvEmail.setText(email);
        tvRole.setText(role);
        tvUserId.setText(id != null ? String.valueOf(id) : "-");

        // Admin ID – רק אם קיים
        if (rowAdminId != null && tvAdminId != null) {
            if (adminId != null) {
                tvAdminId.setText(String.valueOf(adminId));
                rowAdminId.setVisibility(View.VISIBLE);
            } else {
                rowAdminId.setVisibility(View.GONE);
            }
        }

        // תורים: לאדמין – סיכום מכל המשתמשים שלו; למשתמש רגיל – מהמשתמש עצמו
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            loadAdminAppointmentStats();
        } else {
            List<String> appts = AppointmentUtils.readValues(u);
            tvApptCount.setText(String.valueOf(appts.size()));
            tvNextAppt.setText(calcNext(appts));
        }

        // שדות מותאמים (מלבד Appointment*)
        List<CustomFieldDTO> fields = new ArrayList<>();
        if (u.getCustomFields() != null) {
            for (CustomFieldDTO f : u.getCustomFields()) {
                String n = safe(f.getFieldName());
                if (!n.toLowerCase(Locale.ROOT).startsWith("appointment")) {
                    fields.add(f);
                }
            }
        }
        // מיון לפי שם שדה
        fields.sort(Comparator.comparing(cf -> safe(cf.getFieldName()).toLowerCase(Locale.ROOT)));

        ((CustomFieldsAdapter) rvFields.getAdapter()).submit(fields);
    }

    /** For admin: fetch my-users and show total appointment count + next appointment across all managed users. */
    private void loadAdminAppointmentStats() {
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override
            public void onSuccess(List<UserDTO> users) {
                int total = 0;
                List<String> allAppts = new ArrayList<>();
                if (users != null) {
                    for (UserDTO u : users) {
                        List<String> vals = AppointmentUtils.readValues(u);
                        total += vals.size();
                        allAppts.addAll(vals);
                    }
                }
                final int count = total;
                final String nextText = calcNext(allAppts);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvApptCount != null) tvApptCount.setText(String.valueOf(count));
                    if (tvNextAppt != null) tvNextAppt.setText(nextText);
                });
            }
            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvApptCount != null) tvApptCount.setText("0");
                    if (tvNextAppt != null) tvNextAppt.setText("אין תורים");
                });
            }
        });
    }

    private String calcNext(List<String> appts) {
        if (appts == null || appts.isEmpty()) return "אין תורים";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime best = null;
        for (String s : appts) {
            try {
                LocalDateTime dt = LocalDateTime.parse(s.trim(), DATE_TIME_FMT);
                if (!dt.isBefore(now) && (best == null || dt.isBefore(best))) best = dt;
            } catch (Exception ignore) {}
        }
        if (best != null) return best.format(DATE_TIME_FMT);

        for (String s : appts) {
            try {
                LocalDateTime dt = LocalDateTime.parse(s.trim(), DATE_TIME_FMT);
                if (best == null || dt.isAfter(best)) best = dt;
            } catch (Exception ignore) {}
        }
        return best != null ? best.format(DATE_TIME_FMT) : "אין תורים";
    }

    private String safe(String s) { return s == null ? "" : s; }

    // ===== Adapter לשדות מותאמים =====
    static class CustomFieldsAdapter extends RecyclerView.Adapter<CustomFieldsAdapter.VH> {
        private final List<CustomFieldDTO> items = new ArrayList<>();
        CustomFieldsAdapter(List<CustomFieldDTO> initial) {
            if (initial != null) items.addAll(initial);
        }
        void submit(List<CustomFieldDTO> newData) {
            items.clear();
            if (newData != null) items.addAll(newData);
            notifyDataSetChanged();
        }
        static class VH extends RecyclerView.ViewHolder {
            final TextView title, value;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.tvFieldTitle);
                value = v.findViewById(R.id.tvFieldValue);
            }
        }
        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_profile_field, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            CustomFieldDTO f = items.get(pos);
            h.title.setText(f.getFieldName() == null ? "-" : f.getFieldName());
            h.value.setText(f.getFieldValue() == null ? "-" : f.getFieldValue());
        }
        @Override public int getItemCount() { return items.size(); }
    }
}