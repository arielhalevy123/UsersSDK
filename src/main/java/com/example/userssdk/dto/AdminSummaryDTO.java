package com.example.userssdk.dto;

/**
 * Public view of an admin, for "pick your admin" screens before the user has an account.
 * Deliberately only id and display name: no email, no custom fields, no appointments.
 */
public record AdminSummaryDTO(Long id, String name) {
}
