package com.garbo.api.controller;

import com.garbo.core.entity.BinCollector;
import com.garbo.core.service.BinCollectorService;
import com.garbo.core.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bincollectors")
public class BinCollectorController {
    private final BinCollectorService binCollectorService;
    private final CurrentUserService currentUserService;

    public BinCollectorController(BinCollectorService binCollectorService, CurrentUserService currentUserService) {
        this.binCollectorService = binCollectorService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<?> createBinCollector(@RequestBody BinCollector binCollector) {
        try {
            String role = currentUserService.getCurrentRole().orElse("");
            // Only admin may create BinCollector
            if (!role.equals("admin")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Only admin can create bin collectors"));
            }

            // For admin, require admin council and force assignment
            java.util.Optional<String> councilOpt = currentUserService.getCurrentCouncil();
            if (councilOpt.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "message", "Admin council not found"));
            }

            String adminCouncil = councilOpt.get();
            binCollector.setAssignedCouncil(adminCouncil);

            BinCollector saved = binCollectorService.saveBinCollector(binCollector);
            return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to create bin collector"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            String role = currentUserService.getCurrentRole().orElse("");
            if (role.equals("superadmin")) {
                List<BinCollector> all = binCollectorService.getAll();
                return ResponseEntity.ok().body(Map.of("success", true, "data", all));
            } else if (role.equals("admin")) {
                java.util.Optional<String> councilOpt = currentUserService.getCurrentCouncil();
                List<BinCollector> byCouncil = councilOpt.map(binCollectorService::findByCouncil).orElse(List.of());
                return ResponseEntity.ok().body(Map.of("success", true, "data", byCouncil));
            } else {
                // other roles are forbidden
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Forbidden"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to fetch bin collectors"));
        }
    }
}
