package com.example.userssdk.entities;

/**
 * Lifecycle of an appointment.
 *
 * The string-based design had no way to express this: a slot either appeared in the list or it did
 * not. Cancelling meant deleting the text, which destroyed the record that a booking ever existed.
 */
public enum AppointmentStatus {
    /** Reserved and expected. */
    BOOKED,
    /** Called off. Retained for history; never blocks the slot. */
    CANCELLED,
    /** The customer attended. */
    COMPLETED,
    /** The customer did not attend — the fact a barber most wants to track. */
    NO_SHOW
}
