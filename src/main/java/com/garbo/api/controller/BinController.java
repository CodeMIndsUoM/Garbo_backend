package com.garbo.api.controller;

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
            System.err.println("❌ Error: " + e.getMessage());
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
        }
    }
}
