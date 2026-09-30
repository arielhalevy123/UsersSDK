package com.example.userssdk;

import com.example.userssdk.entities.Role;
import com.example.userssdk.entities.User;
import com.example.userssdk.entities.UserCustomField;
import com.example.userssdk.repositories.UserCustomFieldRepository;
import com.example.userssdk.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Appointments are stored as a semicolon-separated string inside a single
 * {@link UserCustomField} row (README: "no dedicated appointment entity").
 *
 * Booking is therefore a read-modify-write of one shared string. Two clients booking at the same
 * moment both read the same value, each appends its own slot, and whichever writes second
 * overwrites the other's booking.
 *
 * For a barbershop queue that is the primary use case, not an edge case: two customers booking
 * 11:00 at the same time.
 *
 * MEASURED BEFORE THE FIX — with no {@code @Version} on UserCustomField, this test suite ran and
 * the stored value was:
 *
 *     2026-10-01 10:00;2026-10-01 12:00
 *
 * The 11:00 booking was destroyed with no error raised. {@code @Version} was then added and the
 * same race now fails loudly instead, which is what these tests assert.
 */
@DataJpaTest
@ActiveProfiles("test")
class AppointmentConcurrencyTest {

    private static final String FIELD = "Appointment";

    @Autowired private UserRepository users;
    @Autowired private UserCustomFieldRepository fields;
    @Autowired private EntityManager em;

    private User persistUser() {
        User u = new User();
        u.setName("Barber Customer");
        u.setEmail("concurrency-" + System.nanoTime() + "@test.local");
        u.setPassword("irrelevant");
        u.setRole(Role.USER);
        return users.saveAndFlush(u);
    }

    /** Appends one slot to the semicolon-separated list, exactly as the booking path does. */
    private static String append(String current, String slot) {
        return (current == null || current.isBlank()) ? slot : current + ";" + slot;
    }

    @Test
    @DisplayName("two clients booking the same slot concurrently: the conflict is detected, not silently lost")
    void concurrentBooking_isDetected() {
        User user = persistUser();
        Long id = fields.saveAndFlush(new UserCustomField(FIELD, "2026-10-01 10:00", user)).getId();

        // Two requests arrive together. Each loads the row before either has written.
        em.clear();
        UserCustomField clientA = fields.findById(id).orElseThrow();
        em.detach(clientA);

        em.clear();
        UserCustomField clientB = fields.findById(id).orElseThrow();
        em.detach(clientB);

        assertEquals(clientA.getFieldValue(), clientB.getFieldValue(),
                "both clients start from the same state");

        // Client A books 11:00 and commits first.
        clientA.setFieldValue(append(clientA.getFieldValue(), "2026-10-01 11:00"));
        fields.saveAndFlush(clientA);

        // Client B commits second, still holding the row as it was before A wrote.
        clientB.setFieldValue(append(clientB.getFieldValue(), "2026-10-01 12:00"));

        assertThrows(Exception.class, () -> fields.saveAndFlush(clientB),
                "B's stale write must be rejected — without @Version it would overwrite A's booking");

        // A's booking survives, because B was stopped rather than allowed to overwrite.
        em.clear();
        String stored = fields.findById(id).orElseThrow().getFieldValue();
        assertTrue(stored.contains("2026-10-01 11:00"), "A's booking survived the conflict");

        System.out.println("[concurrent booking] stored value after conflict = " + stored);
    }

    @Test
    @DisplayName("UserCustomField carries an optimistic-locking version column")
    void entityIsVersioned() throws Exception {
        assertNotNull(UserCustomField.class.getDeclaredField("version"),
                "appointments live in a shared mutable string and require optimistic locking");
    }

    @Test
    @DisplayName("sequential bookings still accumulate normally")
    void sequentialBookings_accumulate() {
        User user = persistUser();
        Long id = fields.saveAndFlush(new UserCustomField(FIELD, "2026-10-01 10:00", user)).getId();

        for (String slot : new String[]{"2026-10-01 11:00", "2026-10-01 12:00"}) {
            em.clear();
            UserCustomField f = fields.findById(id).orElseThrow();
            f.setFieldValue(append(f.getFieldValue(), slot));
            fields.saveAndFlush(f);
        }

        em.clear();
        String stored = fields.findById(id).orElseThrow().getFieldValue();
        assertEquals("2026-10-01 10:00;2026-10-01 11:00;2026-10-01 12:00", stored,
                "locking must not break the normal one-at-a-time path");
    }
}
