package com.example.userssdk.appointments;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.O)
public final class AppointmentUtils {

    public static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    private static final String FIELD_NAME = "Appointment";

    private AppointmentUtils() {}

    /** מחזיר/יוצר את השדה Appointment של המשתמש. */
    public static CustomFieldDTO getOrCreateField(UserDTO user) {
        List<CustomFieldDTO> fields = user.getCustomFields();
        if (fields == null) {
            fields = new ArrayList<>();
            user.setCustomFields(fields);
        }
        for (CustomFieldDTO f : fields) {
            if (f.getFieldName() != null &&
                    f.getFieldName().equalsIgnoreCase(FIELD_NAME)) {
                return f;
            }
        }
        CustomFieldDTO created = new CustomFieldDTO(FIELD_NAME, "");
        fields.add(created);
        return created;
    }

    /** מפצל מחרוזת תורים ל־List<String> לפי ; */
    public static List<String> split(String value) {
        List<String> out = new ArrayList<>();
        if (value == null || value.isEmpty()) return out;
        for (String s : value.split(";")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** מאחד רשימת ערכים למחרוזת אחת עם ; ורווח. */
    public static String join(List<String> list) {
        return String.join("; ", list);
    }

    /** קורא את התורים של משתמש כרשימת String (ללא דקות). */
    public static List<String> readValues(UserDTO user) {
        CustomFieldDTO f = getOrCreateField(user);
        return split(f.getFieldValue());
    }

    /** קובע למשתמש את רשימת התורים כטקסט. */
    public static void writeValues(UserDTO user, List<String> values) {
        CustomFieldDTO f = getOrCreateField(user);
        f.setFieldValue(join(values));
    }

    /** מוסיף ערך טקסטואלי (כבר בפורמט yyyy-MM-dd HH:mm). */
    public static void addValue(UserDTO user, String value) {
        List<String> list = readValues(user);
        list.add(value.trim());
        writeValues(user, list);
    }

    /** מחליף ערך ישן בחדש. */
    public static void replaceValue(UserDTO user, String oldValue, String newValue) {
        List<String> list = readValues(user);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).trim().equals(oldValue.trim())) {
                list.set(i, newValue.trim());
                break;
            }
        }
        writeValues(user, list);
    }

    /** מוחק ערך. */
    public static void removeValue(UserDTO user, String value) {
        List<String> list = readValues(user);
        list.removeIf(s -> s.trim().equals(value.trim()));
        writeValues(user, list);
    }

    /** פרסינג מלא לאובייקטי Appointment עם דקות ברירת מחדל. */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static List<Appointment> parse(UserDTO user, int defaultMinutes) {
        List<Appointment> out = new ArrayList<>();
        for (String s : readValues(user)) {
            try {
                LocalDateTime start = LocalDateTime.parse(s, FMT);
                out.add(new Appointment(start, defaultMinutes));
            } catch (Exception ignore) {}
        }
        return out;
    }
    public static List<String> extractAppointments(UserDTO user, String fieldName) {
        List<String> out = new ArrayList<>();
        if (user == null) return out;
        List<CustomFieldDTO> fields = user.getCustomFields();
        if (fields == null) return out;

        String key = (fieldName == null ? "Appointment" : fieldName).toLowerCase(Locale.US);

        for (CustomFieldDTO f : fields) {
            if (f.getFieldName() == null || f.getFieldValue() == null) continue;
            if (!f.getFieldName().toLowerCase(Locale.US).startsWith(key)) continue;

            for (String part : f.getFieldValue().split(";")) {
                String s = part.trim();
                if (!s.isEmpty()) out.add(s);
            }
        }
        return out;
    }
}