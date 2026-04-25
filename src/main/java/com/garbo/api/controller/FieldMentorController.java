package com.garbo.api.controller;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.service.FieldMentorService;
import com.garbo.core.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/fieldmentors")
public class FieldMentorController {

    final private FieldMentorService fieldMentorService;
    final private CurrentUserService currentUserService;

    public FieldMentorController(FieldMentorService fieldMentorService, CurrentUserService currentUserService) {
        this.fieldMentorService = fieldMentorService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<?> createFieldMentor(@RequestBody FieldMentor fieldMentor) {
        try {
            String role = currentUserService.getCurrentRole().orElse("");
            // Only admin may create FieldMentor
            if (!role.equals("admin")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Only admin can create field mentors"));
            }

            // For admin, require admin council and force assignment
            java.util.Optional<String> councilOpt = currentUserService.getCurrentCouncil();
            if (councilOpt.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "message", "Admin council not found"));
            }

            String adminCouncil = councilOpt.get();
            fieldMentor.setAssignedCouncil(adminCouncil);

            FieldMentor saved = fieldMentorService.saveFieldMentor(fieldMentor);
            return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to create field mentor"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFieldMentors() {
        try {
            String role = currentUserService.getCurrentRole().orElse("");
            if (role.equals("superadmin")) {
                java.util.List<FieldMentor> all = fieldMentorService.getAll();
                return ResponseEntity.ok().body(Map.of("success", true, "data", all));
            } else if (role.equals("admin")) {
                java.util.Optional<String> councilOpt = currentUserService.getCurrentCouncil();
                if (councilOpt.isEmpty()) {
                    return ResponseEntity.status(400).body(Map.of(
                            "success", false,
                            "message", "Admin council not found"));
                }
                String council = councilOpt.get();
                java.util.List<FieldMentor> byCouncil = fieldMentorService.findByCouncil(council);
                return ResponseEntity.ok().body(Map.of("success", true, "data", byCouncil));
            } else {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Forbidden"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to fetch field mentors"));
        }
    }
}
