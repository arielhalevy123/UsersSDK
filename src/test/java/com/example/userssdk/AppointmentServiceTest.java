package com.example.userssdk;

import com.example.userssdk.entities.Appointment;
import com.example.userssdk.entities.AppointmentStatus;
import com.example.userssdk.entities.Role;
import com.example.userssdk.entities.User;
import com.example.userssdk.repositories.AppointmentRepository;
import com.example.userssdk.repositories.UserRepository;
import com.example.userssdk.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour of the {@link Appointment} entity, contrasted with what the semicolon-separated
 * string could not do.
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class AppointmentServiceTest {

    @Autowired private AppointmentService service;
    @Autowired private AppointmentRepository appointments;
    @Autowired private UserRepository users;

    private User user;
    private static final LocalDateTime TEN_AM = LocalDateTime.of(2026, 10, 1, 10, 0);

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setName("Barber Customer");
        u.setEmail("appt-" + System.nanoTime() + "@test.local");
        u.setPassword("irrelevant");
        u.setRole(Role.USER);
        user = users.saveAndFlush(u);
    }

    @Test
    @DisplayName("a booking is stored with its own identity, status and duration")
    void bookingIsAFirstClassRow() {
        Appointment a = service.book(user.getId(), TEN_AM, 45, "beard trim");

        assertNotNull(a.getId(), "the booking has its own primary key");
        assertEquals(AppointmentStatus.BOOKED, a.getStatus());
        assertEquals(45, a.getDurationMinutes());
        assertEquals("beard trim", a.getNote(), "per-appointment data the string format could not hold");
        assertEquals(TEN_AM.plusMinutes(45), a.getEndsAt());
    }

    @Test
    @DisplayName("double-booking the same slot is refused — the defect the string design allowed")
    void doubleBookingIsRefused() {
        service.book(user.getId(), TEN_AM, 30, null);

        assertThrows(AppointmentService.SlotUnavailableException.class,
                () -> service.book(user.getId(), TEN_AM, 30, null),
                "the same slot must not be bookable twice");
    }

    @Test
    @DisplayName("a partially overlapping slot is refused")
    void partialOverlapIsRefused() {
        service.book(user.getId(), TEN_AM, 60, null);          // 10:00–11:00

        assertThrows(AppointmentService.SlotUnavailableException.class,
                () -> service.book(user.getId(), TEN_AM.plusMinutes(30), 30, null),  // 10:30–11:00
                "10:30 falls inside the 10:00–11:00 booking");
    }

    @Test
    @DisplayName("back-to-back slots are allowed — intervals are half-open")
    void adjacentSlotsAreAllowed() {
        service.book(user.getId(), TEN_AM, 30, null);           // 10:00–10:30
        Appointment next = service.book(user.getId(), TEN_AM.plusMinutes(30), 30, null); // 10:30–11:00

        assertNotNull(next.getId(), "10:30 starts exactly when 10:00 ends, so it must be allowed");
    }

    @Test
    @DisplayName("cancelling frees the slot but keeps the record")
    void cancellingFreesTheSlotAndKeepsHistory() {
        Appointment first = service.book(user.getId(), TEN_AM, 30, null);
        service.cancel(first.getId());

        Appointment rebooked = service.book(user.getId(), TEN_AM, 30, null);
        assertNotNull(rebooked.getId(), "the slot is free once cancelled");

        assertTrue(appointments.findById(first.getId()).isPresent(),
                "the cancelled booking is retained — the string format could only delete");
        assertEquals(AppointmentStatus.CANCELLED,
                appointments.findById(first.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("appointments can be queried by date range without parsing strings")
    void queryByDateRange() {
        service.book(user.getId(), TEN_AM, 30, null);
        service.book(user.getId(), TEN_AM.plusDays(1), 30, null);

        List<Appointment> tuesdayOnly =
                service.between(TEN_AM.toLocalDate().atStartOfDay(),
                                TEN_AM.toLocalDate().plusDays(1).atStartOfDay());

        assertEquals(1, tuesdayOnly.size(), "only the bookings on that day — one indexed query");
    }

    @Test
    @DisplayName("no-shows are countable, which the string format made impossible")
    void noShowsAreCountable() {
        Appointment a = service.book(user.getId(), TEN_AM, 30, null);
        service.markStatus(a.getId(), AppointmentStatus.NO_SHOW);

        assertEquals(1, service.noShowCount(user.getId()));
    }

    @Test
    @DisplayName("invalid input is rejected")
    void invalidInputRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.book(user.getId(), null, 30, null));
        assertThrows(IllegalArgumentException.class, () -> service.book(user.getId(), TEN_AM, 0, null));
        assertThrows(IllegalArgumentException.class, () -> service.book(user.getId(), TEN_AM, -5, null));
        assertThrows(IllegalArgumentException.class, () -> service.book(999_999L, TEN_AM, 30, null));
    }
}
