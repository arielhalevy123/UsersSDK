package com.example.userssdk.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An appointment as a first-class entity.
 *
 * The original design stored appointments as a semicolon-separated string inside a single
 * {@link UserCustomField} row. That kept the SDK generic — no domain-specific table in a
 * general-purpose user library — but it cost four things:
 *
 * <ul>
 *   <li><b>Queryability.</b> "every booking on Tuesday" meant loading every row and parsing strings.</li>
 *   <li><b>Per-appointment data.</b> No duration, no status, no note; a single booking could not be
 *       cancelled without rewriting the whole list.</li>
 *   <li><b>Referential integrity.</b> A booking was text, not a row.</li>
 *   <li><b>Concurrency.</b> Every booking rewrote one shared string, so simultaneous bookings lost
 *       each other — reproduced in {@code AppointmentConcurrencyTest}.</li>
 * </ul>
 *
 * This entity addresses all four. The custom-field path is left intact so existing SDK clients keep
 * working; {@code AppointmentMigrationService} can backfill from it.
 */
@Entity
@Table(
    name = "appointments",
    indexes = {
        @Index(name = "idx_appointment_user", columnList = "user_id"),
        @Index(name = "idx_appointment_start", columnList = "startsAt"),
        @Index(name = "idx_appointment_user_start", columnList = "user_id,startsAt")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who the appointment belongs to. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Start of the slot. Indexed so date-range queries do not scan the table. */
    @Column(nullable = false)
    private LocalDateTime startsAt;

    /** Length in minutes. Defaults to a 30-minute slot. */
    @Column(nullable = false)
    private int durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    /** Free-text note — the thing the string format could never carry. */
    @Column(length = 500)
    private String note;

    /** Optimistic locking, for the same reason it was added to UserCustomField. */
    @Version
    private Long version;

    public Appointment(User user, LocalDateTime startsAt) {
        this.user = user;
        this.startsAt = startsAt;
        this.durationMinutes = 30;
        this.status = AppointmentStatus.BOOKED;
    }

    /** Exclusive end of the slot. */
    @Transient
    public LocalDateTime getEndsAt() {
        return startsAt.plusMinutes(durationMinutes);
    }

    /**
     * True when this slot overlaps another. Half-open interval [start, end), so a 10:00–10:30
     * booking and a 10:30–11:00 booking do not conflict.
     *
     * Cancelled appointments never conflict — the slot is free again.
     */
    public boolean overlaps(Appointment other) {
        if (other == null) return false;
        if (this.status == AppointmentStatus.CANCELLED || other.status == AppointmentStatus.CANCELLED) {
            return false;
        }
        return this.startsAt.isBefore(other.getEndsAt()) && other.startsAt.isBefore(this.getEndsAt());
    }
}
