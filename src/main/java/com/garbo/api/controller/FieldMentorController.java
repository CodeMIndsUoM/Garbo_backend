package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.service.BinService;
import com.garbo.core.service.FieldMentorService;
<<<<<<< HEAD
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
=======

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
>>>>>>> kevin-RWS

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

// Field staff (field mentor) flow:
//   Flutter screens under presentation/field_staff hit these endpoints to
//   list a mentor's assigned bins and submit bin status reports with photo.
@RestController
@RequestMapping("/api/fieldmentors")
public class FieldMentorController {
<<<<<<< HEAD

    final private FieldMentorService fieldMentorService;
    final private BinService binService;
    final private CloudinaryUploadService cloudinaryUploadService;

    public FieldMentorController(FieldMentorService fieldMentorService, BinService binService, CloudinaryUploadService cloudinaryUploadService) {
        this.fieldMentorService = fieldMentorService;
        this.binService = binService;
        this.cloudinaryUploadService = cloudinaryUploadService;
    }
=======
    @Autowired
    private FieldMentorService fieldMentorService;
>>>>>>> kevin-RWS

    @PostMapping
    public ResponseEntity<?> createFieldMentor(@RequestBody FieldMentor fieldMentor) {
        try {
            String role = currentUserService.getCurrentRole().orElse("");
            // Only admin may create FieldMentor
            if (!role.equals("admin")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Only admin can create field mentors"));
            }

            // For admin, require admin council and force assignment
            java.util.Optional<String> councilOpt = currentUserService.getCurrentCouncil();
            if (councilOpt.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "message", "Admin council not found"));
            }

            String adminCouncil = councilOpt.get();
            fieldMentor.setAssignedCouncil(adminCouncil);

            FieldMentor saved = fieldMentorService.saveFieldMentor(fieldMentor);
            return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to create field mentor"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFieldMentors() {
        try {
            String role = currentUserService.getCurrentRole().orElse("");
            if (role.equals("superadmin")) {
                java.util.List<FieldMentor> all = fieldMentorService.getAll();
                return ResponseEntity.ok().body(Map.of("success", true, "data", all));
            } else if (role.equals("admin")) {
                java.util.Optional<String> councilOpt = currentUserService.getCurrentCouncil();
                if (councilOpt.isEmpty()) {
                    return ResponseEntity.status(400).body(Map.of(
                            "success", false,
                            "message", "Admin council not found"));
                }
                String council = councilOpt.get();
                java.util.List<FieldMentor> byCouncil = fieldMentorService.findByCouncil(council);
                return ResponseEntity.ok().body(Map.of("success", true, "data", byCouncil));
            } else {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Forbidden"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to fetch field mentors"));
        }
    }

    @GetMapping("/{empId}")
    public ResponseEntity<ApiResponse<FieldMentor>> getFieldMentor(@PathVariable Long empId) {
        try {
            FieldMentor fieldMentor = fieldMentorService.getFieldMentor(empId);
            return ResponseEntity.ok(ApiResponse.success(fieldMentor));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage(), "USER_NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch field mentor", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/{empId}/bins")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignedBins(@PathVariable Long empId) {
        List<Bin> bins = binService.getAssignedBins(empId);

        List<Map<String, Object>> allowedBins = bins.stream().map(bin -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bin.getId());

            String fullLocation = bin.getLocation() != null ? bin.getLocation() : "Unknown";
            // Split "Galle Road, Colombo 03" into location="Galle Road" and
            // address="Colombo 03"
            String locationName = fullLocation;
            String addressName = fullLocation;
            if (fullLocation.contains(",")) {
                int commaIdx = fullLocation.indexOf(",");
                locationName = fullLocation.substring(0, commaIdx).trim();
                addressName = fullLocation.substring(commaIdx + 1).trim();
            }
            map.put("location", locationName);
            map.put("address", addressName);

            map.put("category", bin.getCategory() != null ? bin.getCategory() : "public");
            map.put("status", bin.getStatus() != null ? bin.getStatus() : "notChecked");
            map.put("fillLevel", bin.getFillLevel());
            map.put("lastChecked", bin.getLastChecked());
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(allowedBins));
    }

    @PostMapping(value = "/{empId}/bins/{binId}/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportBinStatus(
            @PathVariable Long empId,
            @PathVariable Long binId,
            @RequestParam("status") String status,
            @RequestParam("fillLevel") Integer fillLevel,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        try {
            BinReportRequest request = new BinReportRequest();
            request.setStatus(status);
            request.setFillLevel(fillLevel);
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setNotes(notes);

            if (photo != null && !photo.isEmpty()) {
                String photoUrl = cloudinaryUploadService.uploadBinReportPhoto(photo, binId);
                request.setPhotoUrl(photoUrl);
            }

            Bin updatedBin = binService.reportBinStatus(binId, empId, request);

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
