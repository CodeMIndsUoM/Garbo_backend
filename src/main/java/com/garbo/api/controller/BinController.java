package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.entity.Bin;
import com.garbo.core.service.field_staff.BinService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bins")
public class BinController {

    private final BinService binService;
    private final CloudinaryUploadService cloudinaryUploadService;

    public BinController(BinService binService, CloudinaryUploadService cloudinaryUploadService) {
        this.binService = binService;
        this.cloudinaryUploadService = cloudinaryUploadService;
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

    // Primary field mentor report endpoint used by Flutter (multipart + optional photo).
    @PostMapping(value = "/{binId}/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FIELD_MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportBinStatusFromFieldMentor(
            @PathVariable Long binId,
            @RequestParam("status") String status,
            @RequestParam("fillLevel") Integer fillLevel,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        try {
            Long reporterId = CurrentUserService.getCurrentEmpId()
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

            com.garbo.core.dto.BinReportRequest request = new com.garbo.core.dto.BinReportRequest();
            request.setStatus(status);
            request.setFillLevel(fillLevel);
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setNotes(notes);

            if (photo != null && !photo.isEmpty()) {
                String photoUrl = cloudinaryUploadService.uploadBinReportPhoto(photo, binId);
                request.setPhotoUrl(photoUrl);
            }

            Bin updatedBin = binService.reportBinStatus(binId, reporterId, request);

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

    // Dedicated undo endpoint used by Flutter to revert a report without resubmitting payload.
    @PostMapping("/{binId}/undo")
    @PreAuthorize("hasRole('FIELD_MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> undoBinReportFromFieldMentor(@PathVariable Long binId) {
        try {
            Long reporterId = CurrentUserService.getCurrentEmpId()
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

            Bin updatedBin = binService.undoBinReport(binId, reporterId);

            Map<String, Object> data = new HashMap<>();
            data.put("id", updatedBin.getId());
            data.put("status", updatedBin.getStatus());
            data.put("fillLevel", updatedBin.getFillLevel());
            data.put("lastChecked", updatedBin.getLastChecked());

            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "UNDO_FAILED"));
        }
    }
}
