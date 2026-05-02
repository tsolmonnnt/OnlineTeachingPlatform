package com.tsolmon.online_teaching_platform;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads {@code .env} into JVM system properties before Spring starts so
 * {@code application.yaml} placeholders like {@code ${CLOUDINARY_CLOUD_NAME}} resolve.
 * Existing OS environment variables win over {@code .env}.
 */
final class DotEnvBootstrap {

    private DotEnvBootstrap() {
    }

    static void load() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        // Cover: module dir, parent `backend`, repo root (see README IntelliJ working directory).
        Path[] candidates = new Path[]{
                cwd.resolve(".env"),
                cwd.resolve("online_teaching_platform/.env"),
                cwd.resolve("backend/online_teaching_platform/.env"),
        };

        for (Path envFile : candidates) {
            if (!Files.isRegularFile(envFile)) {
                continue;
            }
            Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent().toString())
                    .filename(envFile.getFileName().toString())
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(e -> {
                String key = e.getKey();
                if (key == null || key.isBlank()) {
                    return;
                }
                if (System.getenv(key) != null) {
                    return;
                }
                if (System.getProperty(key) != null) {
                    return;
                }
                String value = e.getValue();
                if (value != null) {
                    System.setProperty(key, value);
                }
            });
            return;
        }
    }
}
