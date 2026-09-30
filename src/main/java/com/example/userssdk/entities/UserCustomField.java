package com.example.userssdk.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_custom_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCustomField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fieldName;
    private String fieldValue;

    /**
     * Optimistic-locking version.
     *
     * Appointments are stored as a semicolon-separated list in {@link #fieldValue}, so booking is a
     * read-modify-write of one shared string. Without this column two clients booking at the same
     * moment both read the same value and the second write silently erases the first booking — see
     * {@code AppointmentConcurrencyTest#lostUpdate_whenTwoClientsBookConcurrently}, which reproduces it.
     *
     * With it, the stale write fails with an optimistic-locking exception the caller can retry.
     * A detectable conflict is recoverable; silent data loss is not.
     */
    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    public UserCustomField(String fieldName, String fieldValue, User user) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.user = user;
    }
}