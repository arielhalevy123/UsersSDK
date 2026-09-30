package com.example.userssdk.config;

import com.example.userssdk.entities.Role;
import com.example.userssdk.entities.User;
import com.example.userssdk.entities.UserCustomField;
import com.example.userssdk.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedService {

    private static final String SAMPLE_APPOINTMENTS = "2025-02-15 10:00; 2025-02-16 14:00";

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void ensureAppointmentFieldsForDemoUsers() {
        for (String email : new String[]{
                "alice@example.com", "bob@example.com", "charlie@example.com",
                "eve@barber.com", "frank@barber.com"
        }) {
            userRepo.findByEmail(email).ifPresent(user -> {
                User fresh = userRepo.findById(user.getId()).orElse(null);
                if (fresh == null) return;
                List<UserCustomField> fields = fresh.getCustomFields();
                if (fields == null) {
                    fields = new ArrayList<>();
                    fresh.setCustomFields(fields);
                }
                boolean hasAppointment = fields.stream()
                        .anyMatch(f -> "Appointment".equalsIgnoreCase(f.getFieldName()));
                if (!hasAppointment) {
                    fields.add(new UserCustomField("Appointment", SAMPLE_APPOINTMENTS, fresh));
                    userRepo.save(fresh);
                }
            });
        }
    }
}
