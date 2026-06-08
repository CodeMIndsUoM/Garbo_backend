package com.garbo.api.controller;

import com.garbo.api.dto.EventCreateRequest;
import com.garbo.core.entity.Event;
import com.garbo.core.service.EventService;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;
    private final JwtUtil jwtUtil;
    private final CloudinaryUploadService cloudinaryUploadService;

    public EventController(EventService eventService, JwtUtil jwtUtil, CloudinaryUploadService cloudinaryUploadService) {
        this.eventService = eventService;
        this.jwtUtil = jwtUtil;
        this.cloudinaryUploadService = cloudinaryUploadService;
    }

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

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadEventImage(
            @RequestParam("photo") MultipartFile photo,
            HttpServletRequest httpRequest) {
        resolveRequesterEmail(httpRequest);
        String imageUrl = cloudinaryUploadService.uploadEventImage(photo);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody EventCreateRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(eventService.createEvent(request, resolveRequesterEmail(httpRequest)));
    }

    @PostMapping("/suggestions")
    public ResponseEntity<Event> suggestEvent(@RequestBody EventCreateRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(eventService.suggestEvent(request, resolveRequesterEmail(httpRequest)));
    }

    @GetMapping
    public ResponseEntity<List<Event>> getVisibleEvents(HttpServletRequest request) {
        return ResponseEntity.ok(eventService.getVisibleEvents(resolveRequesterEmail(request)));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Event>> getMyEvents(HttpServletRequest request) {
        return ResponseEntity.ok(eventService.getMyEvents(resolveRequesterEmail(request)));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<Event>> getPendingSuggestions(HttpServletRequest request) {
        return ResponseEntity.ok(eventService.getPendingSuggestions(resolveRequesterEmail(request)));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<Event> enroll(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(eventService.enrollInEvent(id, resolveRequesterEmail(httpRequest)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Event> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        return ResponseEntity.ok(eventService.updateEventStatus(id, body.get("status"), resolveRequesterEmail(request)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Event> approveSuggestion(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(eventService.approveSuggestion(id, resolveRequesterEmail(request)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Event> rejectSuggestion(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(eventService.rejectSuggestion(id, reason, resolveRequesterEmail(request)));
    }
}
