package com.garbo.core.service.field_staff;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.BinDTO;
import com.garbo.api.dto.BinReportRequest;
import com.garbo.api.dto.BinUpdateRequest;
import com.garbo.api.dto.CouncilBoundaryDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.CouncilBoundary;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.CouncilBoundaryRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.UserTaskProgressService;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.core.service.event.BinChangedEvent;
import com.garbo.core.service.zone.ZoneClusteringService;
import com.garbo.core.entity.RouteBinStop;
import com.garbo.core.repository.RouteBinStopRepository;
import com.garbo.infrastructure.websocket.TaskProgressBroadcaster;
import com.garbo.infrastructure.websocket.RouteCollectionBroadcaster;
import com.garbo.infrastructure.websocket.TaskAlertBroadcaster;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Bin service includes multiple feature areas.
// In this scoped refactor pass, mobile-critical methods are:
//   - getAssignedBins
//   - reportBinStatus
//   - undoBinReport
@Service
@Slf4j
public class BinService {

    // ── HEAD dependencies ─────────────────────────────────────────────────────

    private final BinReportRepository binReportRepository;
    private final FieldMentorRepository fieldMentorRepository;
    private final CouncilAccessService councilAccessService;
    private final CouncilBoundaryRepository councilBoundaryRepository;
    private final UserTaskProgressService userTaskProgressService;
    private static final Map<String, CouncilBounds> COUNCIL_BOUNDS = buildCouncilBounds();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ── kevin-RWS dependencies ────────────────────────────────────────────────

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ZoneClusteringService zoneClusteringService;

    @Autowired
    private TaskAlertBroadcaster taskAlertBroadcaster;

    @Autowired
    private RouteBinStopRepository routeBinStopRepository;

    @Autowired
    private TaskProgressBroadcaster taskProgressBroadcaster;

    @Autowired
    private NotificationPublisher notificationPublisher;

    @Autowired
    private RouteCollectionBroadcaster routeCollectionBroadcaster;

    @Autowired
    private com.garbo.core.service.security.SystemIncidentService systemIncidentService;

