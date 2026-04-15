package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.service.BinService;
import com.garbo.core.service.FieldMentorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fieldmentors")
public class FieldMentorController {

    final private FieldMentorService fieldMentorService;
    final private BinService binService;

    public FieldMentorController(FieldMentorService fieldMentorService, BinService binService) {
        this.fieldMentorService = fieldMentorService;
        this.binService = binService;
    }

    @PostMapping
    public void createFieldMentor(@RequestBody FieldMentor fieldMentor) {
        System.out.println("admin saved successfully");
        fieldMentorService.saveFieldMentor(fieldMentor);
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

    @PostMapping("/{empId}/bins/{binId}/report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportBinStatus(
            @PathVariable Long empId,
            @PathVariable String binId,
            @RequestBody BinReportRequest request) {

        try {
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
