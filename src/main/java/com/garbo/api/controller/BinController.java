package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.service.BinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bins")
public class BinController {

    private final BinService binService;

    public BinController(BinService binService) {
        this.binService = binService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Bin>> createBin(@RequestBody Bin bin) {
        try {
            Bin createdBin = binService.createBin(bin);
            return ResponseEntity.ok(ApiResponse.success(createdBin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "BIN_EXISTS"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
        }
    }

    @PostMapping("/{binId}/report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportBinStatus(
            @PathVariable Long binId,
            @RequestBody BinReportRequest request) {
        
        try {
            // Pass null for reporterId as this is a generic/anonymous report
            Bin updatedBin = binService.reportBinStatus(binId, null, request);
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", updatedBin.getId());
            data.put("status", updatedBin.getStatus());
            data.put("fillLevel", updatedBin.getFillLevel());
            data.put("lastChecked", updatedBin.getLastChecked());

            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "REPORT_FAILED"));
        }
    }
}
