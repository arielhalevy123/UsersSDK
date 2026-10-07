package com.example.userssdk.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Maps the PostgreSQL variables that Railway (and Heroku-style hosts) provide onto Spring's
 * datasource properties.
 *
 * Spring needs a {@code jdbc:postgresql://host:port/db} URL with the credentials passed separately,
 * whereas Railway exposes either {@code DATABASE_URL=postgresql://user:pass@host:port/db} or the
 * individual {@code PGHOST/PGPORT/PGUSER/PGPASSWORD/PGDATABASE} variables.
 *
 * Order of precedence:
 * <ol>
 *   <li>{@code SPRING_DATASOURCE_URL} or {@code DB_URL} set explicitly: nothing is changed.</li>
 *   <li>{@code DATABASE_URL}: parsed and converted to a jdbc URL plus username/password.</li>
 *   <li>{@code PGHOST} (with the other PG* variables): assembled into a jdbc URL.</li>
 *   <li>none of the above: the defaults in {@code application.properties} apply (local dev).</li>
 * </ol>
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "railwayDatabase";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        Map<String, Object> props = resolve(env::getProperty);
        if (!props.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
        }
    }

    /** Pure function of the variable lookup, so it can be unit tested without Spring. */
    static Map<String, Object> resolve(Function<String, String> var) {
        Map<String, Object> props = new HashMap<>();
        if (notBlank(var.apply("SPRING_DATASOURCE_URL")) || notBlank(var.apply("DB_URL"))) {
            return props;
        }

        String databaseUrl = var.apply("DATABASE_URL");
        if (notBlank(databaseUrl)) {
            fromDatabaseUrl(databaseUrl.trim(), props);
            return props;
        }

        String host = var.apply("PGHOST");
        if (notBlank(host)) {
            String port = orDefault(var.apply("PGPORT"), "5432");
            String db = orDefault(var.apply("PGDATABASE"), "railway");
            props.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + db);
            putIfPresent(props, "spring.datasource.username", var.apply("PGUSER"));
            putIfPresent(props, "spring.datasource.password", var.apply("PGPASSWORD"));
        }
        return props;
    }

    private static void fromDatabaseUrl(String databaseUrl, Map<String, Object> props) {
        if (databaseUrl.startsWith("jdbc:")) {
            props.put("spring.datasource.url", databaseUrl);
            return;
        }
        URI uri;
        try {
            uri = URI.create(databaseUrl);
        } catch (IllegalArgumentException e) {
            // Never echo the value: it contains the database password.
            throw new IllegalStateException("DATABASE_URL is not a valid URL", e);
        }
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalStateException("DATABASE_URL must start with postgresql:// or postgres://");
        }
        if (uri.getHost() == null) {
            throw new IllegalStateException("DATABASE_URL has no host");
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        jdbc.append(':').append(uri.getPort() > 0 ? uri.getPort() : 5432);
        jdbc.append(uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbc.append('?').append(uri.getRawQuery());
        }
        props.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            putIfPresent(props, "spring.datasource.username", decode(user));
            if (colon >= 0) {
                props.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
    }

    private static String decode(String s) {
        // URLDecoder treats '+' as a space; in a URL's user-info it is a literal plus.
        return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String orDefault(String s, String def) {
        return notBlank(s) ? s.trim() : def;
    }

    private static void putIfPresent(Map<String, Object> props, String key, String value) {
        if (notBlank(value)) props.put(key, value);
    }
}
