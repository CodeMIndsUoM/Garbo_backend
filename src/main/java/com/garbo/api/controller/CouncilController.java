package com.garbo.api.controller;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.repository.CouncilRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/councils")
@CrossOrigin(origins = "*")
public class CouncilController {

    private final CouncilRepository councilRepository;

    public CouncilController(CouncilRepository councilRepository) {
        this.councilRepository = councilRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getActiveCouncils() {
        List<String> councils = councilRepository.findByIsActiveTrue().stream()
                .map(c -> c.getName())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(councils));
    }
}
