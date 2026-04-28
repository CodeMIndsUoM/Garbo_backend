package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.service.BinService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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

    @PostMapping(value = "/{binId}/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FIELD_MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportBinStatusFromFieldStaff(
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

    @PostMapping("/{binId}/undo")
    @PreAuthorize("hasRole('FIELD_MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> undoBinReportFromFieldStaff(@PathVariable Long binId) {
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
