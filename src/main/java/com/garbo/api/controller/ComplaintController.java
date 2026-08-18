package com.garbo.api.controller;

import com.garbo.api.dto.ComplaintCreateRequest;
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

import com.garbo.infrastructure.storage.CloudinaryUploadService;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CloudinaryUploadService cloudinaryUploadService;

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

    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<Complaint>> getAssignedComplaints(HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(complaintService.getAssignedComplaints(email));
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

    @PostMapping("/bulk-assign")
    public ResponseEntity<Map<String, String>> bulkAssignComplaints(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        
        List<Integer> complaintIdsInt = (List<Integer>) body.get("complaintIds");
        if (complaintIdsInt == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "complaintIds is required"));
        }
        
        List<Long> complaintIds = complaintIdsInt.stream().map(Integer::longValue).toList();
        
        Number personnelIdNum = (Number) body.get("personnelId");
        if (personnelIdNum == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "personnelId is required"));
        }
        Long personnelId = personnelIdNum.longValue();
        
        complaintService.bulkAssignComplaints(complaintIds, personnelId, email);
        return ResponseEntity.ok(Map.of("message", "Complaints successfully assigned"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Complaint> confirmComplaint(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        Boolean isTrue = (Boolean) body.get("isTrue");
        String note = (String) body.get("note");
        String photoUrl = (String) body.get("photoUrl");
        
        return ResponseEntity.ok(complaintService.confirmComplaint(id, isTrue, note, photoUrl, email));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadComplaintImage(@RequestParam("photo") MultipartFile photo) {
        String url = cloudinaryUploadService.uploadComplaintPhoto(photo);
        return ResponseEntity.ok(Map.of(
                "photoUrl", url,
                "imageUrl", url,
                "message", "Image uploaded"));
    }

    @PostMapping("/add-to-route")
    public ResponseEntity<?> addToRoute(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("complaintIds");
        if (!(idsObj instanceof List)) {
            return ResponseEntity.badRequest().body(Map.of("error", "complaintIds are required"));
        }
        
        List<?> rawIds = (List<?>) idsObj;
        List<Long> complaintIds = rawIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();

        if (complaintIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "complaintIds are required"));
        }
        try {
            complaintService.addToRoute(complaintIds);
            return ResponseEntity.ok(Map.of("message", "Complaints added to route successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
