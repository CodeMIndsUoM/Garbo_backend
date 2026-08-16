package com.garbo.api.controller;

import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.security.BackendAuditReadService;
import com.garbo.core.service.security.FileIntegrityMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
@CrossOrigin(origins = "*")
public class AdminAuditController {

    private final BackendAuditReadService backendAuditReadService;
    private final CouncilAccessService councilAccessService;
    private final FileIntegrityMonitor fileIntegrityMonitor;

    public AdminAuditController(
            BackendAuditReadService backendAuditReadService,
            CouncilAccessService councilAccessService,
            FileIntegrityMonitor fileIntegrityMonitor) {
        this.backendAuditReadService = backendAuditReadService;
        this.councilAccessService = councilAccessService;
        this.fileIntegrityMonitor = fileIntegrityMonitor;
    }

    @GetMapping("/file-changes")
    public ResponseEntity<Map<String, Object>> getRecentFileChangeAudits(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        enforceGovernanceAccess();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("limit", Math.min(Math.max(limit, 1), 500));
            body.put("entries", backendAuditReadService.getRecentEntries(limit));
            return ResponseEntity.ok(body);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read audit log");
        }
    }

    @GetMapping("/file-changes/integrity")
    public ResponseEntity<Map<String, Object>> getAuditLogIntegrityReport() {
        enforceGovernanceAccess();
        try {
            return ResponseEntity.ok(backendAuditReadService.verifyActiveLogIntegrity());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify audit log");
        }
    }

    @GetMapping("/fim/status")
    public ResponseEntity<Map<String, Object>> getFileIntegrityStatus() {
        enforceGovernanceAccess();
        return ResponseEntity.ok(fileIntegrityMonitor.getIntegrityStatus());
    }

    private void enforceGovernanceAccess() {
        String email = CurrentUserService.getCurrentEmail()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        if (!councilAccessService.isAdmin(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Superadmin role required");
        }
    }
}