    public BinService(BinRepository binRepository,
            BinReportRepository binReportRepository,
            FieldMentorRepository fieldMentorRepository,
            CouncilAccessService councilAccessService,
            CouncilBoundaryRepository councilBoundaryRepository,
            UserTaskProgressService userTaskProgressService) {
        this.binRepository = binRepository;
        this.binReportRepository = binReportRepository;
        this.fieldMentorRepository = fieldMentorRepository;
        this.councilAccessService = councilAccessService;
        this.councilBoundaryRepository = councilBoundaryRepository;
        this.userTaskProgressService = userTaskProgressService;
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

        String previousStatus = bin.getStatus();

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

        if (previousStatus != null && "empty".equalsIgnoreCase(previousStatus.trim())) {
            String newStatus = request.getStatus();
            if (newStatus != null
                    && ("half".equalsIgnoreCase(newStatus) || "full".equalsIgnoreCase(newStatus))) {
                report.setDiscrepancy(true);
                report.setPreviousStatus(previousStatus);
            }
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
        String reporterName = reporter != null ? reporter.getEmpName() : null;
        eventPublisher.publishEvent(new BinChangedEvent(
                "STATUS_REPORTED",
                binId,
                request.getStatus(),
                effectiveFillLevel,
                checkedAt,
                savedReport.getId(),
                request.getNotes(),
                request.getPhotoUrl(),
                reporterName,
                savedReport.isDiscrepancy(),
                savedReport.getPreviousStatus()));

        if (savedReport.isDiscrepancy()) {
            notificationPublisher.binDiscrepancyReported(bin, savedReport.getId());
        }

        if (reporter != null) {
            var updatedTasks = userTaskProgressService.incrementFieldMentorReportTasks(
                    reporter.getEmpId(),
                    binId
            );
            taskProgressBroadcaster.broadcastTaskProgressUpdate(
                    reporter.getEmpId(),
                    binId,
                    updatedTasks.size(),
                    updatedTasks
            );
        }

        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus(request.getStatus());
        updated.setFillLevel(effectiveFillLevel);
        updated.setLastChecked(checkedAt);
        return new BinStatusReportResult(
                updated,
                savedReport.getId(),
                savedReport.isDiscrepancy(),
                savedReport.getPreviousStatus());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.garbo.api.dto.BinLatestReportDTO getLatestReport(Long binId) {
        Bin bin = binRepository.findByNumericId(binId)
                .orElseThrow(() -> new EntityNotFoundException("Bin not found with ID: " + binId));

        return binReportRepository.findFirstByBin_IdOrderByReportedAtDesc(binId)
                .map(report -> com.garbo.api.dto.BinLatestReportDTO.builder()
                        .reportId(report.getId())
                        .binId(binId)
                        .binCode(bin.getBinCode())
                        .council(bin.getCouncil())
                        .status(report.getStatus())
                        .fillLevel(report.getFillLevel())
                        .notes(report.getNotes())
                        .photoUrl(report.getPhotoUrl())
                        .reporterName(report.getReporter() != null ? report.getReporter().getEmpName() : null)
                        .reportedAt(report.getReportedAt())
                        .discrepancy(report.isDiscrepancy())
                        .previousStatus(report.getPreviousStatus())
                        .build())
                .orElse(null);
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
        return toFormattedBins(getBins(council));
    }

    public List<Map<String, Object>> getFormattedBinsForMentor(Long empId) {
        if (empId == null) {
            return List.of();
        }
        List<Bin> bins = binRepository.findByAssignedToEmpId(empId);
        bins.forEach(this::normalizeReadModel);
        return toFormattedBins(bins);
    }

    private List<Map<String, Object>> toFormattedBins(List<Bin> bins) {
        List<Long> binIds = bins.stream()
                .map(Bin::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, ActiveDiscrepancy> activeDiscrepancies = new HashMap<>();
        if (!binIds.isEmpty()) {
            try {
                for (Object[] row : binReportRepository.findActiveDiscrepancyDetails(binIds)) {
                    if (row == null || row.length < 1 || row[0] == null) {
                        continue;
                    }
                    Long binId = ((Number) row[0]).longValue();
                    String status = row.length > 1 && row[1] != null ? row[1].toString() : null;
                    Integer fillLevel = row.length > 2 && row[2] != null ? ((Number) row[2]).intValue() : null;
                    String previousStatus = row.length > 3 && row[3] != null ? row[3].toString() : null;
                    String reporterName = row.length > 4 && row[4] != null ? row[4].toString() : null;
                    activeDiscrepancies.put(
                            binId,
                            new ActiveDiscrepancy(status, fillLevel, previousStatus, reporterName));
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to load active bin discrepancies: {}", ex.getMessage());
            }
        }

        return bins.stream().map(bin -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bin.getId());
            map.put("binCode", bin.getBinCode());
            map.put("council", bin.getCouncil());
            map.put("displayCode", formatDisplayCode(bin));

            String fullLocation = bin.getLocation() != null ? bin.getLocation() : "Unknown";
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
            String rowStatus = bin.getStatus() != null ? bin.getStatus() : "notChecked";
            Integer rowFillLevel = bin.getFillLevel();
            ActiveDiscrepancy discrepancy = activeDiscrepancies.get(bin.getId());
            boolean hasDiscrepancy = discrepancy != null;
            if (hasDiscrepancy && discrepancy.status() != null) {
                map.put("discrepancyStatus", discrepancy.status());
                rowStatus = discrepancy.status();
                if (discrepancy.fillLevel() != null) {
                    rowFillLevel = discrepancy.fillLevel();
                }
                if (discrepancy.previousStatus() != null) {
                    map.put("discrepancyPreviousStatus", discrepancy.previousStatus());
                }
                if (discrepancy.reporterName() != null) {
                    map.put("discrepancyReporterName", discrepancy.reporterName());
                }
            }
            map.put("status", rowStatus);
            map.put("fillLevel", rowFillLevel);
            map.put("lastChecked", bin.getLastChecked());
            map.put("lat", bin.getLatitude());
            map.put("lng", bin.getLongitude());
            map.put("latitude", bin.getLatitude());
            map.put("longitude", bin.getLongitude());
            map.put("zone", bin.getZone() != null ? bin.getZone() : "unassigned");
            map.put("priority", bin.getPriority() != null ? bin.getPriority() : "medium");
            map.put("coordinates", bin.getCoordinates());
            map.put("isAssigned", Boolean.TRUE.equals(bin.getIsAssigned()) || bin.getAssignedTo() != null);
            map.put("hasDiscrepancy", hasDiscrepancy);
            if (bin.getAssignedTo() != null) {
                map.put("assignedToName", bin.getAssignedTo().getEmpName());
                map.put("assignedToEmpId", bin.getAssignedTo().getEmpId());
            }
            return map;
        }).toList();
    }

    public Bin createBinForCurrentUser(Bin payload) {
        String email = currentEmail();
        String council = councilAccessService.resolveCouncilForEmail(email).orElse(null);
        double[] latLng = resolveIncomingCoordinates(payload);

        if (council == null) {
            // Verify if user is Superadmin
            if (councilAccessService.isSuperAdmin(email)) {
                // Use council from payload if provided, otherwise resolve automatically from coordinates
                if (payload.getCouncil() != null && !payload.getCouncil().isBlank()) {
                    council = payload.getCouncil();
                } else {
                    council = resolveCouncilFromCoordinates(latLng[0], latLng[1]);
                }
                if (council == null) {
                    throw new IllegalArgumentException("Coordinates are outside any supported municipal council boundary");
                }
            } else {
                throw new AccessDeniedException("Your account has no assigned council");
            }
        } else {
            validateCoordinatesInCouncil(council, latLng[0], latLng[1]);
        }

        payload.setCouncil(council);
        payload.setLocation(latLng[0] + "," + latLng[1]);
        payload.setCoordinates(latLng[0] + "," + latLng[1]);
        payload.setLatitude(latLng[0]);
        payload.setLongitude(latLng[1]);
        payload.setLastChecked(LocalDateTime.now());
        normalizeCreateModel(payload);
        assignZoneIfMissing(payload, council);

        Bin saved = binRepository.save(payload);
        // Use database-generated ID as the bin code
        saved.setBinCode(String.valueOf(saved.getId()));
        saved = binRepository.save(saved);
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        return saved;
    }

    @Transactional
    public Bin createBinFromSuggestion(BinSuggestion suggestion) {
        if (suggestion == null || suggestion.getCouncil() == null || suggestion.getCouncil().isBlank()) {
            throw new IllegalArgumentException("Suggestion council is required");
        }
        if (suggestion.getLatitude() == null || suggestion.getLongitude() == null) {
            throw new IllegalArgumentException("Suggestion coordinates are required");
        }

        String council = suggestion.getCouncil().trim();
        double lat = suggestion.getLatitude();
        double lng = suggestion.getLongitude();

        Bin payload = new Bin();
        payload.setCategory(
                suggestion.getCategory() != null && !suggestion.getCategory().isBlank()
                        ? suggestion.getCategory().trim()
                        : "general");
        payload.setStatus("empty");
        payload.setCouncil(council);
        payload.setLatitude(lat);
        payload.setLongitude(lng);
        payload.setLocation(lat + "," + lng);
        payload.setCoordinates(lat + "," + lng);
        payload.setLastChecked(LocalDateTime.now());
        normalizeCreateModel(payload);
        assignZoneIfMissing(payload, council);

        if (suggestion.getMentorId() != null) {
            fieldMentorRepository.findById(suggestion.getMentorId()).ifPresent(mentor -> {
                payload.setAssignedTo(mentor);
                payload.setIsAssigned(true);
            });
        }

        Bin saved = binRepository.save(payload);
        // Use database-generated ID as the bin code
        saved.setBinCode(String.valueOf(saved.getId()));
        saved = binRepository.save(saved);
        systemIncidentService.logIncident(
            "BIN_ADDITION",
            saved.getId().toString(),
            "Bin code " + saved.getBinCode() + " created in council " + saved.getCouncil()
        );
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        if (saved.getAssignedTo() != null) {
            taskAlertBroadcaster.notifyMentorBinAssigned(saved.getAssignedTo(), saved);
            notificationPublisher.binAssigned(saved.getAssignedTo(), saved);
        }
        return saved;
    }

    @Transactional
    public void deleteBinForCurrentUser(Long id) {
        Bin bin = getBinWithCouncilAccess(id);
        binReportRepository.deleteByBinId(bin.getId());
        binRepository.deleteByIdNative(bin.getId());
        systemIncidentService.logIncident(
            "BIN_DELETION",
            id.toString(),
            "Bin deleted: code " + bin.getBinCode()
        );
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
    }

    @Transactional
    public void deleteBinsForCurrentUser(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Bin bin = getBinWithCouncilAccess(id);
            systemIncidentService.logIncident(
                "BIN_DELETION",
                id.toString(),
                "Batch bin deleted: code " + bin.getBinCode()
            );
        }
        binReportRepository.deleteByBinIds(ids);
        binRepository.deleteAllByIds(ids);
        for (Long id : ids) {
            eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
        }
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

    @Transactional
    public Bin assignMentorToBin(Long binId, Long mentorEmpId) {
        Bin bin = getBinWithCouncilAccess(binId);
        if (mentorEmpId == null) {
            bin.setAssignedTo(null);
        } else {
            FieldMentor mentor = fieldMentorRepository.findById(mentorEmpId)
                    .orElseThrow(() -> new NoSuchElementException("Field mentor not found"));
            if (bin.getCouncil() != null && mentor.getAssignedCouncil() != null
                    && !bin.getCouncil().equalsIgnoreCase(mentor.getAssignedCouncil())) {
                throw new IllegalArgumentException("Mentor must belong to the same council as the bin");
            }
            bin.setAssignedTo(mentor);
        }
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent(
                "MENTOR_ASSIGNED",
                saved.getId(),
                saved.getStatus(),
                saved.getFillLevel(),
                LocalDateTime.now()));
        if (saved.getAssignedTo() != null) {
            taskAlertBroadcaster.notifyMentorBinAssigned(saved.getAssignedTo(), saved);
            notificationPublisher.binAssigned(saved.getAssignedTo(), saved);
        }
        normalizeReadModel(saved);
        return saved;
    }

    @Transactional
    public Bin updateBinForCurrentUser(Long binId, BinUpdateRequest request) {
        Bin bin = getBinWithCouncilAccess(binId);
        if (request == null) {
            throw new IllegalArgumentException("Update payload is required");
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            bin.setCategory(request.getCategory().trim());
        }
        if (request.getBinCode() != null && !request.getBinCode().isBlank()) {
            bin.setBinCode(request.getBinCode().trim());
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            String normalized = request.getPriority().trim().toLowerCase(Locale.ROOT);
            if (!normalized.equals("low") && !normalized.equals("medium") && !normalized.equals("high")) {
                throw new IllegalArgumentException("Priority must be low, medium, or high");
            }
            bin.setPriority(normalized);
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            bin.setStatus(request.getStatus().trim().toLowerCase(Locale.ROOT));
            if ("full".equalsIgnoreCase(bin.getStatus())) {
                bin.setFillLevel(100);
            } else if ("half".equalsIgnoreCase(bin.getStatus())) {
                bin.setFillLevel(50);
            } else if ("empty".equalsIgnoreCase(bin.getStatus())) {
                bin.setFillLevel(0);
            }
        }
        Double lat = request.getLatitude();
        Double lng = request.getLongitude();
        if (lat != null && lng != null) {
            String email = currentEmail();
            Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(email);
            if (councilOpt.isPresent()) {
                validateCoordinatesInCouncil(councilOpt.get(), lat, lng);
            }
            bin.setLatitude(lat);
            bin.setLongitude(lng);
            bin.setCoordinates(lat + "," + lng);
            bin.setLocation(lat + "," + lng);
        } else if (request.getLocation() != null && !request.getLocation().isBlank()) {
            bin.setLocation(request.getLocation().trim());
            double[] parsed = parseCoordinates(request.getLocation().trim());
            if (parsed != null) {
                bin.setLatitude(parsed[0]);
                bin.setLongitude(parsed[1]);
                bin.setCoordinates(parsed[0] + "," + parsed[1]);
            }
        }
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", saved.getId()));
        normalizeReadModel(saved);
        return saved;
    }

    /** Resets bin to empty after collection (collector or admin). */
    @Transactional
    public void resetBinAfterCollection(Long binId) {
        if (binId == null) {
            return;
        }
        Bin bin = binRepository.findByNumericId(binId)
                .orElseThrow(() -> new EntityNotFoundException("Bin not found with ID: " + binId));
        int updated = binRepository.resetAfterCollection(binId);
        if (updated == 0) {
            throw new EntityNotFoundException("Bin not found with ID: " + binId);
        }

        BinReport collectionReport = new BinReport();
        collectionReport.setBin(bin);
        collectionReport.setStatus("empty");
        collectionReport.setFillLevel(0);
        collectionReport.setSource("COLLECTION");
        collectionReport.setDiscrepancy(false);
        binReportRepository.save(collectionReport);

        eventPublisher.publishEvent(new BinChangedEvent(
                "COLLECTED",
                binId,
                "empty",
                0,
                LocalDateTime.now()));
    }

    /** Admin manual collection — resets bin and marks any pending route stops collected. */
    @Transactional
    public void adminCollectBin(Long binId) {
        getBinWithCouncilAccess(binId);
        if (binId == null || binId <= 0) {
            throw new IllegalArgumentException("Invalid bin id for admin collection");
        }
        List<RouteBinStop> pendingStops = routeBinStopRepository.findPendingStopsByBinId(binId);
        for (RouteBinStop stop : pendingStops) {
            int updated = routeBinStopRepository.markCollected(stop.getId(), LocalDateTime.now());
            if (updated > 0 && stop.getVehicleRoute() != null) {
                routeCollectionBroadcaster.broadcastBinStatusUpdate(
                        stop.getVehicleRoute().getSessionId(),
                        binId,
                        "COLLECTED");
            }
        }
        resetBinAfterCollection(binId);
    }

    private double[] parseCoordinates(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length < 2) {
            return null;
        }
        try {
            return new double[] {
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim())
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ── Methods from kevin-RWS ────────────────────────────────────────────────

    // Add new bin
    public Bin addBin(BinDTO dto) {
        Bin bin = new Bin();
        bin.setLatitude(dto.getLat());
        bin.setLng(dto.getLng());
        bin.setFillLevel(dto.getFillLevel());
        bin.setPriority(dto.getPriority());
        if (dto.getZone() != null && !dto.getZone().isBlank()) {
            bin.setZone(dto.getZone());
        }
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
    }

    /** Backend assigns zone from coordinates when admin omits it (W5). */
    private void assignZoneIfMissing(Bin bin, String council) {
        if (bin.getZone() != null && !bin.getZone().isBlank()
                && !"unassigned".equalsIgnoreCase(bin.getZone().trim())) {
            return;
        }
        if (council == null || bin.getLatitude() == null || bin.getLongitude() == null) {
            bin.setZone("1");
            return;
        }
        List<Bin> councilBins = binRepository.findAllByCouncil(council);
        String zone = zoneClusteringService.assignZoneForCoordinates(
                council, bin.getLatitude(), bin.getLongitude(), councilBins);
        bin.setZone(zone);
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

    private boolean isPointInPolygon(double lat, double lng, List<CouncilBoundaryDTO.CoordinatePoint> polygon) {
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
        Optional<CouncilBoundary> dbBoundary = councilBoundaryRepository.findByCouncilIgnoreCase(council);
        if (dbBoundary.isPresent()) {
            List<CouncilBoundaryDTO.CoordinatePoint> points = null;
            try {
                points = objectMapper.readValue(
                    dbBoundary.get().getBoundaryPoints(),
                    new TypeReference<List<CouncilBoundaryDTO.CoordinatePoint>>() {}
                );
            } catch (Exception e) {
                // Ignore parsing errors and fallback
            }
            if (points != null && !points.isEmpty()) {
                if (!isPointInPolygon(lat, lng, points)) {
                    throw new IllegalArgumentException("Coordinates are outside the council boundary");
                }
                return;
            }
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

    public record BinStatusReportResult(
            Bin bin,
            Long reportId,
            boolean discrepancy,
            String previousStatus) {
    }

    private record ActiveDiscrepancy(
            String status,
            Integer fillLevel,
            String previousStatus,
            String reporterName) {
    }

    private String resolveCouncilFromCoordinates(double lat, double lng) {
        // Query all council boundary points from DB
        List<CouncilBoundary> allBoundaries = councilBoundaryRepository.findAll();
        if (allBoundaries == null || allBoundaries.isEmpty()) {
            // Fallback to legacy rectangular bounds
            for (Map.Entry<String, CouncilBounds> entry : COUNCIL_BOUNDS.entrySet()) {
                if (entry.getValue().contains(lat, lng)) {
                    return getStandardCouncilName(entry.getKey());
                }
            }
            return null;
        }

        // Perform point-in-polygon checks
        for (CouncilBoundary cb : allBoundaries) {
            List<CouncilBoundaryDTO.CoordinatePoint> points = null;
            try {
                points = objectMapper.readValue(
                    cb.getBoundaryPoints(),
                    new TypeReference<List<CouncilBoundaryDTO.CoordinatePoint>>() {}
                );
            } catch (Exception e) {
                // Ignore parsing errors
            }
            if (points != null && !points.isEmpty()) {
                if (isPointInPolygon(lat, lng, points)) {
                    return cb.getCouncil();
                }
            }
        }

        return null;
    }

    private String getStandardCouncilName(String lowercaseName) {
        switch (lowercaseName) {
            case "colombo": return "Colombo";
            case "dehiwala-mt. lavinia": return "Dehiwala-Mt. Lavinia";
            case "kaduwela": return "Kaduwela";
            case "moratuwa": return "Moratuwa";
            case "sri jayewardenepura kotte": return "Sri Jayewardenepura Kotte";
            default: return lowercaseName;
        }
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
