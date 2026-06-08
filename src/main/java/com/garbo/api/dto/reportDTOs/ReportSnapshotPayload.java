package com.garbo.api.dto.reportDTOs;

import com.garbo.api.dto.Collect_analyze_dtos.DashboardResponseDTO;
import com.garbo.api.dto.binAnalyzeDTOs.BinAnalyticsResponseDTO;
import com.garbo.api.dto.ComplaintDTOs.ComplaintAnalyticsResponseDTO;
import com.garbo.api.dto.staffAnalyzeDTOs.StaffAnalyticsResponseDTO;
import com.garbo.api.dto.ThirdPartyAnalyseDTOs.ThirdPartyAnalyticsResponseDTO;
import com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO;
import com.garbo.api.dto.ZoneCollectionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSnapshotPayload {

    // ── Collection analytics (MONTH filter) ──────────────────────────────────
    private DashboardResponseDTO collection;

    // ── Collection by zone (MONTH filter) ────────────────────────────────────
    private List<ZoneCollectionDTO> zoneCollection;

    // ── Bin analytics — zone breakdown + total bin count ─────────────────────
    private BinAnalyticsResponseDTO binAnalytics;

    // ── Complaint analytics (MONTH filter) ───────────────────────────────────
    private ComplaintAnalyticsResponseDTO complaints;

    // ── Staff analytics — total staff count ──────────────────────────────────
    private StaffAnalyticsResponseDTO staff;

    // ── Third-party analytics (LAST_MONTH filter) ────────────────────────────
    private ThirdPartyAnalyticsResponseDTO thirdParty;

    // ── Vehicle analytics — full fleet ───────────────────────────────────────
    private VehicleAnalyticsDTO vehicles;

    // ── Extra snapshots ───────────────────────────────────────────────────────
    private Map<String, Integer> binZoneSnapshot;
    private Map<String, Long>    vehicleTypeSnapshot;

    // ── Report metadata ───────────────────────────────────────────────────────
    private String generatedAt;
    private String periodLabel;
}