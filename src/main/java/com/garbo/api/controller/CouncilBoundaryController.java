package com.garbo.api.controller;

import com.garbo.api.dto.CouncilBoundaryDTO;
import com.garbo.core.service.CouncilBoundaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/council-boundaries")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CouncilBoundaryController {

    private final CouncilBoundaryService service;

    @GetMapping
    public ResponseEntity<?> getBoundary(
            @RequestParam(name = "council") String council) {
        CouncilBoundaryDTO dto = service.getBoundary(council);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}