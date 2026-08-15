package com.garbo.api.controller;

import com.garbo.api.dto.ComplaintCreateRequest;
import com.garbo.common.logging.BackendFileAuditLogger;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.core.entity.Complaint;
import com.garbo.core.service.ComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BackendFileAuditLogger backendFileAuditLogger;

    private String resolveRequesterEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            return auth.getName();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractUsername(authHeader.substring(7));
            } catch (Exception ignored) {
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token");
    }

    @PostMapping
    public ResponseEntity<Complaint> createComplaint(
            @RequestBody ComplaintCreateRequest request,
            HttpServletRequest httpRequest) {
        String email = resolveRequesterEmail(httpRequest);
        return ResponseEntity.ok(complaintService.createComplaint(request, email));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Complaint>> getMyComplaints(HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(complaintService.getComplaintsByCitizen(email));
    }

    @GetMapping
    public ResponseEntity<List<Complaint>> getAllComplaints(HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(complaintService.getAllComplaintsForRequester(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getComplaintById(@PathVariable Long id, HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(complaintService.getComplaintById(id, email));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String email = resolveRequesterEmail(request);
            String status = body.get("status");
            String resolutionNotes = body.get("resolutionNotes");
            Complaint updated = complaintService.updateStatus(id, status, resolutionNotes, email);
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<Complaint> assignComplaint(@PathVariable Long id, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        Long personnelId = body.get("personnelId");
        return ResponseEntity.ok(complaintService.assignComplaint(id, personnelId, email));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadComplaintImage(@RequestParam("photo") MultipartFile photo) {
        try {
            String original = StringUtils.cleanPath(photo.getOriginalFilename() == null ? "complaint.jpg" : photo.getOriginalFilename());
            String extension = "";
            int idx = original.lastIndexOf('.');
            if (idx > -1) {
                extension = original.substring(idx);
            }
            String fileName = "complaint-" + UUID.randomUUID() + extension;
            Path uploadDir = Path.of("uploads", "complaints");
                Path target = uploadDir.resolve(fileName);
                backendFileAuditLogger.logFileModificationAttempt(
                    "BACKEND_FILE_CHANGE_ATTEMPT",
                    target.toString(),
                    "ATTEMPT",
                    "Attempting to store complaint image");
            Files.createDirectories(uploadDir);
            Files.copy(photo.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                backendFileAuditLogger.logFileModificationAttempt(
                    "BACKEND_FILE_CHANGE_ATTEMPT",
                    target.toString(),
                    "SUCCESS",
                    "Complaint image stored");
            return ResponseEntity.ok(Map.of(
                    "photoUrl", target.toString(),
                    "message", "Image uploaded"));
        } catch (Exception e) {
                backendFileAuditLogger.logFileModificationAttempt(
                    "BACKEND_FILE_CHANGE_ATTEMPT",
                    "uploads/complaints",
                    "FAILED",
                    "Complaint image storage failed: " + e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload image"));
        }
    }
}
