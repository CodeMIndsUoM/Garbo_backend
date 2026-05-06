package com.garbo.api.controller;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.entity.CollectorLabour;
import com.garbo.core.service.CollectorLabourService;
import com.garbo.core.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collector-labours")
@CrossOrigin("*")
@RequiredArgsConstructor
public class CollectorLabourController {

    private final CollectorLabourService labourService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectorLabour>>> getAll() {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (role.equals("superadmin")) {
            return ResponseEntity.ok(ApiResponse.success(labourService.getAll()));
        } else {
            String council = CurrentUserService.getCurrentCouncil().orElse("Unassigned");
            return ResponseEntity.ok(ApiResponse.success(labourService.findByCouncil(council)));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectorLabour>> create(@RequestBody CollectorLabour labour) {
        String council = CurrentUserService.getCurrentCouncil().orElse("Unassigned");
        labour.setCouncil(council);
        return ResponseEntity.ok(ApiResponse.success(labourService.save(labour)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        labourService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
