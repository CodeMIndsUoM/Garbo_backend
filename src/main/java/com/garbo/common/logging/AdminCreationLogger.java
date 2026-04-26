package com.garbo.common.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminCreationLogger {

    private static final Path LOG_PATH = Paths.get("logs", "admin_creations.log");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AdminCreationLogger() {
        // utility
    }

    public static void log(String email, String tempPassword) {
        try {
            Path parent = LOG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String ts = LocalDateTime.now().format(FMT);
            StringBuilder sb = new StringBuilder();
            sb.append("=========================").append(System.lineSeparator());
            sb.append("New Admin Created").append(System.lineSeparator());
            sb.append("Email: ").append(email == null ? "" : email).append(System.lineSeparator());
            sb.append("Temp Password: ").append(tempPassword == null ? "" : tempPassword)
                    .append(System.lineSeparator());
            sb.append("Created At: ").append(ts).append(System.lineSeparator());
            sb.append("=========================").append(System.lineSeparator());

            try (BufferedWriter w = Files.newBufferedWriter(LOG_PATH, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(sb.toString());
            }
        } catch (IOException ignored) {
            // Intentionally swallow errors in dev-only utility to avoid affecting business
            // logic
        }
    }
}
