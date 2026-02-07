package com.example.userssdk.appointments;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.O)
public final class ConflictDetector {

    private final int minutes;
    private final Map<LocalDateTime, Long> busy = new HashMap<>();
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    // <-- היה private? תהפוך ל-public
    public ConflictDetector(int minutes) {
        this.minutes = minutes;
    }

    // המתודה שחסרה לך
    public void indexFromUsers(List<UserDTO> users, String fieldName) {
        busy.clear();
        if (users == null) return;

        for (UserDTO u : users) {
            for (String v : AppointmentUtils.extractAppointments(u, fieldName)) {
                try {
                    LocalDateTime start = LocalDateTime.parse(v, DT_FMT);
                    busy.put(start, u.getId());
                } catch (Exception ignore) {}
            }
        }
    }

    // בדיקת חפיפה על בסיס האינדקס שבפנים
    public boolean hasConflict(String candidate, Long currentUserId, boolean allowSameUserId) {
        final LocalDateTime cStart;
        try { cStart = LocalDateTime.parse(candidate, DT_FMT); } catch (Exception e) { return true; }
        final LocalDateTime cEnd = cStart.plusMinutes(minutes);

        for (Map.Entry<LocalDateTime, Long> e : busy.entrySet()) {
            LocalDateTime aStart = e.getKey();
            Long holderId = e.getValue();
            if (allowSameUserId && Objects.equals(holderId, currentUserId)) continue;

            LocalDateTime aEnd = aStart.plusMinutes(minutes);
            boolean overlap = !cEnd.isBefore(aStart) && !cStart.isAfter(aEnd);
            if (overlap) return true;
        }
        return false;
    }

    // נוחות – בלי הפרמטר השלישי
    public boolean hasConflict(String candidate, Long currentUserId) {
        return hasConflict(candidate, currentUserId, false);
    }
}