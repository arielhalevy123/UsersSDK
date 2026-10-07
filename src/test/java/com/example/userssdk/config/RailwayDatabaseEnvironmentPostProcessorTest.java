package com.example.userssdk.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RailwayDatabaseEnvironmentPostProcessorTest {

    private static Map<String, Object> resolve(Map<String, String> env) {
        return RailwayDatabaseEnvironmentPostProcessor.resolve(env::get);
    }

    @Test
    void convertsDatabaseUrlToJdbc() {
        Map<String, Object> p = resolve(Map.of(
                "DATABASE_URL", "postgresql://pguser:p%40ss+word@postgres.railway.internal:5432/railway"));
        assertEquals("jdbc:postgresql://postgres.railway.internal:5432/railway", p.get("spring.datasource.url"));
        assertEquals("pguser", p.get("spring.datasource.username"));
        assertEquals("p@ss+word", p.get("spring.datasource.password"));
    }

    @Test
    void keepsQueryParametersAndDefaultsPort() {
        Map<String, Object> p = resolve(Map.of(
                "DATABASE_URL", "postgres://u:pw@db.example.com/app?sslmode=require"));
        assertEquals("jdbc:postgresql://db.example.com:5432/app?sslmode=require", p.get("spring.datasource.url"));
    }

    @Test
    void acceptsJdbcDatabaseUrlAsIs() {
        Map<String, Object> p = resolve(Map.of("DATABASE_URL", "jdbc:postgresql://h:5432/d"));
        assertEquals("jdbc:postgresql://h:5432/d", p.get("spring.datasource.url"));
        assertFalse(p.containsKey("spring.datasource.username"));
    }

    @Test
    void buildsFromPgVariables() {
        Map<String, Object> p = resolve(Map.of(
                "PGHOST", "postgres.railway.internal", "PGPORT", "6543",
                "PGUSER", "postgres", "PGPASSWORD", "pw", "PGDATABASE", "railway"));
        assertEquals("jdbc:postgresql://postgres.railway.internal:6543/railway", p.get("spring.datasource.url"));
        assertEquals("postgres", p.get("spring.datasource.username"));
        assertEquals("pw", p.get("spring.datasource.password"));
    }

    @Test
    void explicitDbUrlWins() {
        Map<String, String> env = new HashMap<>();
        env.put("DB_URL", "jdbc:postgresql://localhost:5432/usersdb");
        env.put("DATABASE_URL", "postgresql://u:p@other:5432/x");
        assertTrue(resolve(env).isEmpty());
    }

    @Test
    void nothingSetMeansNoOverride() {
        assertTrue(resolve(Map.of()).isEmpty());
    }

    @Test
    void rejectsNonPostgresScheme() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> resolve(Map.of("DATABASE_URL", "mysql://u:secretpw@h/d")));
        assertFalse(e.getMessage().contains("secretpw"), "error must not leak the password");
    }
}
