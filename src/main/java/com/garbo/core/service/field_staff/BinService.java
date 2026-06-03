package com.garbo.core.service.field_staff;

import com.garbo.api.dto.BinDTO;
import com.garbo.api.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.CouncilBoundary;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.CouncilBoundaryRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.event.BinChangedEvent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

// Bin service includes multiple feature areas.
// In this scoped refactor pass, mobile-critical methods are:
//   - getAssignedBins
//   - reportBinStatus
//   - undoBinReport
@Service
public class BinService {

    // ── HEAD dependencies ─────────────────────────────────────────────────────

    private final BinReportRepository binReportRepository;
    private final FieldMentorRepository fieldMentorRepository;
    private final CouncilAccessService councilAccessService;
    private final CouncilBoundaryRepository councilBoundaryRepository;
    private static final Map<String, CouncilBounds> COUNCIL_BOUNDS = buildCouncilBounds();

    // ── kevin-RWS dependencies ────────────────────────────────────────────────

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public BinService(BinRepository binRepository,
            BinReportRepository binReportRepository,
            FieldMentorRepository fieldMentorRepository,
            CouncilAccessService councilAccessService,
            CouncilBoundaryRepository councilBoundaryRepository) {
        this.binRepository = binRepository;
        this.binReportRepository = binReportRepository;
        this.fieldMentorRepository = fieldMentorRepository;
        this.councilAccessService = councilAccessService;
        this.councilBoundaryRepository = councilBoundaryRepository;
    }

    // ── Methods from HEAD ─────────────────────────────────────────────────────

    public List<Bin> getAssignedBins(Long empId) {
        return binRepository.findByAssignedToEmpId(empId);
    }

    // Shared report operation used by both anonymous JSON report and field-staff
    // multipart report.
    @Transactional
    public BinStatusReportResult reportBinStatus(Long binId, Long reporterId, BinReportRequest request) {
        Bin bin = binRepository.findByNumericId(binId)
                .orElseThrow(() -> new EntityNotFoundException("Bin not found with ID: " + binId));

        FieldMentor reporter = null;
        if (reporterId != null) {
            reporter = fieldMentorRepository.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Field Mentor not found with ID: " + reporterId));
        }

        // Create Report
        BinReport report = new BinReport();
        report.setBin(bin);
        report.setReporter(reporter);
        report.setFillLevel(request.getFillLevel());
        report.setStatus(request.getStatus());
        report.setNotes(request.getNotes());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setPhotoUrl(request.getPhotoUrl());

        // Determine source
        if (reporter != null) {
            report.setSource("FIELD_STAFF");
        } else {
            report.setSource("ANONYMOUS");
        }

        BinReport savedReport = binReportRepository.save(report);

        // Update the bin via native query because bins.id is stored as text in DB.
        Integer effectiveFillLevel = "full".equalsIgnoreCase(request.getStatus()) ? 100
                : "half".equalsIgnoreCase(request.getStatus()) ? 50 : 0;
        int updatedRows = binRepository.updateStatusForReport(binId, request.getStatus(), effectiveFillLevel);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Bin not found with ID: " + binId);
        }

        LocalDateTime checkedAt = LocalDateTime.now();
        bin.setStatus(request.getStatus());
        bin.setFillLevel(request.getFillLevel());
        bin.setLastChecked(checkedAt);

