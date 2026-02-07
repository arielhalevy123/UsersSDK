// sdk: com.example.userssdk.appointments.AppointmentManager
package com.example.userssdk.appointments;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;
import com.example.userssdk.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.O)
public class AppointmentManager {

    private final int minutes;
    private final String fieldName;

    public AppointmentManager() {
        this.minutes = UsersSdk.config().getAppointmentMinutes();        // ברירת מחדל 30
        this.fieldName = UsersSdk.config().getAppointmentFieldName();    // "Appointment"
    }

    // שליפת רשימת התורים כ-Strings (לשימוש UI קיים)

    public List<String> getAppointments(@NonNull UserDTO user) {
        return AppointmentUtils.extractAppointments(user, fieldName);
    }

    // בניית Index תפוסות לאדמין מכל המשתמשים
    public void buildAdminBusyIndex(UsersSdk.Callback<ConflictDetector> cb) {
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override public void onSuccess(List<UserDTO> users) {
                ConflictDetector cd = new ConflictDetector(minutes);
                cd.indexFromUsers(users, fieldName);
                if (cb != null) cb.onSuccess(cd);
            }
            @Override public void onError(Throwable error) {
                if (cb != null) cb.onError(error);
            }
        });
    }

    // הוספת תור למשתמש (כולל בדיקת חפיפה מול האדמין)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void addAppointment(UserDTO user, String newTime, ConflictDetector cd,
                               UsersSdk.Callback<UserDTO> cb) {
        if (cd != null && cd.hasConflict(newTime, user.getId())) {
            cb.onError(new IllegalStateException("Conflict"));
            return;
        }
        List<String> items = getAppointments(user);
        if (items == null) items = new ArrayList<>();
        if (!items.contains(newTime)) items.add(newTime);
        saveAppointments(user, items, cb);
    }

    // עריכת תור קיים
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void editAppointment(UserDTO user, String oldVal, String newVal, ConflictDetector cd,
                                UsersSdk.Callback<UserDTO> cb) {
        if (cd != null && cd.hasConflict(newVal, user.getId())) {
            cb.onError(new IllegalStateException("Conflict"));
            return;
        }
        List<String> items = getAppointments(user);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).trim().equals(oldVal.trim())) {
                items.set(i, newVal);
                break;
            }
        }
        saveAppointments(user, items, cb);
    }

    // מחיקת תור
    public void deleteAppointment(UserDTO user, String value, UsersSdk.Callback<UserDTO> cb) {
        List<String> items = getAppointments(user);
        items.removeIf(s -> s.trim().equals(value.trim()));
        saveAppointments(user, items, cb);
    }

    // שמירה לשרת
    private void saveAppointments(UserDTO user, List<String> items, UsersSdk.Callback<UserDTO> cb) {
        String joined = String.join("; ", items);
        List<CustomFieldDTO> fields = user.getCustomFields();
        if (fields == null) fields = new ArrayList<>();
        boolean found = false;
        for (CustomFieldDTO f : fields) {
            if (f.getFieldName() != null && f.getFieldName().equalsIgnoreCase(fieldName)) {
                f.setFieldValue(joined); found = true; break;
            }
        }
        if (!found) fields.add(new CustomFieldDTO(fieldName, joined));
        user.setCustomFields(fields);

        UsersSdk.get().updateUser(user, new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO res) { if (cb != null) cb.onSuccess(res); }
            @Override public void onError(Throwable e) { if (cb != null) cb.onError(e); }
        });
    }
}