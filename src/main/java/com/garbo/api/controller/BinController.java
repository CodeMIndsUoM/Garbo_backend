package com.garbo.api.controller;

<<<<<<< HEAD
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
=======
import com.garbo.api.dto.BinDTO;
import com.garbo.api.mapper.BinMapper;
import com.garbo.core.entity.Bin;
import com.garbo.core.service.BinService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bins")
@CrossOrigin("*")
public class BinController {

    @Autowired
    private BinService service;

    @GetMapping
    public ResponseEntity<List<BinDTO>> getAll() {
        try {
            List<BinDTO> bins = service.getAllBins()
                    .stream()
                    .map(BinMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(bins);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BinDTO dto) {
        try {
            System.out.println("📥 Received: lat=" + dto.getLat() + ", lng=" + dto.getLng() + ", fill=" + dto.getFillLevel() + ", priority=" + dto.getPriority());
            
            Bin saved = service.addBin(dto);
            
            System.out.println("✅ Saved with ID: " + saved.getId());
            
            BinDTO response = BinMapper.toDTO(saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.deleteBin(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/priority")
    public ResponseEntity<?> updatePriority(
            @PathVariable Long id,
            @RequestParam String priority
    ) {
        try {
            service.updatePriority(id, priority);
            return ResponseEntity.ok("{\"message\": \"Priority updated\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/zone")
    public ResponseEntity<?> updateZone(
            @PathVariable Long id,
            @RequestParam String zone
    ) {
        try {
            service.updateZone(id, zone);
            return ResponseEntity.ok("{\"message\": \"Zone updated\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
>>>>>>> kevin-RWS
        }
    }
}
