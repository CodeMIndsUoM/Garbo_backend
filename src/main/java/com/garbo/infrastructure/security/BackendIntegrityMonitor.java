package com.garbo.infrastructure.security;

import com.garbo.common.logging.BackendFileAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class BackendIntegrityMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendIntegrityMonitor.class);

    private final BackendFileAuditLogger backendFileAuditLogger;
    private final boolean enabled;
    private final String monitoredPathsCsv;
    private final int maxFiles;
    private final Map<String, String> baselineHashes = new LinkedHashMap<>();
    private boolean baselineInitialized = false;

    public BackendIntegrityMonitor(
            BackendFileAuditLogger backendFileAuditLogger,
            @Value("${audit.integrity.enabled:true}") boolean enabled,
            @Value("${audit.integrity.monitored-paths:pom.xml,Dockerfile,src/main/resources/application.yml,src/main/resources/application-local.yml,src/main/resources/application-prod.yml,scripts}") String monitoredPathsCsv,
            @Value("${audit.integrity.max-files:1000}") int maxFiles) {
        this.backendFileAuditLogger = backendFileAuditLogger;
        this.enabled = enabled;
        this.monitoredPathsCsv = monitoredPathsCsv;
        this.maxFiles = Math.max(maxFiles, 100);
    }

    @Scheduled(fixedDelayString = "${audit.integrity.scan-interval-ms:300000}")
    public void scan() {
        if (!enabled) {
            return;
        }

        try {
            Map<String, String> current = collectCurrentHashes();
            if (!baselineInitialized) {
                baselineHashes.clear();
                baselineHashes.putAll(current);
                baselineInitialized = true;
                backendFileAuditLogger.logSecurityAlert(
                        "BACKEND_INTEGRITY_BASELINE_CREATED",
                        "integrity-monitor",
                        "Baseline created for " + current.size() + " files");
                return;
            }

            for (Map.Entry<String, String> entry : current.entrySet()) {
                String previousHash = baselineHashes.get(entry.getKey());
                if (previousHash == null) {
                    backendFileAuditLogger.logSecurityAlert(
                            "BACKEND_INTEGRITY_NEW_FILE",
                            entry.getKey(),
                            "New monitored file detected");
                } else if (!previousHash.equals(entry.getValue())) {
                    backendFileAuditLogger.logSecurityAlert(
                            "BACKEND_INTEGRITY_FILE_CHANGED",
                            entry.getKey(),
                            "Monitored file hash changed");
                }
            }

            for (String previousPath : new ArrayList<>(baselineHashes.keySet())) {
                if (!current.containsKey(previousPath)) {
                    backendFileAuditLogger.logSecurityAlert(
                            "BACKEND_INTEGRITY_FILE_DELETED",
                            previousPath,
                            "Previously monitored file is missing");
                }
            }

            baselineHashes.clear();
            baselineHashes.putAll(current);
        } catch (Exception ex) {
            LOGGER.warn("Backend integrity monitor scan failed", ex);
            backendFileAuditLogger.logSecurityAlert(
                    "BACKEND_INTEGRITY_MONITOR_FAILURE",
                    "integrity-monitor",
                    "Integrity scan failed: " + ex.getClass().getSimpleName());
        }
    }

    private Map<String, String> collectCurrentHashes() {
        Map<String, String> hashes = new LinkedHashMap<>();
        int[] counter = new int[] { 0 };

        for (String raw : monitoredPathsCsv.split(",")) {
            String normalized = raw.trim();
            if (normalized.isEmpty()) {
                continue;
            }

            Path root = Path.of(normalized).normalize();
            if (!Files.exists(root)) {
                continue;
            }

            if (Files.isDirectory(root)) {
                collectDirectoryHashes(root, hashes, counter);
            } else if (Files.isRegularFile(root)) {
                collectFileHash(root, hashes, counter);
            }

            if (counter[0] >= maxFiles) {
                break;
            }
        }

        return hashes;
    }

    private void collectDirectoryHashes(Path dir, Map<String, String> hashes, int[] counter) {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> collectFileHash(path, hashes, counter));
        } catch (IOException ex) {
            LOGGER.debug("Failed to scan directory {}", dir, ex);
        }
    }

    private void collectFileHash(Path file, Map<String, String> hashes, int[] counter) {
        if (counter[0] >= maxFiles) {
            return;
        }
        try {
            hashes.put(file.toString().replace('\\', '/'), sha256(Files.readAllBytes(file)));
            counter[0]++;
        } catch (IOException ex) {
            LOGGER.debug("Failed to hash monitored file {}", file, ex);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "sha256-error";
        }
    }
}
