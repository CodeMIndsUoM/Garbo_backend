package com.garbo.api.controller;

import com.garbo.api.dto.BinSuggestionCreateRequest;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.service.BinSuggestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.garbo.infrastructure.storage.CloudinaryUploadService;

@RestController
@RequestMapping("/api/bin-suggestions")
@CrossOrigin(origins = "*")
public class BinSuggestionController {

    @Autowired
    private BinSuggestionService binSuggestionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CloudinaryUploadService cloudinaryUploadService;

    private String resolveRequesterEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            return auth.getName();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractUsername(authHeader.substring(7));
            } catch (Exception ignored) {
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token");
    }

    @PostMapping
    public ResponseEntity<BinSuggestion> createSuggestion(
            @RequestBody BinSuggestionCreateRequest request,
            HttpServletRequest httpRequest) {
        String email = resolveRequesterEmail(httpRequest);
        return ResponseEntity.ok(binSuggestionService.createSuggestion(request, email));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BinSuggestion>> getMySuggestions(HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(binSuggestionService.getMySuggestions(email));
    }

    @GetMapping
    public ResponseEntity<List<BinSuggestion>> getAllSuggestions(HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(binSuggestionService.getAllForRequester(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BinSuggestion> getSuggestionById(@PathVariable Long id, HttpServletRequest request) {
        String email = resolveRequesterEmail(request);
        return ResponseEntity.ok(binSuggestionService.getById(id, email));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String email = resolveRequesterEmail(request);
            String status = body.get("status");
            String resolutionNotes = body.get("resolutionNotes");
            BinSuggestion updated = binSuggestionService.updateStatus(id, status, resolutionNotes, email);
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadSuggestionImage(@RequestParam("photo") MultipartFile photo) {
        String url = cloudinaryUploadService.uploadBinSuggestionPhoto(photo);
        return ResponseEntity.ok(Map.of(
                "photoUrl", url,
                "imageUrl", url,
                "message", "Image uploaded"));
    }
}
