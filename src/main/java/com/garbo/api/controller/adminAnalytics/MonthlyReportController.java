package com.garbo.api.controller.adminAnalytics;

import com.garbo.api.dto.reportDTOs.MonthlyReportDetailDTO;
import com.garbo.api.dto.reportDTOs.MonthlyReportSummaryDTO;
import com.garbo.core.service.AdminAnalytics.MonthlyReportGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportGeneratorService reportService;

    // ── POST /api/admin/reports/generate ─────────────────────────────────────
    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(
            @RequestParam(required = false) String councilId) {
        try {
            MonthlyReportSummaryDTO result = reportService.generateReport(councilId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Report generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error",
                "cause",   e.getCause() != null ? e.getCause().getMessage() : "No cause"
            ));
        }
    }

    // ── GET /api/admin/reports ────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAllReports() {
        try {
            List<MonthlyReportSummaryDTO> reports = reportService.getAllReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Failed to fetch reports list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    // ── GET /api/admin/reports/{id} ───────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(@PathVariable Long id) {
        try {
            MonthlyReportDetailDTO report = reportService.getReportById(id);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Report not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error",   "NotFound",
                    "message", e.getMessage()
                ));
            }
            log.error("Failed to fetch report id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    // ── GET /api/admin/reports/{id}/download ──────────────────────────────────
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadReport(@PathVariable Long id) {
        try {
            String json = reportService.getRawSnapshot(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "garbo-report-" + id + ".json");
            return ResponseEntity.ok().headers(headers).body(json);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Report not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error",   "NotFound",
                    "message", e.getMessage()
                ));
            }
            log.error("Failed to download report id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }
}