        // Trigger realtime websocket push for dashboards listening to bin-status
        // changes.
        eventPublisher.publishEvent(new BinChangedEvent(
                "STATUS_REPORTED",
                binId,
                request.getStatus(),
                request.getFillLevel(),
                checkedAt));

        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus(request.getStatus());
        updated.setFillLevel(effectiveFillLevel);
        updated.setLastChecked(checkedAt);
        return new BinStatusReportResult(updated, savedReport.getId());
    }

    // Field-staff undo operation used by dedicated mobile undo endpoint.
    @Transactional
    public Bin undoBinReport(Long binId, Long reporterId) {
        if (reporterId != null) {
            fieldMentorRepository.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Field Mentor not found with ID: " + reporterId));
        }

        int updatedRows = binRepository.resetStatusForUndo(binId);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Bin not found with ID: " + binId);
        }

        // Trigger realtime websocket push for dashboards listening to bin-status
        // changes.
        eventPublisher.publishEvent(new BinChangedEvent(
                "STATUS_UNDONE",
                binId,
                "notChecked",
                0,
                null));

        // Return a lightweight response object with final state expected by mobile.
        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus("notChecked");
        updated.setFillLevel(0);
        updated.setLastChecked(null);
        return updated;
    }

    public Bin createBin(Bin bin) {
        if (bin.getId() != null && binRepository.existsById(bin.getId())) {
            throw new IllegalArgumentException("Bin with ID " + bin.getId() + " already exists.");
        }
        bin.setId(null);
        return binRepository.save(bin);
    }

    public List<Bin> getBins(String council) {
        List<Bin> bins = (council == null || council.isBlank())
                ? binRepository.findAll()
                : binRepository.findByCouncilIgnoreCase(council.trim());
        bins.forEach(this::normalizeReadModel);
        return bins;
    }

    /**
     * Format bins for API response with human-readable display codes.
     * Transforms Bin entities into Map<String, Object> for JSON serialization.
     */
    public List<Map<String, Object>> getFormattedBinsForCouncil(String council) {
        List<Bin> bins = getBins(council);
        return bins.stream().map(bin -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bin.getId());
            map.put("binCode", bin.getBinCode());
            map.put("council", bin.getCouncil());
            map.put("displayCode", formatDisplayCode(bin));

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
            map.put("lat", bin.getLatitude());
            map.put("lng", bin.getLongitude());
            if (bin.getAssignedTo() != null) {
                map.put("assignedToName", bin.getAssignedTo().getEmpName());
            }
            return map;
        }).toList();

    }

    public Bin createBinForCurrentUser(Bin payload) {
        String email = currentEmail();
        String council = councilAccessService.resolveCouncilForEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Your account has no assigned council"));
        double[] latLng = resolveIncomingCoordinates(payload);
        validateCoordinatesInCouncil(council, latLng[0], latLng[1]);

        String generatedCode = generateNextBinCode(council);
        payload.setBinCode(generatedCode);
        payload.setCouncil(council);
        payload.setLocation(latLng[0] + "," + latLng[1]);
        payload.setCoordinates(latLng[0] + "," + latLng[1]);
        payload.setLatitude(latLng[0]);
        payload.setLongitude(latLng[1]);
        payload.setLastChecked(LocalDateTime.now());
        normalizeCreateModel(payload);

        Bin saved = binRepository.save(payload);
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        return saved;
    }

    public void deleteBinForCurrentUser(Long id) {
        Bin bin = getBinWithCouncilAccess(id);
        binRepository.deleteByIdNative(bin.getId());
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
    }

    public Bin updatePriorityForCurrentUser(Long id, String priority) {
        Bin bin = getBinWithCouncilAccess(id);
        String normalized = (priority == null || priority.isBlank()) ? "medium"
                : priority.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("low") && !normalized.equals("medium") && !normalized.equals("high")) {
            throw new IllegalArgumentException("Priority must be low, medium, or high");
        }
        bin.setPriority(normalized);
        binRepository.updatePriorityNative(id, normalized);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
        normalizeReadModel(bin);
        return bin;
    }

    public Bin updateZoneForCurrentUser(Long id, String zone) {
        Bin bin = getBinWithCouncilAccess(id);
        String safeZone = zone == null || zone.isBlank() ? "unassigned" : zone.trim();
        bin.setZone(safeZone);
        binRepository.updateZoneNative(id, safeZone);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
        normalizeReadModel(bin);
        return bin;
    }

    // ── Methods from kevin-RWS ────────────────────────────────────────────────

    // Add new bin
    public Bin addBin(BinDTO dto) {
        Bin bin = new Bin();
        bin.setLatitude(dto.getLat());
        bin.setLng(dto.getLng());
        bin.setFillLevel(dto.getFillLevel());
        bin.setPriority(dto.getPriority());
        String zone = dto.getZone() == null || dto.getZone().isBlank() ? "unassigned" : dto.getZone();
        bin.setZone(zone);
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        return saved;
    }

    // Remove bin
    public void deleteBin(Long id) {
        binRepository.deleteByIdNative(id);
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
    }

    // Get all bins (for map)
    public List<Bin> getAllBins() {
        return binRepository.findAllForMap()
                .stream()
                .map(row -> {
                    Bin bin = new Bin();
                    bin.setId(row.getId());
                    bin.setLatitude(row.getLat());
                    bin.setLng(row.getLng());
                    bin.setFillLevel(row.getFillLevel() != null ? row.getFillLevel() : 0);
                    bin.setPriority(row.getPriority());
                    bin.setZone(row.getZone());
                    return bin;
                })
                .toList();
    }

    // Get bin details
    public Bin getBinById(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bin not found"));
    }

    // Update bin priority
    public void updatePriority(Long id, String priority) {
        binRepository.updatePriorityNative(id, priority);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
    }

    // Update bin zone
    public void updateZone(Long id, String zone) {
        String safeZone = zone == null || zone.isBlank() ? "unassigned" : zone;
        binRepository.updateZoneNative(id, safeZone);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
    }

    private void normalizeCreateModel(Bin bin) {
        if (bin.getStatus() == null || bin.getStatus().isBlank()) {
            bin.setStatus("empty");
        } else {
            bin.setStatus(bin.getStatus().trim().toLowerCase(Locale.ROOT));
        }

        // Map status to fill level for legacy support/optimization
        if ("full".equalsIgnoreCase(bin.getStatus())) {
            bin.setFillLevel(100);
        } else if ("half".equalsIgnoreCase(bin.getStatus())) {
            bin.setFillLevel(50);
        } else {
            bin.setFillLevel(0);
        }

        if (bin.getPriority() == null || bin.getPriority().isBlank()) {
            bin.setPriority("medium");
        } else {
            bin.setPriority(bin.getPriority().trim().toLowerCase(Locale.ROOT));
        }
        if (bin.getZone() == null || bin.getZone().isBlank()) {
            bin.setZone("unassigned");
        }
    }

    private void normalizeReadModel(Bin bin) {
        if ((bin.getCoordinates() == null || bin.getCoordinates().isBlank())
                && bin.getLatitude() != null && bin.getLongitude() != null) {
            bin.setCoordinates(bin.getLatitude() + "," + bin.getLongitude());
        }
        if (bin.getLocation() == null || bin.getLocation().isBlank()) {
            bin.setLocation(bin.getCoordinates());
        }
    }

    private String currentEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return auth.getName();
    }

    private Bin getBinWithCouncilAccess(Long id) {
        Bin existing = binRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Bin not found"));
        String email = currentEmail();
        boolean superAdmin = councilAccessService.isSuperAdmin(email);
        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(email);
        if (!superAdmin) {
            String requesterCouncil = councilOpt.orElse("");
            if (requesterCouncil.isBlank()
                    || existing.getCouncil() == null
                    || !existing.getCouncil().equalsIgnoreCase(requesterCouncil)) {
                throw new AccessDeniedException("You can only manage bins from your council");
            }
        }
        return existing;
    }

    /**
     * Format a Bin into a display code (e.g., "BIN-03|Moratuwa").
     * Extracts numeric suffix from binCode and appends council name.
     * Fallback: uses full binCode or bin ID if extraction fails.
     */
    public String formatDisplayCode(Bin bin) {
        String binCode = bin.getBinCode();
        String council = bin.getCouncil();

        String codePart = null;
        if (binCode != null && !binCode.isBlank()) {
            String trimmed = binCode.trim();
            int lastDash = trimmed.lastIndexOf('-');
            String numericSuffix = lastDash >= 0 && lastDash < trimmed.length() - 1
                    ? trimmed.substring(lastDash + 1).trim()
                    : trimmed;
            if (numericSuffix.matches("\\d+")) {
                codePart = String.format("BIN-%02d", Integer.parseInt(numericSuffix));
            }
        }

        String councilPart = council;
        if (councilPart == null || councilPart.isBlank()) {
            if (binCode != null && !binCode.isBlank() && binCode.contains("-")) {
                councilPart = binCode.substring(0, binCode.lastIndexOf('-')).trim();
            }
        }

        if (codePart != null && councilPart != null && !councilPart.isBlank()) {
            return codePart + "|" + councilPart;
        }
        if (binCode != null && !binCode.isBlank() && councilPart != null && !councilPart.isBlank()) {
            return binCode.trim() + "|" + councilPart;
        }
        if (binCode != null && !binCode.isBlank()) {
            return binCode.trim();
        }
        return bin.getId() != null ? "BIN-" + bin.getId() : "Unknown";
    }

    private String generateNextBinCode(String council) {
        List<Bin> councilBins = binRepository.findByCouncilIgnoreCase(council.trim());
        String prefix = council.trim() + "-";
        int nextNumber = councilBins.stream()
                .map(Bin::getBinCode)
                .filter(code -> code != null && code.regionMatches(true, 0, prefix, 0, prefix.length()))
                .map(code -> code.substring(prefix.length()).trim())
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0) + 1;
        return prefix + nextNumber;
    }

    private double[] parseLatLng(String rawLocation) {
        String[] parts = rawLocation.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Location must be in lat,lng format");
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw new IllegalArgumentException("Latitude/longitude out of range");
            }
            return new double[] { lat, lng };
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Location must contain valid latitude and longitude");
        }
    }

    private double[] resolveIncomingCoordinates(Bin payload) {
        if (payload.getLocation() != null && !payload.getLocation().isBlank()) {
            return parseLatLng(payload.getLocation());
        }
        if (payload.getLatitude() != null && payload.getLongitude() != null) {
            double lat = payload.getLatitude();
            double lng = payload.getLongitude();
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw new IllegalArgumentException("Latitude/longitude out of range");
            }
            return new double[] { lat, lng };
        }
        throw new IllegalArgumentException("Location is required as lat,lng");
    }

    private boolean isPointInPolygon(double lat, double lng, List<CouncilBoundary> polygon) {
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i).getLng();
            double yi = polygon.get(i).getLat();
            double xj = polygon.get(j).getLng();
            double yj = polygon.get(j).getLat();

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private void validateCoordinatesInCouncil(String council, double lat, double lng) {
        List<CouncilBoundary> dbBoundary = councilBoundaryRepository.findByCouncilIgnoreCaseOrderByPointOrderAsc(council);
        if (dbBoundary != null && !dbBoundary.isEmpty()) {
            if (!isPointInPolygon(lat, lng, dbBoundary)) {
                throw new IllegalArgumentException("Coordinates are outside the council boundary");
            }
            return;
        }

        // Fallback to legacy rectangular bounds
        CouncilBounds bounds = COUNCIL_BOUNDS.get(council.toLowerCase(Locale.ROOT));
        if (bounds == null) {
            throw new IllegalArgumentException("Unsupported council for coordinate validation: " + council);
        }
        if (!bounds.contains(lat, lng)) {
            throw new IllegalArgumentException("Coordinates are outside the council boundary");
        }
    }

    private static Map<String, CouncilBounds> buildCouncilBounds() {
        Map<String, CouncilBounds> bounds = new HashMap<>();
        bounds.put("colombo", new CouncilBounds(6.83, 6.98, 79.82, 79.91));
        bounds.put("dehiwala-mt. lavinia", new CouncilBounds(6.79, 6.88, 79.84, 79.92));
        bounds.put("kaduwela", new CouncilBounds(6.91, 7.03, 79.96, 80.08));
        bounds.put("moratuwa", new CouncilBounds(6.74, 6.83, 79.85, 79.92));
        bounds.put("sri jayewardenepura kotte", new CouncilBounds(6.86, 6.93, 79.89, 79.95));
        return bounds;
    }

    public record BinStatusReportResult(Bin bin, Long reportId) {
    }

    private static class CouncilBounds {
        private final double minLat;
        private final double maxLat;
        private final double minLng;
        private final double maxLng;

        private CouncilBounds(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }

        private boolean contains(double lat, double lng) {
            return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
        }
    }
}
