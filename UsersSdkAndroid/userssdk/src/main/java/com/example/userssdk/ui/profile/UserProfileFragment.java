package com.example.userssdk.ui.profile;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.userssdk.R;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.appointments.AppointmentUtils;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.O)
public class UserProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole;
    private TextView tvNextAppt, tvApptCount; // ⬅️ מציגים גם ל-USER וגם ל-ADMIN
    private RecyclerView rvFields;
    private FieldsAdapter adapter;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.userssdk_fragment_user_profile, parent, false);

        tvName      = v.findViewById(R.id.tvName);
        tvEmail     = v.findViewById(R.id.tvEmail);
        tvRole      = v.findViewById(R.id.tvRole);
        tvNextAppt  = v.findViewById(R.id.tvNextAppt);
        tvApptCount = v.findViewById(R.id.tvApptCount);

        rvFields = v.findViewById(R.id.rvFields);
        rvFields.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FieldsAdapter();
        rvFields.setAdapter(adapter);

        loadUser();
        return v;
    }

    private void loadUser() {
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO u) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    tvName.setText("Name: " + safe(u.getName()));
                    tvEmail.setText("Email: " + safe(u.getEmail()));
                    tvRole.setText("Role: " + (u.getRole() != null ? u.getRole() : "-"));
                    adapter.submit(u.getCustomFields());
                });

                // חלוקה לפי תפקיד
                String role = u.getRole() == null ? "" : u.getRole().toUpperCase(Locale.ROOT);
                if ("ADMIN".equals(role)) {
                    loadAdminStats();
                } else {
                    loadUserStats(u);
                }
            }

            @Override public void onError(Throwable e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
                    setApptCount(0);
                    setNextApptText("—");
                });
            }
        });
    }

    /** סטטיסטיקות למשתמש רגיל (סופר ומוצא הקרוב של המשתמש עצמו). */
    private void loadUserStats(UserDTO user) {
        List<String> vals = AppointmentUtils.readValues(user);
        int count = vals.size();
        LocalDateTime soonest = findSoonestAfterNow(vals);

        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            setApptCount(count);
            setNextApptText(formatOrDash(soonest));
        });
    }

    /** סטטיסטיקות לאדמין (סופר ומוצא הקרוב מכל המשתמשים של האדמין). */
    private void loadAdminStats() {
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override public void onSuccess(List<UserDTO> users) {
                int totalCount = 0;
                List<String> all = new ArrayList<>();
                for (UserDTO u : users) {
                    List<String> vals = AppointmentUtils.readValues(u);
                    totalCount += vals.size();
                    all.addAll(vals);
                }
                LocalDateTime soonest = findSoonestAfterNow(all);

                final int finalCount = totalCount;
                final LocalDateTime finalSoonest = soonest;

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setApptCount(finalCount);
                    setNextApptText(formatOrDash(finalSoonest));
                });
            }

            @Override public void onError(Throwable error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setApptCount(0);
                    setNextApptText("—");
                });
            }
        });
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
        return best;
    }

    private void setApptCount(int n) { if (tvApptCount != null) tvApptCount.setText("Appointments count: " + n); }
    private void setNextApptText(String s) { if (tvNextAppt != null) tvNextAppt.setText("Next appointment: " + s); }
    private String formatOrDash(LocalDateTime t) { return t == null ? "—" : DT_FMT.format(t); }
    private String safe(String s) { return s == null ? "-" : s; }

    // ===== Recycler לשדות מותאמים =====
    static class FieldsAdapter extends RecyclerView.Adapter<FieldsAdapter.VH> {
        private final List<CustomFieldDTO> items = new ArrayList<>();
        void submit(List<CustomFieldDTO> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }
        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;
            VH(@NonNull View v) { super(v); tv = v.findViewById(android.R.id.text1); }
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            TextView t = new TextView(p.getContext());
            t.setId(android.R.id.text1);
            t.setTextSize(16f);
            int pad = (int)(16 * p.getResources().getDisplayMetrics().density);
            t.setPadding(pad, pad/2, pad, pad/2);
            return new VH(t);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            CustomFieldDTO f = items.get(i);
            String n = f.getFieldName() == null ? "-" : f.getFieldName();
            String v = f.getFieldValue() == null ? "-" : f.getFieldValue();
            h.tv.setText(n + ": " + v);
        }
        @Override public int getItemCount() { return items.size(); }
    }
}