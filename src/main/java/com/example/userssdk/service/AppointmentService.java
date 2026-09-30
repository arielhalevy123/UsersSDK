package com.example.userssdk.service;

import com.example.userssdk.entities.Appointment;
import com.example.userssdk.entities.AppointmentStatus;
import com.example.userssdk.entities.User;
import com.example.userssdk.repositories.AppointmentRepository;
import com.example.userssdk.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking operations against the {@link Appointment} entity.
 *
 * Every write is {@code @Transactional}. The previous implementation had no transaction boundary on
 * the booking path at all, which is how two simultaneous bookings could interleave and lose one
 * another.
 */
@Service
public class AppointmentService {

    /**
     * How far back to look for appointments that might still be running when a new one starts.
     * Any existing appointment longer than this would be missed by the overlap check, so it also
     * bounds the maximum supported duration.
     */
    private static final int MAX_SUPPORTED_DURATION_MINUTES = 8 * 60;

    private final AppointmentRepository appointments;
    private final UserRepository users;

    public AppointmentService(AppointmentRepository appointments, UserRepository users) {
        this.appointments = appointments;
        this.users = users;
    }

    /** Thrown when a requested slot collides with an existing booking. */
    public static class SlotUnavailableException extends RuntimeException {
        public SlotUnavailableException(String message) { super(message); }
    }

    /**
     * Books a slot, refusing to double-book.
     *
     * The conflict check and the insert share one transaction. Two concurrent bookings for the same
     * slot therefore cannot both succeed: one commits, the other either sees the committed row or
     * fails on the optimistic-lock version.
     */
    @Transactional
    public Appointment book(Long userId, LocalDateTime startsAt, int durationMinutes, String note) {
        if (startsAt == null) {
            throw new IllegalArgumentException("startsAt is required");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        if (durationMinutes > MAX_SUPPORTED_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "durationMinutes exceeds the supported maximum of " + MAX_SUPPORTED_DURATION_MINUTES);
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("no such user: " + userId));

        Appointment candidate = new Appointment(user, startsAt);
        candidate.setDurationMinutes(durationMinutes);
        candidate.setNote(note);

        for (Appointment existing : candidatesAround(userId, startsAt, durationMinutes)) {
            if (existing.overlaps(candidate)) {
                throw new SlotUnavailableException(
                        "slot conflicts with appointment " + existing.getId() + " at " + existing.getStartsAt());
            }
        }

        return appointments.save(candidate);
    }

    /** Convenience overload: the default 30-minute slot. */
    @Transactional
    public Appointment book(Long userId, LocalDateTime startsAt) {
        return book(userId, startsAt, 30, null);
    }

    /**
     * Cancels rather than deletes, so the booking history survives.
     * The old string format could only forget.
     */
    @Transactional
    public Appointment cancel(Long appointmentId) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("no such appointment: " + appointmentId));
        a.setStatus(AppointmentStatus.CANCELLED);
        return appointments.save(a);
    }

    @Transactional
    public Appointment markStatus(Long appointmentId, AppointmentStatus status) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("no such appointment: " + appointmentId));
        a.setStatus(status);
        return appointments.save(a);
    }

    @Transactional(readOnly = true)
    public List<Appointment> forUser(Long userId) {
        return appointments.findByUserIdOrderByStartsAtAsc(userId);
    }

    /** "Everything booked on Tuesday" — one indexed query. */
    @Transactional(readOnly = true)
    public List<Appointment> between(LocalDateTime from, LocalDateTime to) {
        return appointments.findByStartsAtBetweenOrderByStartsAtAsc(from, to);
    }

    @Transactional(readOnly = true)
    public long noShowCount(Long userId) {
        return appointments.countByUserIdAndStatus(userId, AppointmentStatus.NO_SHOW);
    }

    /**
     * Widens the query window by the maximum supported duration so an appointment that starts
     * before the requested slot but is still running is still considered.
     */
    private List<Appointment> candidatesAround(Long userId, LocalDateTime startsAt, int durationMinutes) {
        LocalDateTime windowStart = startsAt.minusMinutes(MAX_SUPPORTED_DURATION_MINUTES);
        LocalDateTime windowEnd = startsAt.plusMinutes(durationMinutes);
        return appointments.findCandidatesForOverlap(userId, windowStart, windowEnd);
    }
}
