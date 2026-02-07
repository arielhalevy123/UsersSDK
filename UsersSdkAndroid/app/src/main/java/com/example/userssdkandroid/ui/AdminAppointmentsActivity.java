package com.example.userssdkandroid.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;
import com.example.userssdkandroid.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AdminAppointmentsActivity – גרסה המבוססת SDK:
 * - שימוש ב-UsersSdk.appointments() לכל פעולה על תורים
 * - בדיקות חפיפה דרך ה-SDK (מול כל myUsers)
 * - דיאלוג עריכת תורים מסודר (שורה לכל תור: עריכה/מחיקה + הוספת תור)
 */
public class AdminAppointmentsActivity extends AppCompatActivity
        implements AdminUsersAdapter.Listener {

    private RecyclerView list;
    private CircularProgressIndicator progress;
    private AdminUsersAdapter adapter;

    // תצורת אורך תור – נמשכת מה-SDK (ברירת מחדל 30 אם לא הוגדר)
    private int apptMinutes() {
        int m = UsersSdk.config().getAppointmentMinutes();
        return m > 0 ? m : 30;
    }

    // פורמט אחיד לתור
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appointments);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        list = findViewById(R.id.users_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUsersAdapter(this);
        list.setAdapter(adapter);

        progress = findViewById(R.id.progress);

        BottomNavigationView bottom = findViewById(R.id.bottom_navigation);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_calendar) {
                startActivity(new Intent(this, AdminCalendarActivity.class));
                return true;
            } else if (id == R.id.nav_admin_users) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
        bottom.setSelectedItemId(R.id.nav_admin_users);

        loadUsers();
    }

    private void loadUsers() {
        progress.setVisibility(android.view.View.VISIBLE);
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override public void onSuccess(List<UserDTO> users) {
                runOnUiThread(() -> {
                    progress.setVisibility(android.view.View.GONE);
                    adapter.submit(users);
                });
            }
            @Override public void onError(Throwable error) {
                runOnUiThread(() -> {
                    progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(AdminAppointmentsActivity.this,
                            "Error loading users", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ====== ממשק מה-Adapter ======

    /** לחיצה על "Add" בכרטיס משתמש – פתיחת Date/Time + הוספה עם בדיקת חפיפה דרך ה-SDK. */
    @Override
    public void onAddAppointment(UserDTO user) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "Requires Android O+", Toast.LENGTH_SHORT).show();
            return;
        }
        pickDateTime(candidate -> {
            if (!isValidDateTime(candidate)) {
                Toast.makeText(this, "Bad date format", Toast.LENGTH_SHORT).show();
                return;
            }

            // בדיקת חפיפה מול כל משתמשי האדמין (כולל אותו משתמש)
            UsersSdk.appointments().hasConflictAgainstMyUsers(
                    candidate,
                    apptMinutes(),
                    user.getId(),
                    /*allowSameUserId*/ false,
                    new UsersSdk.Callback<Boolean>() {
                        @Override public void onSuccess(Boolean conflict) {
                            if (conflict) {
                                runOnUiThread(() ->
                                        Toast.makeText(AdminAppointmentsActivity.this,
                                                "Conflict at " + candidate, Toast.LENGTH_SHORT).show());
                                return;
                            }
                            // אין חפיפה → מוסיפים
                            UsersSdk.appointments().add(user, candidate, new UsersSdk.Callback<UserDTO>() {
                                @Override public void onSuccess(UserDTO result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(AdminAppointmentsActivity.this,
                                                "Appointment added", Toast.LENGTH_SHORT).show();
                                        loadUsers();
                                    });
                                }
                                @Override public void onError(Throwable error) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AdminAppointmentsActivity.this,
                                                    "Error saving appointment", Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                        @Override public void onError(Throwable error) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminAppointmentsActivity.this,
                                            "Error checking conflict", Toast.LENGTH_SHORT).show());
                        }
                    }
            );
        });
    }

    /** עריכת *כלל* השדות (לא רק תורים) – כמו שהיה אצלך. */
    @Override public void onEditFields(UserDTO user) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        List<EditText> names = new ArrayList<>();
        List<EditText> values = new ArrayList<>();

        List<CustomFieldDTO> fields = user.getCustomFields();
        if (fields == null) fields = new ArrayList<>();

        for (CustomFieldDTO f : fields) {
            EditText etName = new EditText(this);
            etName.setHint("Field name");
            etName.setText(f.getFieldName());

            EditText etVal = new EditText(this);
            etVal.setHint("Field value");
            etVal.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            etVal.setText(f.getFieldValue());

            container.addView(etName);
            container.addView(etVal);
            names.add(etName);
            values.add(etVal);
        }

        // שדה ריק ליצירת חדש
        EditText newName = new EditText(this); newName.setHint("New field name");
        EditText newVal  = new EditText(this); newVal.setHint("New field value");
        container.addView(newName); container.addView(newVal);
        names.add(newName); values.add(newVal);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Fields")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    List<CustomFieldDTO> updated = new ArrayList<>();
                    for (int i = 0; i < names.size(); i++) {
                        String n = names.get(i).getText().toString().trim();
                        String v = values.get(i).getText().toString().trim();
                        if (!n.isEmpty()) updated.add(new CustomFieldDTO(n, v));
                    }
                    user.setCustomFields(updated);

                    UsersSdk.get().updateUser(user, new UsersSdk.Callback<UserDTO>() {
                        @Override public void onSuccess(UserDTO result) {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminAppointmentsActivity.this,
                                        "Saved", Toast.LENGTH_SHORT).show();
                                loadUsers();
                            });
                        }
                        @Override public void onError(Throwable error) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminAppointmentsActivity.this,
                                            "Error saving", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** עריכת פרטי המשתמש (שם/אימייל). */
    @Override public void onEditUser(UserDTO user) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(this);
        etName.setHint("Name");
        etName.setText(user.getName());

        EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setText(user.getEmail());

        container.addView(etName);
        container.addView(etEmail);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit User")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    user.setName(etName.getText().toString().trim());
                    user.setEmail(etEmail.getText().toString().trim());
                    UsersSdk.get().updateUser(user, new UsersSdk.Callback<UserDTO>() {
                        @Override public void onSuccess(UserDTO result) {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminAppointmentsActivity.this,
                                        "Saved", Toast.LENGTH_SHORT).show();
                                loadUsers();
                            });
                        }
                        @Override public void onError(Throwable error) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminAppointmentsActivity.this,
                                            "Error saving", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // אפשר שהאדפטר יקרא לאלו – השארתי למקרה שיש לך כבר כפתורי Edit/Delete ברמת הפריט:
    @Override
    public void onEditAppointment(UserDTO user, String oldValue) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        pickDateTime(newValue -> {
            if (!isValidDateTime(newValue)) {
                Toast.makeText(this, "Bad date format", Toast.LENGTH_SHORT).show();
                return;
            }
            // בדיקת חפיפה: נתעלם מחפיפה עם אותו משתמש ברמת ה-SDK,
            // ואז נבדוק חפיפה מקומית מול שאר תורי המשתמש (למעט הישן) כדי למנוע כפל.
            UsersSdk.appointments().hasConflictAgainstMyUsers(
                    newValue, apptMinutes(), user.getId(), /*allowSameUserId*/ true,
                    new UsersSdk.Callback<Boolean>() {
                        @Override public void onSuccess(Boolean conflictAcrossUsers) {
                            if (conflictAcrossUsers) {
                                runOnUiThread(() ->
                                        Toast.makeText(AdminAppointmentsActivity.this,
                                                "Conflict at " + newValue, Toast.LENGTH_SHORT).show());
                                return;
                            }
                            // בדיקת כפילות/חפיפה פנימית אצל אותו משתמש (למעט הישן)
                            List<String> vals = com.example.userssdk.appointments.AppointmentUtils.readValues(user);
                            for (String v : vals) {
                                if (v.trim().equals(oldValue.trim())) continue;
                                if (overlaps(v, newValue)) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AdminAppointmentsActivity.this,
                                                    "Overlaps user's other slot", Toast.LENGTH_SHORT).show());
                                    return;
                                }
                            }
                            // להחליף
                            UsersSdk.appointments().replace(user, oldValue, newValue, new UsersSdk.Callback<UserDTO>() {
                                @Override public void onSuccess(UserDTO result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(AdminAppointmentsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                        loadUsers();
                                    });
                                }
                                @Override public void onError(Throwable error) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AdminAppointmentsActivity.this,
                                                    "Error updating", Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                        @Override public void onError(Throwable error) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminAppointmentsActivity.this,
                                            "Error checking conflict", Toast.LENGTH_SHORT).show());
                        }
                    }
            );
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDeleteAppointment(UserDTO user, String value) {
        UsersSdk.appointments().delete(user, value, new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO result) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminAppointmentsActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadUsers();
                });
            }
            @Override public void onError(Throwable error) {
                runOnUiThread(() ->
                        Toast.makeText(AdminAppointmentsActivity.this,
                                "Error deleting", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ====== דיאלוג ניהול תורים (שורה לכל תור + הוספה) ======

    /** אם תרצה לפתוח דיאלוג עריכת תורים במקום כפתורי האדפטר – קרא לפונקציה הזו. */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openAppointmentsEditor(UserDTO user) {
        // שליפת הערכים מה-DTO (ללא רשת)
        List<String> items = com.example.userssdk.appointments.AppointmentUtils.readValues(user);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        rebuildAppointmentRows(user, items, container);

        Button addBtn = new Button(this);
        addBtn.setText("Add appointment");
        addBtn.setOnClickListener(v -> pickDateTime(newTime -> {
            if (!isValidDateTime(newTime)) {
                Toast.makeText(this, "Bad datetime format", Toast.LENGTH_SHORT).show();
                return;
            }
            // אין כפילות פנימית?
            for (String s : items) {
                if (s.trim().equalsIgnoreCase(newTime.trim())) {
                    Toast.makeText(this, "Duplicate for user", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // בדיקת חפיפה גלובלית
            UsersSdk.appointments().hasConflictAgainstMyUsers(
                    newTime, apptMinutes(), user.getId(), /*allowSameUserId*/ false,
                    new UsersSdk.Callback<Boolean>() {
                        @Override public void onSuccess(Boolean conflict) {
                            if (conflict) {
                                runOnUiThread(() ->
                                        Toast.makeText(AdminAppointmentsActivity.this,
                                                "Conflict at " + newTime, Toast.LENGTH_SHORT).show());
                                return;
                            }
                            UsersSdk.appointments().add(user, newTime, new UsersSdk.Callback<UserDTO>() {
                                @Override public void onSuccess(UserDTO result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(AdminAppointmentsActivity.this, "Added", Toast.LENGTH_SHORT).show();
                                        // עדכון UI: בניית שורות מחדש
                                        container.removeAllViews();
                                        List<String> fresh = com.example.userssdk.appointments.AppointmentUtils.readValues(result);
                                        rebuildAppointmentRows(result, fresh, container);
                                        ((ViewGroup) addBtn.getParent()).addView(addBtn);
                                        loadUsers();
                                    });
                                }
                                @Override public void onError(Throwable error) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AdminAppointmentsActivity.this,
                                                    "Error saving appointment", Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                        @Override public void onError(Throwable error) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminAppointmentsActivity.this,
                                            "Error checking conflict", Toast.LENGTH_SHORT).show());
                        }
                    }
            );
        }));
        container.addView(addBtn);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Manage Appointments")
                .setView(container)
                .setPositiveButton("Close", null)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void rebuildAppointmentRows(UserDTO user, List<String> items, LinearLayout parent) {
        parent.removeAllViews();

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            String initial = items.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            EditText et = new EditText(this);
            et.setSingleLine(true);
            et.setText(initial);
            et.setHint("yyyy-MM-dd HH:mm");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            et.setLayoutParams(lp);

            Button btnSave = new Button(this);
            btnSave.setText("Save");
            btnSave.setOnClickListener(v -> {
                String newVal = et.getText().toString().trim();
                if (!isValidDateTime(newVal)) {
                    Toast.makeText(this, "Bad datetime format", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newVal.equals(initial)) return;

                // כפילות פנימית (למעט עצמו)
                for (int j = 0; j < items.size(); j++) {
                    if (j != index && newVal.equals(items.get(j))) {
                        Toast.makeText(this, "Duplicate for user", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // חפיפה אצל אותו משתמש (למעט הישן)
                for (int j = 0; j < items.size(); j++) {
                    if (j == index) continue;
                    if (overlaps(items.get(j), newVal)) {
                        Toast.makeText(this, "Overlaps user's other slot", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // חפיפה גלובלית (מתעלם מחפיפה עם אותו משתמש – כבר בדקנו ידנית)
                UsersSdk.appointments().hasConflictAgainstMyUsers(
                        newVal, apptMinutes(), user.getId(), /*allowSameUserId*/ true,
                        new UsersSdk.Callback<Boolean>() {
                            @Override public void onSuccess(Boolean conflictAcrossUsers) {
                                if (conflictAcrossUsers) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AdminAppointmentsActivity.this,
                                                    "Conflict at " + newVal, Toast.LENGTH_SHORT).show());
                                    return;
                                }
                                UsersSdk.appointments().replace(user, initial, newVal, new UsersSdk.Callback<UserDTO>() {
                                    @Override public void onSuccess(UserDTO result) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(AdminAppointmentsActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                                            items.set(index, newVal);
                                            loadUsers();
                                        });
                                    }
                                    @Override public void onError(Throwable error) {
                                        runOnUiThread(() ->
                                                Toast.makeText(AdminAppointmentsActivity.this,
                                                        "Error saving", Toast.LENGTH_SHORT).show());
                                    }
                                });
                            }
                            @Override public void onError(Throwable error) {
                                runOnUiThread(() ->
                                        Toast.makeText(AdminAppointmentsActivity.this,
                                                "Error checking conflict", Toast.LENGTH_SHORT).show());
                            }
                        }
                );
            });

            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setOnClickListener(v -> {
                UsersSdk.appointments().delete(user, initial, new UsersSdk.Callback<UserDTO>() {
                    @Override public void onSuccess(UserDTO result) {
                        runOnUiThread(() -> {
                            Toast.makeText(AdminAppointmentsActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            parent.removeView(row);
                            items.remove(index);
                            loadUsers();
                        });
                    }
                    @Override public void onError(Throwable error) {
                        runOnUiThread(() ->
                                Toast.makeText(AdminAppointmentsActivity.this,
                                        "Error deleting", Toast.LENGTH_SHORT).show());
                    }
                });
            });

            row.addView(et);
            row.addView(btnSave);
            row.addView(btnDelete);
            parent.addView(row);
        }
    }

    // ====== עוזרים ======
    interface OnDateTimePicked { void onPicked(String formatted); }

    @RequiresApi(Build.VERSION_CODES.O)
    private void pickDateTime(OnDateTimePicked cb) {
        LocalDate today = LocalDate.now();
        DatePickerDialog dp = new DatePickerDialog(this, (v, y, m, d) -> {
            TimePickerDialog tp = new TimePickerDialog(this, (vv, hh, mm) -> {
                String formatted = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                        y, m + 1, d, hh, mm);
                cb.onPicked(formatted);
            }, LocalTime.now().getHour(), LocalTime.now().getMinute(), true);
            tp.show();
        }, today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth());
        dp.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isValidDateTime(String s) {
        try { LocalDateTime.parse(s, DT_FMT); return true; }
        catch (Exception e) { return false; }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean overlaps(String a, String b) {
        try {
            LocalDateTime s1 = LocalDateTime.parse(a, DT_FMT);
            LocalDateTime e1 = s1.plusMinutes(apptMinutes());
            LocalDateTime s2 = LocalDateTime.parse(b, DT_FMT);
            LocalDateTime e2 = s2.plusMinutes(apptMinutes());
            // חופפים אם אינם "גב אל גב"
            return !e1.isBefore(s2) && !s1.isAfter(e2);
        } catch (Exception ignored) { return false; }
    }
}