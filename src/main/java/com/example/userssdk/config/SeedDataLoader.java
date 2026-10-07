package com.example.userssdk.config;

import com.example.userssdk.entities.Role;
import com.example.userssdk.entities.User;
import com.example.userssdk.entities.UserCustomField;
import com.example.userssdk.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates demo users on startup so the credentials in the README work:
 * - admin@example.com / admin123 (ADMIN)
 * - mike@barber.com / barber123 (ADMIN) – second barber for Barber App
 * - alice@example.com, bob@example.com, charlie@example.com (USER, managed by admin)
 * - eve@barber.com, frank@barber.com (USER, managed by Mike)
 * Demo users get a sample "Appointment" custom field so they can see appointments in the app.
 *
 * Disabled with SEED_DEMO_DATA=false (app.seed-demo-data); do that on any public deployment.
 */
@Configuration
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class SeedDataLoader {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepo, PasswordEncoder passwordEncoder, SeedService seedService) {
        return args -> {
            // First barber (admin)
            User admin = userRepo.findByEmail("admin@example.com").orElse(null);
            if (admin == null) {
                admin = userRepo.save(User.builder()
                        .name("Admin")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .admin(null)
                        .build());
            }
            final User adminRef = admin;

            // Second barber (for Barber App)
            User mike = userRepo.findByEmail("mike@barber.com").orElse(null);
            if (mike == null) {
                mike = userRepo.save(User.builder()
                        .name("Mike Barber")
                        .email("mike@barber.com")
                        .password(passwordEncoder.encode("barber123"))
                        .role(Role.ADMIN)
                        .admin(null)
                        .build());
            }
            final User mikeRef = mike;

            // Customers of first admin
            for (String[] u : new String[][]{
                    {"Alice Smith", "alice@example.com", "secret"},
                    {"Bob Jones", "bob@example.com", "secret"},
                    {"Charlie Brown", "charlie@example.com", "secret"}
            }) {
                userRepo.findByEmail(u[1]).ifPresentOrElse(
                        existing -> {
                            existing.setPassword(passwordEncoder.encode(u[2]));
                            existing.setName(u[0]);
                            existing.setAdmin(adminRef);
                            userRepo.save(existing);
                        },
                        () -> userRepo.save(User.builder()
                                .name(u[0])
                                .email(u[1])
                                .password(passwordEncoder.encode(u[2]))
                                .role(Role.USER)
                                .admin(adminRef)
                                .build())
                );
            }

            // Customers of second barber (Mike)
            for (String[] u : new String[][]{
                    {"Eve Wilson", "eve@barber.com", "secret"},
                    {"Frank Lee", "frank@barber.com", "secret"}
            }) {
                userRepo.findByEmail(u[1]).ifPresentOrElse(
                        existing -> {
                            existing.setPassword(passwordEncoder.encode(u[2]));
                            existing.setName(u[0]);
                            existing.setAdmin(mikeRef);
                            userRepo.save(existing);
                        },
                        () -> userRepo.save(User.builder()
                                .name(u[0])
                                .email(u[1])
                                .password(passwordEncoder.encode(u[2]))
                                .role(Role.USER)
                                .admin(mikeRef)
                                .build())
                );
            }

            seedService.ensureAppointmentFieldsForDemoUsers();
        };
    }
}
