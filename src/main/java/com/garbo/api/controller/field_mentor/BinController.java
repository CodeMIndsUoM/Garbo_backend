package com.garbo.api.controller.field_mentor;

import com.garbo.api.dto.BinLatestReportDTO;
import com.garbo.api.dto.BinReportRequest;
import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.entity.Bin;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.field_staff.BinReportPhotoService;
import com.garbo.core.service.field_staff.BinService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Bin endpoints used in the scoped field mentor flow.
// Main mobile usage:
//   - POST /api/bins/{binId}/report (multipart) from field mentor app
//   - POST /api/bins/{binId}/undo from field mentor app
@RestController("fieldMentorBinController")
@RequestMapping("/api/bins")
public class BinController {

    private final BinService binService;
    private final BinReportPhotoService binReportPhotoService;

    public BinController(BinService binService, BinReportPhotoService binReportPhotoService) {
        this.binService = binService;
        this.binReportPhotoService = binReportPhotoService;
    }

    @GetMapping("/{binId}/latest-report")
    public ResponseEntity<ApiResponse<BinLatestReportDTO>> getLatestReport(@PathVariable Long binId) {
        try {
            BinLatestReportDTO report = binService.getLatestReport(binId);
            if (report == null) {
                return ResponseEntity.ok(ApiResponse.success(null));
            }
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "FETCH_FAILED"));
        }
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

    @DeleteMapping("/{binId:[0-9]+}")
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

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteBins(@RequestBody List<Long> binIds) {
        try {
            binService.deleteBinsForCurrentUser(binIds);
            Map<String, Object> data = new HashMap<>();
            data.put("ids", binIds);
            data.put("deleted", true);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "BATCH_DELETE_FAILED"));
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

            BinReportRequest request = new BinReportRequest();
            request.setStatus(status);
            request.setFillLevel(fillLevel);
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setNotes(notes);

            byte[] photoBytes = null;
            String photoFilename = null;
            String photoContentType = null;
            if (photo != null && !photo.isEmpty()) {
                photoBytes = photo.getBytes();
                photoFilename = photo.getOriginalFilename();
                photoContentType = photo.getContentType();
            }

            BinService.BinStatusReportResult result = binService.reportBinStatus(binId, reporterId, request);
            Bin updatedBin = result.bin();

            if (photoBytes != null) {
                binReportPhotoService.uploadAndAttachAsync(
                        result.reportId(),
                        binId,
                        photoBytes,
                        photoFilename,
                        photoContentType);
            }

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
