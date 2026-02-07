package com.example.userssdkandroid.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.userssdk.UsersSdk;
import com.example.userssdk.appointments.AppointmentUtils;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;
import com.example.userssdkandroid.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ProfileFragment extends Fragment {

    private TextView avatar, tvName, tvEmail, tvRole, tvUserId, tvAdminId, tvNextAppt, tvApptCount;
    private View rowAdminId;
    private RecyclerView rvFields;
    private ProgressBar progress;
    private FieldsAdapter adapter;

    private final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle st) {
        View v = inf.inflate(R.layout.fragment_profile, parent, false);

        avatar      = v.findViewById(R.id.avatarText);
        tvName      = v.findViewById(R.id.tvName);
        tvEmail     = v.findViewById(R.id.tvEmail);
        tvRole      = v.findViewById(R.id.tvRole);
        tvUserId    = v.findViewById(R.id.tvUserId);
        tvAdminId   = v.findViewById(R.id.tvAdminId);
        rowAdminId  = v.findViewById(R.id.rowAdminId);
        tvNextAppt  = v.findViewById(R.id.tvNextAppt);
        tvApptCount = v.findViewById(R.id.tvApptCount);
        progress    = v.findViewById(R.id.progress);

        rvFields = v.findViewById(R.id.rvFields);
        rvFields.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FieldsAdapter();
        rvFields.setAdapter(adapter);

        load();
        return v;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO u) {
                if (!isAdded()) return;

                // UI בסיסי של פרופיל
                List<CustomFieldDTO> filtered = filterNonAppointment(u.getCustomFields());
                filtered.sort(Comparator.comparing(cf -> {
                    String n = cf.getFieldName();
                    return n == null ? "" : n.toLowerCase(Locale.ROOT);
                }));

                requireActivity().runOnUiThread(() -> {
                    bindProfileHeader(u);
                    adapter.submit(filtered);
                });

                // לוגיקת תורים לפי תפקיד
                String role = safe(u.getRole()).toUpperCase(Locale.ROOT);
                if ("ADMIN".equals(role)) {
                    loadAdminStats(); // מכל myUsers
                } else {
                    loadUserStats(u); // מהמשתמש עצמו
                }
            }

            @Override public void onError(Throwable error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvNextAppt.setText(getString(R.string.userssdk_no_appointments));
                    tvApptCount.setText("0");
                });
            }
        });
    }

    private void loadUserStats(UserDTO u) {
        List<String> appts = AppointmentUtils.readValues(u);
        final int count = appts.size();
        final LocalDateTime soonest = findSoonestAfterNow(appts);

        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            progress.setVisibility(View.GONE);
            tvApptCount.setText(String.valueOf(count));
            tvNextAppt.setText(soonest == null ? getString(R.string.userssdk_no_appointments)
                    : DT_FMT.format(soonest));
        });
    }

    private void loadAdminStats() {
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override public void onSuccess(List<UserDTO> users) {
                int total = 0;
                List<String> allTimes = new ArrayList<>();
                for (UserDTO u : users) {
                    List<String> vals = AppointmentUtils.readValues(u);
                    total += vals.size();
                    allTimes.addAll(vals);
                }
                final int finalTotal = total;
                final LocalDateTime soonest = findSoonestAfterNow(allTimes);

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvApptCount.setText(String.valueOf(finalTotal));
                    tvNextAppt.setText(soonest == null ? getString(R.string.userssdk_no_appointments)
                            : DT_FMT.format(soonest));
                });
            }

            @Override public void onError(Throwable error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvApptCount.setText("0");
                    tvNextAppt.setText(getString(R.string.userssdk_no_appointments));
                });
            }
        });
    }

    // ===== עזרים =====

    private void bindProfileHeader(UserDTO u) {
        String name  = nullToDash(u.getName());
        String email = nullToDash(u.getEmail());
        String role  = nullToDash(u.getRole());
        long   id    = u.getId();
        Long   adminId = u.getAdminId();

        avatar.setText(name.isEmpty() ? "?" : name.substring(0,1).toUpperCase(Locale.getDefault()));
        tvName.setText(name);
        tvEmail.setText(email);
        tvRole.setText(role);
        tvUserId.setText(id > 0 ? String.valueOf(id) : "-");

        if (adminId != null) {
            tvAdminId.setText(String.valueOf(adminId));
            rowAdminId.setVisibility(View.VISIBLE);
        } else {
            rowAdminId.setVisibility(View.GONE);
        }
    }

    private List<CustomFieldDTO> filterNonAppointment(List<CustomFieldDTO> src) {
        List<CustomFieldDTO> out = new ArrayList<>();
        if (src == null) return out;
        for (CustomFieldDTO f : src) {
            String n = f.getFieldName() == null ? "" : f.getFieldName();
            if (!n.toLowerCase(Locale.ROOT).startsWith("appointment")) {
                out.add(f);
            }
        }
        return out;
    }

    private LocalDateTime findSoonestAfterNow(List<String> values) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime best = null;
        for (String s : values) {
            try {
                LocalDateTime t = LocalDateTime.parse(s.trim(), DT_FMT);
                if (t.isAfter(now) && (best == null || t.isBefore(best))) best = t;
            } catch (Exception ignore) {}
        }
        // אם אין עתידי בכלל, ניקח המאוחר ביותר (לא חובה, אבל נותן אינדיקציה)
        if (best == null) {
            for (String s : values) {
                try {
                    LocalDateTime t = LocalDateTime.parse(s.trim(), DT_FMT);
                    if (best == null || t.isAfter(best)) best = t;
                } catch (Exception ignore) {}
            }
        }
        return best;
    }

    private String nullToDash(String s){ return s == null || s.trim().isEmpty() ? "-" : s; }
    private String safe(String s) { return s == null ? "" : s; }

    // ===== Adapter לשדות מותאמים =====
    static class FieldsAdapter extends RecyclerView.Adapter<FieldsAdapter.VH> {
        private final List<CustomFieldDTO> items = new ArrayList<>();
        void submit(List<CustomFieldDTO> list) {
            items.clear();
            if (list != null) items.addAll(list);
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
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
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