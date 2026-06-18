package com.garbo.core.service.AdminAnalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.ZoneCollectionDTO;
import com.garbo.api.dto.binAnalyzeDTOs.BinAnalyticsResponseDTO;
import com.garbo.api.dto.binAnalyzeDTOs.ZoneAnalyticsDTO;
import com.garbo.api.dto.reportDTOs.MonthlyReportDetailDTO;
import com.garbo.api.dto.reportDTOs.MonthlyReportSummaryDTO;
import com.garbo.api.dto.reportDTOs.ReportSnapshotPayload;
import com.garbo.core.entity.MonthlyReport;
import com.garbo.core.repository.MonthlyReportRepository;
import com.garbo.core.service.AnalyticsService;
import com.garbo.core.service.AdminAnalytics.ZoneCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyReportGeneratorService {

    // ── Analytics services ────────────────────────────────────────────────────
    private final AnalyticsService           analyticsService;
    private final BinAnalyticsService        binAnalyticsService;
    private final ComplaintAnalyticsService  complaintAnalyticsService;
    private final StaffAnalyticsService      staffAnalyticsService;
    private final ThirdPartyAnalyticsService thirdPartyAnalyticsService;
    private final VehicleAnalyticsService    vehicleAnalyticsService;
    private final ZoneCollectionService      zoneCollectionService;

    // ── Persistence ───────────────────────────────────────────────────────────
    private final MonthlyReportRepository reportRepository;
    private final ObjectMapper            objectMapper;

    // ── GENERATE ──────────────────────────────────────────────────────────────

    @Transactional
    public MonthlyReportSummaryDTO generateReport(String councilId) {

        boolean filtered = councilId != null && !councilId.isBlank();

        LocalDate today       = LocalDate.now();
        LocalDate periodStart = today.minusDays(30);
        String periodLabel    = buildPeriodLabel(periodStart, today);
        String title          = filtered
            ? "Monthly Report — " + councilId + " — " + periodLabel
            : "Monthly Report — " + periodLabel;

        log.info("Generating report: {} ({} → {}) council={}",
                 title, periodStart, today, councilId);

        // ── 1. Call analytics services ────────────────────────────────────────
        var collection   = analyticsService.getDashboard("MONTH", filtered ? councilId : null);
        var binAnalytics = binAnalyticsService.getAnalytics(filtered ? councilId : null);
        var complaints   = complaintAnalyticsService.getAnalytics("MONTH", filtered ? councilId : null);
        var staff        = staffAnalyticsService.getAnalytics(filtered ? councilId : null);
        var thirdParty   = thirdPartyAnalyticsService.getAnalytics("LAST_MONTH", filtered ? councilId : null);
        var vehicles     = vehicleAnalyticsService.getAnalytics(filtered ? councilId : null);

        // ── 2. Zone collection (monthly) ──────────────────────────────────────
        List<ZoneCollectionDTO> zoneCollection = zoneCollectionService.getZoneCollection(
            "MONTHLY", filtered ? councilId : null
        );

        // ── 3. Build extra snapshots ──────────────────────────────────────────
        Map<String, Integer> binZoneSnapshot     = buildBinZoneSnapshot(binAnalytics);
        Map<String, Long>    vehicleTypeSnapshot = buildVehicleTypeSnapshot(vehicles.getVehicles());

        // ── 4. Assemble payload ───────────────────────────────────────────────
        ReportSnapshotPayload payload = ReportSnapshotPayload.builder()
                .collection(collection)
                .zoneCollection(zoneCollection)
                .binAnalytics(binAnalytics)
                .complaints(complaints)
                .staff(staff)
                .thirdParty(thirdParty)
                .vehicles(vehicles)
                .binZoneSnapshot(binZoneSnapshot)
                .vehicleTypeSnapshot(vehicleTypeSnapshot)
                .generatedAt(LocalDateTime.now().toString())
                .periodLabel(periodLabel)
                .build();

        // ── 5. Serialize to JSON ──────────────────────────────────────────────
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize report snapshot", e);
        }

        int fileSizeKb = snapshotJson.getBytes().length / 1024;

        // ── 6. Persist ────────────────────────────────────────────────────────
        MonthlyReport entity = MonthlyReport.builder()
                .title(title)
                .council(filtered ? councilId : null)
                .periodStart(periodStart)
                .periodEnd(today)
                .status("COMPLETED")
                .snapshot(snapshotJson)
                .fileSizeKb(fileSizeKb)
                .build();

        MonthlyReport saved = reportRepository.save(entity);
        log.info("Report saved with id={}, size={}KB", saved.getId(), fileSizeKb);

        return toSummary(saved);
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyReportSummaryDTO> getAllReports(String councilId) {
        boolean filtered = councilId != null && !councilId.isBlank();
        List<MonthlyReport> rows = filtered
                ? reportRepository.findScopedByCouncilOrderByCreatedAtDesc(councilId)
                : reportRepository.findAllByOrderByCreatedAtDesc();
        return rows.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MonthlyReportDetailDTO getReportById(Long id) {
        MonthlyReport entity = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found: " + id));

        ReportSnapshotPayload snapshot;
        try {
            snapshot = objectMapper.readValue(entity.getSnapshot(), ReportSnapshotPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize report snapshot", e);
        }

        return MonthlyReportDetailDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .fileSizeKb(entity.getFileSizeKb())
                .periodLabel(buildPeriodLabel(entity.getPeriodStart(), entity.getPeriodEnd()))
                .fileSizeDisplay(formatSize(entity.getFileSizeKb()))
                .snapshot(snapshot)
                .build();
    }

    // ── RAW SNAPSHOT ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String getRawSnapshot(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found: " + id))
                .getSnapshot();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private Map<String, Integer> buildBinZoneSnapshot(BinAnalyticsResponseDTO binAnalytics) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (binAnalytics != null && binAnalytics.getZoneData() != null) {
            for (ZoneAnalyticsDTO zone : binAnalytics.getZoneData()) {
                map.put(zone.getZone(), zone.getTotal());
            }
        }
        return map;
    }

    private Map<String, Long> buildVehicleTypeSnapshot(
            List<com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO.VehicleRowDTO> rows) {
        if (rows == null) return Map.of();
        return rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getType() != null ? r.getType() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private MonthlyReportSummaryDTO toSummary(MonthlyReport e) {
        return MonthlyReportSummaryDTO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .council(e.getCouncil())
                .periodStart(e.getPeriodStart())
                .periodEnd(e.getPeriodEnd())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .fileSizeKb(e.getFileSizeKb())
                .periodLabel(buildPeriodLabel(e.getPeriodStart(), e.getPeriodEnd()))
                .fileSizeDisplay(formatSize(e.getFileSizeKb()))
                .build();
    }

    private String buildPeriodLabel(LocalDate start, LocalDate end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return start.format(fmt) + " – " + end.format(fmt);
    }

    private String formatSize(Integer kb) {
        if (kb == null) return "-";
        if (kb < 1024) return kb + " KB";
        return String.format("%.1f MB", kb / 1024.0);
    }
}