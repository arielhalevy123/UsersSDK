package com.example.userssdk.repositories;

import com.example.userssdk.entities.Appointment;
import com.example.userssdk.entities.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Queries that were impossible while appointments lived in a semicolon-separated string.
 *
 * Each of these previously required loading every row and parsing text in Java. They are now
 * index-backed SQL — see the indexes declared on {@link Appointment}.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrderByStartsAtAsc(Long userId);

    List<Appointment> findByUserIdAndStatusOrderByStartsAtAsc(Long userId, AppointmentStatus status);

    /** Every booking in a window — "show me Tuesday", the query the old design could not answer. */
    List<Appointment> findByStartsAtBetweenOrderByStartsAtAsc(LocalDateTime from, LocalDateTime to);

    /**
     * Candidate appointments for overlap checking.
     *
     * Deliberately selects by start time only, over a widened window, and leaves the exact
     * interval comparison to {@link Appointment#overlaps}. Expressing duration arithmetic in JPQL
     * ties the query to one database's interval syntax; keeping it in Java stays portable and is
     * unit-testable without a database.
     *
     * Cancelled appointments are excluded — a cancelled booking frees its slot.
     */
    @Query("""
           select a from Appointment a
           where a.user.id = :userId
             and a.status <> com.example.userssdk.entities.AppointmentStatus.CANCELLED
             and a.startsAt >= :windowStart
             and a.startsAt < :windowEnd
           """)
    List<Appointment> findCandidatesForOverlap(@Param("userId") Long userId,
                                               @Param("windowStart") LocalDateTime windowStart,
                                               @Param("windowEnd") LocalDateTime windowEnd);

    long countByUserIdAndStatus(Long userId, AppointmentStatus status);
}
