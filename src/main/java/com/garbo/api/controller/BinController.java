package com.garbo.api.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garbo.core.entity.WasteBin;
import com.garbo.core.service.WasteBinService;

@RestController
@RequestMapping("/api/bins")
@CrossOrigin(origins = "*")
public class BinController {

    private final WasteBinService wasteBinService;

    public BinController(WasteBinService wasteBinService) {
        this.wasteBinService = wasteBinService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(Map.of("success", true, "data", wasteBinService.getAll()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WasteBin payload) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", wasteBinService.create(payload)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            wasteBinService.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Bin deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
