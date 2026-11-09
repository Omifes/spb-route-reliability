package ru.spb.transit.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Один встроенный PostgreSQL на весь тестовый прогон.
 * Позволяет проверять настоящие миграции и JPA-маппинг без Docker.
 */
public final class EmbeddedPostgresSupport {

    private static final EmbeddedPostgres POSTGRES = start();

    private EmbeddedPostgresSupport() {
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setLocaleConfig("locale", "C")
                    .setLocaleConfig("encoding", "UTF8")
                    .setServerConfig("timezone", "Europe/Moscow")
                    .start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                    // JVM завершается, отдельная обработка не нужна
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось запустить встроенный PostgreSQL", e);
        }
    }
}
