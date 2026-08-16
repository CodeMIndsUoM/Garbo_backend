package com.garbo.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class BackendFileAuditLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendFileAuditLogger.class);
    private static final Logger ALERT_LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT_ALERT");
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path auditLogPath;
    private final Path alertLogPath;
    private final long maxBytes;
    private final int maxArchives;
    private final int retentionDays;
    private final String hmacSecret;
    private String lastEntryHash;

    public BackendFileAuditLogger(
            @Value("${audit.file-change.log-path:logs/backend_file_audit.log}") String auditLogPath,
            @Value("${audit.file-change.alert-log-path:logs/backend_file_audit_alerts.log}") String alertLogPath,
            @Value("${audit.file-change.max-bytes:5242880}") long maxBytes,
            @Value("${audit.file-change.max-archives:10}") int maxArchives,
            @Value("${audit.file-change.retention-days:30}") int retentionDays,
            @Value("${audit.file-change.hmac-secret:change-me-in-prod}") String hmacSecret) {
        this.auditLogPath = Path.of(auditLogPath);
        this.alertLogPath = Path.of(alertLogPath);
        this.maxBytes = Math.max(maxBytes, 1024L);
        this.maxArchives = Math.max(maxArchives, 1);
        this.retentionDays = Math.max(retentionDays, 1);
        this.hmacSecret = hmacSecret;
        this.lastEntryHash = readLastEntryHash();
    }

    public void logFileModificationAttempt(
            String event,
            String targetPath,
            String outcome,
            String detail) {
        writeEvent(event, targetPath, outcome, detail);
        if ("FAILED".equalsIgnoreCase(outcome)) {
            writeAlert("FILE_MODIFICATION_FAILURE", targetPath, detail);
        }
    }

    public void logSecurityAlert(String event, String targetPath, String detail) {
        writeEvent(event, targetPath, "ALERT", detail);
        writeAlert(event, targetPath, detail);
    }

    public Path getAuditLogPath() {
        return auditLogPath;
    }

    private void writeEvent(String event, String targetPath, String outcome, String detail) {
        String timestamp = OffsetDateTime.now().format(TS_FORMATTER);
        String actor = resolveActor();
        String ipAddress = resolveIpAddress();
        String request = resolveRequestSummary();
        String prevHash = lastEntryHash == null ? "GENESIS" : lastEntryHash;
        String canonical = String.format(
                "ts=%s|event=%s|actor=%s|ip=%s|request=%s|target=%s|outcome=%s|detail=%s|prevHash=%s",
                sanitize(timestamp),
                sanitize(event),
                sanitize(actor),
                sanitize(ipAddress),
                sanitize(request),
                sanitize(targetPath),
                sanitize(outcome),
                sanitize(detail),
                sanitize(prevHash));
        String signature = computeHmac(canonical);
        String entryHash = sha256(canonical + "|sig=" + signature);
        String line = canonical + "|sig=" + signature + "|entryHash=" + entryHash + System.lineSeparator();

        try {
            synchronized (this) {
                ensureParent(auditLogPath);
                ensureParent(alertLogPath);
                rotateIfNeeded();
                pruneOldArchives();
                try (BufferedWriter writer = Files.newBufferedWriter(
                        auditLogPath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {
                    writer.write(line);
                }
                hardenFilePermissions(auditLogPath);
                lastEntryHash = entryHash;
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to write backend file audit log entry", ex);
        }
    }

    private void writeAlert(String event, String targetPath, String detail) {
        String timestamp = OffsetDateTime.now().format(TS_FORMATTER);
        String line = String.format(
                "ts=%s|event=%s|target=%s|detail=%s%s",
                sanitize(timestamp),
                sanitize(event),
                sanitize(targetPath),
                sanitize(detail),
                System.lineSeparator());
        ALERT_LOGGER.warn("{} {} {}", event, targetPath, detail);
        try {
            synchronized (this) {
                ensureParent(alertLogPath);
                try (BufferedWriter writer = Files.newBufferedWriter(
                        alertLogPath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {
                    writer.write(line);
                }
                hardenFilePermissions(alertLogPath);
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to write backend audit alert entry", ex);
        }
    }

    private void rotateIfNeeded() throws IOException {
        if (!Files.exists(auditLogPath)) {
            return;
        }
        long size = Files.size(auditLogPath);
        if (size < maxBytes) {
            return;
        }
        for (int i = maxArchives - 1; i >= 1; i--) {
            Path src = archivePath(i);
            if (Files.exists(src)) {
                Path dest = archivePath(i + 1);
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.move(auditLogPath, archivePath(1), StandardCopyOption.REPLACE_EXISTING);
        lastEntryHash = readLastEntryHash();
    }

    private void pruneOldArchives() throws IOException {
        Path parent = auditLogPath.getParent();
        if (parent == null || !Files.exists(parent)) {
            return;
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        String fileNamePrefix = auditLogPath.getFileName().toString() + ".";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, auditLogPath.getFileName() + ".*")) {
            for (Path entry : stream) {
                if (!entry.getFileName().toString().startsWith(fileNamePrefix)) {
                    continue;
                }
                int suffix = parseArchiveSuffix(entry.getFileName().toString(), fileNamePrefix);
                if (suffix > maxArchives) {
                    Files.deleteIfExists(entry);
                    continue;
                }
                OffsetDateTime modified = OffsetDateTime.ofInstant(
                        Files.getLastModifiedTime(entry).toInstant(),
                        OffsetDateTime.now().getOffset());
                if (modified.isBefore(cutoff)) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }

    private int parseArchiveSuffix(String fileName, String prefix) {
        try {
            return Integer.parseInt(fileName.substring(prefix.length()));
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private Path archivePath(int suffix) {
        return Path.of(auditLogPath.toString() + "." + suffix);
    }

    private void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void hardenFilePermissions(Path path) {
        try {
            var file = path.toFile();
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setExecutable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
        } catch (Exception ex) {
            LOGGER.debug("Permission hardening skipped for {}", path, ex);
        }
    }

    private String readLastEntryHash() {
        if (!Files.exists(auditLogPath)) {
            return null;
        }
        try (Stream<String> stream = Files.lines(auditLogPath, StandardCharsets.UTF_8)) {
            String lastLine = stream.filter(line -> !line.isBlank()).reduce((first, second) -> second).orElse(null);
            if (lastLine == null) {
                return null;
            }
            int idx = lastLine.lastIndexOf("|entryHash=");
            if (idx < 0) {
                return null;
            }
            return lastLine.substring(idx + "|entryHash=".length()).trim();
        } catch (IOException ex) {
            LOGGER.debug("Could not read previous audit hash", ex);
            return null;
        }
    }

    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            LOGGER.warn("Failed to compute audit entry HMAC", ex);
            return "hmac-error";
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "sha256-error";
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "unknown";
        }
        String name = authentication.getName();
        if (name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return "anonymous";
        }
        return name;
    }

    private String resolveIpAddress() {
        HttpServletRequest request = resolveRequest();
        if (request == null) {
            return "n/a";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveRequestSummary() {
        HttpServletRequest request = resolveRequest();
        if (request == null) {
            return "n/a";
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    private HttpServletRequest resolveRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("|", "/")
                .replace("\r", " ")
                .replace("\n", " ");
    }
}