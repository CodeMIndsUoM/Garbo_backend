package com.garbo.common.logging;

import com.garbo.core.service.security.BackendAuditReadService;
import com.garbo.core.service.security.FileIntegrityMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FileIntegrityMonitorTest {

    @TempDir
    Path tempDir;

    private BackendFileAuditLogger auditLogger;
    private BackendAuditReadService auditReadService;
    private Path tempAuditLog;

    @BeforeEach
    void setUp() throws IOException {
        tempAuditLog = tempDir.resolve("backend_file_audit.log");
        Files.writeString(tempAuditLog, "ts=2026-08-16T00:00:00Z|event=START|actor=system|ip=127.0.0.1|request=N/A|target=N/A|outcome=SUCCESS|detail=Init|prevHash=GENESIS|sig=dummy-sig|entryHash=dummy-hash\n", StandardCharsets.UTF_8);

        auditLogger = Mockito.mock(BackendFileAuditLogger.class);
        when(auditLogger.getAuditLogPath()).thenReturn(tempAuditLog);

        auditReadService = Mockito.mock(BackendAuditReadService.class);
    }

    @Test
    void reportsHealthyWhenLogsAndIntegrityAreValid() throws IOException {
        Map<String, Object> integrityReport = new HashMap<>();
        integrityReport.put("ok", true);
        integrityReport.put("issues", List.of());
        when(auditReadService.verifyActiveLogIntegrity()).thenReturn(integrityReport);

        FileIntegrityMonitor monitor = new FileIntegrityMonitor(auditLogger, auditReadService);
        monitor.initBaseline();
        monitor.runIntegrityCheck();

        Map<String, Object> status = monitor.getIntegrityStatus();
        assertTrue((Boolean) status.get("healthy"));
    }

    @Test
    void reportsUnhealthyWhenAuditLogIsDeleted() throws IOException {
        Map<String, Object> integrityReport = new HashMap<>();
        integrityReport.put("ok", true);
        when(auditReadService.verifyActiveLogIntegrity()).thenReturn(integrityReport);

        FileIntegrityMonitor monitor = new FileIntegrityMonitor(auditLogger, auditReadService);
        monitor.initBaseline();

        // Delete the audit log file
        Files.delete(tempAuditLog);

        monitor.runIntegrityCheck();

        Map<String, Object> status = monitor.getIntegrityStatus();
        assertFalse((Boolean) status.get("healthy"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) status.get("monitoredFiles");
        boolean foundDeleted = false;
        for (Map<String, Object> f : files) {
            if ("AUDIT_LOG_DELETED".equals(f.get("issue"))) {
                foundDeleted = true;
            }
        }
        assertTrue(foundDeleted);
    }

    @Test
    void reportsUnhealthyWhenAuditLogIsTampered() throws IOException {
        Map<String, Object> integrityReport = new HashMap<>();
        integrityReport.put("ok", false);
        integrityReport.put("issues", List.of("Line 1 signature mismatch"));
        when(auditReadService.verifyActiveLogIntegrity()).thenReturn(integrityReport);

        FileIntegrityMonitor monitor = new FileIntegrityMonitor(auditLogger, auditReadService);
        monitor.initBaseline();
        monitor.runIntegrityCheck();

        Map<String, Object> status = monitor.getIntegrityStatus();
        assertFalse((Boolean) status.get("healthy"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) status.get("monitoredFiles");
        boolean foundTampered = false;
        for (Map<String, Object> f : files) {
            String issue = (String) f.get("issue");
            if (issue != null && issue.contains("AUDIT_LOG_TAMPERED")) {
                foundTampered = true;
            }
        }
        assertTrue(foundTampered);
    }
}
