package com.garbo.api.controller;

import com.garbo.api.dto.ApiResponse;
import com.garbo.core.entity.Bin;
import com.garbo.core.service.field_staff.BinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bins")
public class BinController {

    private final BinService binService;

    public BinController(BinService binService) {
        this.binService = binService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Bin>>> getBins(
            @RequestParam(value = "council", required = false) String council) {
        try {
            return ResponseEntity.ok(ApiResponse.success(binService.getBins(council)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage(), "FETCH_FAILED"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Bin>> createBin(@RequestBody Bin bin) {
        try {
            Bin createdBin = binService.createBinForCurrentUser(bin);
            return ResponseEntity.ok(ApiResponse.success(createdBin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "BIN_CREATE_VALIDATION"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
        }
    }

    @DeleteMapping("/{binId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteBin(@PathVariable Long binId) {
        try {
            binService.deleteBinForCurrentUser(binId);
            Map<String, Object> data = new HashMap<>();
            data.put("id", binId);
            data.put("deleted", true);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "DELETE_FAILED"));
        }
    }

    @PutMapping("/{binId}/priority")
    public ResponseEntity<ApiResponse<Bin>> updatePriority(
            @PathVariable Long binId,
            @RequestParam("priority") String priority) {
        try {
            Bin updated = binService.updatePriorityForCurrentUser(binId, priority);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "PRIORITY_UPDATE_FAILED"));
        }
    }

    @PutMapping("/{binId}/zone")
    public ResponseEntity<ApiResponse<Bin>> updateZone(
            @PathVariable Long binId,
            @RequestParam("zone") String zone) {
        try {
            Bin updated = binService.updateZoneForCurrentUser(binId, zone);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ZONE_UPDATE_FAILED"));
        }
    }
}
