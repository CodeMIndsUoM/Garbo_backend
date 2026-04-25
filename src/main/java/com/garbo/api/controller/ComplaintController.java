package com.garbo.api.controller;

import com.garbo.core.entity.Complaint;
import com.garbo.core.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<Complaint> createComplaint(@RequestBody Complaint complaint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(complaintService.createComplaint(complaint, email));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Complaint>> getMyComplaints() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(complaintService.getComplaintsByCitizen(email));
    }

    @GetMapping
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Complaint> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(complaintService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<Complaint> assignComplaint(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long personnelId = body.get("personnelId");
        return ResponseEntity.ok(complaintService.assignComplaint(id, personnelId));
    }
}
