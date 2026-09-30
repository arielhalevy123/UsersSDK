package com.example.userssdk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs against the in-memory H2 database defined in {@code application-test.properties}.
 *
 * Previously this test used the production datasource from {@code application.properties}, so it
 * failed on any machine without a local PostgreSQL instance holding a {@code usersdb} database —
 * including a clean checkout. Binding it to the test profile makes the suite self-contained.
 */
@SpringBootTest
@ActiveProfiles("test")
class UsersSdkApplicationTests {

    @Test
    void contextLoads() {
    }

}
