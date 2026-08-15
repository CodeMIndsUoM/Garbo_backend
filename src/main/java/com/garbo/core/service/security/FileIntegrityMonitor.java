package com.garbo.core.service.security;

import com.garbo.common.logging.BackendFileAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileIntegrityMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileIntegrityMonitor.class);

    private final BackendFileAuditLogger auditLogger;
    private final BackendAuditReadService auditReadService;

    // Map of monitored file path string -> baseline SHA-256 hash string
    private final Map<String, String> baselineHashes = new ConcurrentHashMap<>();
    
    // List of static configuration files to monitor if they exist
    private final List<Path> criticalStaticFiles = new ArrayList<>();

    // Keep track of the current status of all monitored files
    private final Map<String, FileStatus> currentStatuses = new ConcurrentHashMap<>();

    public FileIntegrityMonitor(
            BackendFileAuditLogger auditLogger,
            BackendAuditReadService auditReadService) {
        this.auditLogger = auditLogger;
        this.auditReadService = auditReadService;

        // Populate baseline static paths
        criticalStaticFiles.add(Path.of("src/main/resources/application.properties"));
        criticalStaticFiles.add(Path.of("src/main/resources/application.yml"));
        criticalStaticFiles.add(Path.of("src/main/resources/application-local.yml"));
        criticalStaticFiles.add(Path.of("src/main/resources/application-prod.yml"));
    }

    @PostConstruct
    public void initBaseline() {
        LOGGER.info("Initializing File Integrity Monitor baseline...");
        
        // 1. Baseline static config files
        for (Path file : criticalStaticFiles) {
            if (Files.exists(file)) {
                String hash = computeFileHash(file);
                if (hash != null) {
                    baselineHashes.put(file.toString(), hash);
                    currentStatuses.put(file.toString(), new FileStatus(file.toString(), true, hash, null));
                    LOGGER.info("FIM baseline registered: {} (SHA-256: {})", file, hash);
                }
            } else {
                LOGGER.debug("FIM baseline skipped (file does not exist): {}", file);
            }
        }

        // 2. Perform initial validation scan
        runIntegrityCheck();
    }

    @Scheduled(fixedRate = 30000) // Run scan every 30 seconds
    public void runIntegrityCheck() {
        LOGGER.debug("FIM scanning filesystem integrity...");

        // 1. Verify static configurations
        for (Map.Entry<String, String> entry : baselineHashes.entrySet()) {
            Path file = Path.of(entry.getKey());
            String expectedHash = entry.getValue();

            if (!Files.exists(file)) {
                currentStatuses.put(entry.getKey(), new FileStatus(entry.getKey(), false, null, "FILE_DELETED"));
                LOGGER.error("FIM SECURITY ALERT: Critical file has been deleted: {}", file);
            } else {
                String currentHash = computeFileHash(file);
                if (currentHash == null) {
                    currentStatuses.put(entry.getKey(), new FileStatus(entry.getKey(), false, null, "HASH_FAILED"));
                } else if (!currentHash.equals(expectedHash)) {
                    currentStatuses.put(entry.getKey(), new FileStatus(entry.getKey(), false, currentHash, "FILE_MODIFIED"));
                    LOGGER.error("FIM SECURITY ALERT: Critical file tampered/modified: {}", file);
                } else {
                    currentStatuses.put(entry.getKey(), new FileStatus(entry.getKey(), true, currentHash, null));
                }
            }
        }

        // 2. Verify dynamic audit log integrity
        Path auditLogPath = auditLogger.getAuditLogPath();
        String auditLogKey = "audit-log:" + auditLogPath.toString();

        if (!Files.exists(auditLogPath)) {
            // Severe security threat: audit log deleted
            currentStatuses.put(auditLogKey, new FileStatus(auditLogPath.toString(), false, null, "AUDIT_LOG_DELETED"));
            LOGGER.error("FIM SECURITY ALERT: Audit log file has been deleted: {}", auditLogPath);
        } else {
            try {
                Map<String, Object> integrityReport = auditReadService.verifyActiveLogIntegrity();
                boolean ok = Boolean.TRUE.equals(integrityReport.get("ok"));
                @SuppressWarnings("unchecked")
                List<String> issues = (List<String>) integrityReport.getOrDefault("issues", Collections.emptyList());

                if (!ok) {
                    String detail = "AUDIT_LOG_TAMPERED: " + String.join("; ", issues);
                    currentStatuses.put(auditLogKey, new FileStatus(auditLogPath.toString(), false, null, detail));
                    LOGGER.error("FIM SECURITY ALERT: Audit log tamper detected: {}", detail);
                } else {
                    currentStatuses.put(auditLogKey, new FileStatus(auditLogPath.toString(), true, null, null));
                }
            } catch (IOException e) {
                currentStatuses.put(auditLogKey, new FileStatus(auditLogPath.toString(), false, null, "INTEGRITY_READ_ERROR: " + e.getMessage()));
                LOGGER.warn("Failed to perform FIM audit log verification", e);
            }
        }
    }

    public Map<String, Object> getIntegrityStatus() {
        Map<String, Object> statusMap = new LinkedHashMap<>();
        List<Map<String, Object>> filesList = new ArrayList<>();
        boolean systemHealthy = true;

        for (Map.Entry<String, FileStatus> entry : currentStatuses.entrySet()) {
            FileStatus status = entry.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("filePath", status.filePath);
            item.put("healthy", status.healthy);
            item.put("currentHash", status.currentHash);
            item.put("issue", status.issue);
            filesList.add(item);

            if (!status.healthy) {
                systemHealthy = false;
            }
        }

        statusMap.put("healthy", systemHealthy);
        statusMap.put("monitoredFiles", filesList);
        return statusMap;
    }

    private String computeFileHash(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("FIM failed to hash file: {}", file, e);
            return null;
        }
    }

    private static class FileStatus {
        final String filePath;
        final boolean healthy;
        final String currentHash;
        final String issue;

        FileStatus(String filePath, boolean healthy, String currentHash, String issue) {
            this.filePath = filePath;
            this.healthy = healthy;
            this.currentHash = currentHash;
            this.issue = issue;
        }
    }
}
