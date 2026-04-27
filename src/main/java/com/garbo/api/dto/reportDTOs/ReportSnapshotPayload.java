package com.garbo.api.dto.reportDTOs;
 
 
import com.garbo.api.dto.Collect_analyze_dtos.DashboardResponseDTO;
import com.garbo.api.dto.binAnalyzeDTOs.BinAnalyticsResponseDTO;
import com.garbo.api.dto.ComplaintDTOs.ComplaintAnalyticsResponseDTO;
import com.garbo.api.dto.staffAnalyzeDTOs.StaffAnalyticsResponseDTO;
import com.garbo.api.dto.ThirdPartyAnalyseDTOs.ThirdPartyAnalyticsResponseDTO;
import com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.util.Map;
 
/**
 * This is the object serialized to JSONB and stored in monthly_reports.snapshot.
 * It aggregates all required analytics service responses for the 30-day window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSnapshotPayload {
 
    // ── Collection analytics (MONTH filter) ──────────────────────────────────
    private DashboardResponseDTO collection;
 
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
 
    // ── Extra snapshots computed at generation time ───────────────────────────
 
    /**
     * Total bin count per zone  e.g. {"Zone A": 12, "Zone B": 8, ...}
     * Derived from binAnalytics.zoneData at generation time.
     */
    private Map<String, Integer> binZoneSnapshot;
 
    /**
     * Vehicle count per type  e.g. {"Truck": 4, "Compactor": 3, "Mini Truck": 1}
     * Derived from vehicles.vehicles at generation time.
     */
    private Map<String, Long> vehicleTypeSnapshot;
 
    // ── Report metadata stored inside snapshot for convenience ───────────────
    private String generatedAt;   // ISO-8601 timestamp
    private String periodLabel;   // e.g. "26 Mar 2025 – 26 Apr 2025"
}