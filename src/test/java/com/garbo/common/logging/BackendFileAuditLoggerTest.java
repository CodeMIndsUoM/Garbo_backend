package com.garbo.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendFileAuditLoggerTest {

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void writesAuditLineWithActorAndRequestContext() throws Exception {
        Path tempDir = Files.createTempDirectory("backend-audit-test");
        Path logFile = tempDir.resolve("backend_file_audit.log");
        BackendFileAuditLogger logger = new BackendFileAuditLogger(
                logFile.toString(),
                tempDir.resolve("backend_file_audit_alerts.log").toString(),
                5242880L,
                10,
                30,
                "change-me-in-prod"
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor@garbo.local", "n/a")
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/complaints/upload-image");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        logger.logFileModificationAttempt(
                "BACKEND_FILE_CHANGE_ATTEMPT",
                "uploads/complaints/test.jpg",
                "SUCCESS",
                "Stored complaint image"
        );

        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("event=BACKEND_FILE_CHANGE_ATTEMPT"));
        assertTrue(content.contains("actor=auditor@garbo.local"));
        assertTrue(content.contains("request=POST /api/complaints/upload-image"));
        assertTrue(content.contains("target=uploads/complaints/test.jpg"));
        assertTrue(content.contains("outcome=SUCCESS"));
    }
}
