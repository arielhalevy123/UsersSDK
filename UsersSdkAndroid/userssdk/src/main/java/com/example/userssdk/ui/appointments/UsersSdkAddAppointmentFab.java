package com.example.userssdk.ui.appointments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.userssdk.R;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.appointments.AppointmentUtils;
import com.example.userssdk.model.UserDTO;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.O)
public class UsersSdkAddAppointmentFab extends FrameLayout {

    public interface Listener {
        default void onAdded(String newTime, UserDTO updated) {}
        default void onConflict(String newTime) {}
        default void onDuplicate(String newTime) {}
        default void onError(Throwable error) {}
    }

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    private FloatingActionButton fab;
    private int durationMinutes = 30;       // ניתן לשינוי מאטריבוט או Setter
    private boolean allowSameUserId = false;
    @Nullable private Listener listener;
    @Nullable private UserDTO targetUser;   // אם אדמין רוצה להוסיף למשתמש ספציפי

    public UsersSdkAddAppointmentFab(Context c) { this(c, null); }
    public UsersSdkAddAppointmentFab(Context c, @Nullable AttributeSet a) { this(c, a, 0); }
    public UsersSdkAddAppointmentFab(Context c, @Nullable AttributeSet a, int defStyle) {
        super(c, a, defStyle);
        init(c, a);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        // קריאת אטריבוטים (אופציונלי)
        if (attrs != null) {
            final int[] set = { R.attr.durationMinutes, R.attr.allowSameUserId };
            final android.content.res.TypedArray ta = context.obtainStyledAttributes(attrs, set);
            try {
                if (ta.hasValue(0)) durationMinutes = ta.getInt(0, durationMinutes);
                if (ta.hasValue(1)) allowSameUserId = ta.getBoolean(1, allowSameUserId);
            } finally { ta.recycle(); }
        }

        fab = new FloatingActionButton(context);
        fab.setUseCompatPadding(true);
        fab.setImageResource(R.drawable.userssdk_ic_add); // האייקון המצורף למטה
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.BOTTOM);
        int m = (int)(16 * getResources().getDisplayMetrics().density);
        lp.setMargins(m, m, m, m);
        addView(fab, lp);

        fab.setOnClickListener(v -> openPickersAndAdd());
    }

    /** אם רוצים להוסיף למשתמש ספציפי (אדמין). אם לא—יוסיף ל-currentUser. */
    public void setTargetUser(@Nullable UserDTO user) { this.targetUser = user; }

    public void setListener(@Nullable Listener l) { this.listener = l; }
    public void setDurationMinutes(int minutes) { this.durationMinutes = minutes; }
    public void setAllowSameUserId(boolean allow) { this.allowSameUserId = allow; }

    private void openPickersAndAdd() {
        final LocalDate today = LocalDate.now();
        DatePickerDialog dp = new DatePickerDialog(getContext(),
                (view, y, m, d) -> {
                    TimePickerDialog tp = new TimePickerDialog(getContext(),
                            (vv, hh, mm) -> {
                                String newTime = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                                        y, m + 1, d, hh, mm);
                                proceedAdd(newTime);
                            },
                            LocalTime.now().getHour(), LocalTime.now().getMinute(), true);
                    tp.show();
                },
                today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth());
        dp.show();
    }

    private void proceedAdd(String newTime) {
        // ולידציית פורמט
        try { LocalDateTime.parse(newTime, DATE_TIME_FMT); }
        catch (Exception e) {
            Toast.makeText(getContext(), "Bad datetime format", Toast.LENGTH_SHORT).show();
            return;
        }

        // השג יעד: משתמש יעד (אם הועבר) או המשתמש המחובר
        if (targetUser != null) {
            checkAndAddForUser(targetUser, newTime);
        } else {
            UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
                @Override public void onSuccess(UserDTO user) { checkAndAddForUser(user, newTime); }
                @Override public void onError(Throwable error) {
                    if (listener != null) listener.onError(error);
                    Toast.makeText(getContext(), "Error loading user", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkAndAddForUser(UserDTO user, String newTime) {
        // 1) בדיקת חפיפה מול כל היוזרים של אותו אדמין
        UsersSdk.appointments().hasConflictAgainstMyUsers(
                newTime, durationMinutes, user.getId(), allowSameUserId,
                new UsersSdk.Callback<Boolean>() {
                    @Override public void onSuccess(Boolean conflict) {
                        if (Boolean.TRUE.equals(conflict)) {
                            if (listener != null) listener.onConflict(newTime);
                            Toast.makeText(getContext(),
                                    "Admin not available at " + newTime,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 2) כפילות אצל המשתמש עצמו
                        List<String> curr = AppointmentUtils.readValues(user);
                        if (curr.contains(newTime)) {
                            if (listener != null) listener.onDuplicate(newTime);
                            Toast.makeText(getContext(),
                                    "Already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 3) הוספה בפועל
                        UsersSdk.appointments().add(user, newTime, new UsersSdk.Callback<UserDTO>() {
                            @Override public void onSuccess(UserDTO updated) {
                                if (listener != null) listener.onAdded(newTime, updated);
                                Toast.makeText(getContext(),
                                        "Appointment added: " + newTime,
                                        Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(Throwable error) {
                                if (listener != null) listener.onError(error);
                                Toast.makeText(getContext(),
                                        "Error saving appointment", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override public void onError(Throwable error) {
                        if (listener != null) listener.onError(error);
                        Toast.makeText(getContext(),
                                "Error checking conflicts", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}