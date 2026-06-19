package com.garbo.core.service;

import com.garbo.api.dto.BinSuggestionCreateRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinSuggestionRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.field_staff.BinService;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.infrastructure.websocket.TaskAlertBroadcaster;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class BinSuggestionService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    private final BinSuggestionRepository binSuggestionRepository;
    private final UserRepository userRepository;
    private final CouncilAccessService councilAccessService;
    private final BinService binService;
    private final TaskAlertBroadcaster taskAlertBroadcaster;
    private final NotificationPublisher notificationPublisher;

    public BinSuggestionService(
            BinSuggestionRepository binSuggestionRepository,
            UserRepository userRepository,
            CouncilAccessService councilAccessService,
            BinService binService,
            TaskAlertBroadcaster taskAlertBroadcaster,
            NotificationPublisher notificationPublisher) {
        this.binSuggestionRepository = binSuggestionRepository;
        this.userRepository = userRepository;
        this.councilAccessService = councilAccessService;
        this.binService = binService;
        this.taskAlertBroadcaster = taskAlertBroadcaster;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional
    public BinSuggestion createSuggestion(BinSuggestionCreateRequest request, String mentorEmail) {
        FieldMentor mentor = requireFieldMentor(mentorEmail);
        String council = mentor.getAssignedCouncil();
        if (council == null || council.isBlank()) {
            throw new IllegalArgumentException("Your account has no assigned council");
        }

        double[] latLng = resolveCoordinates(request);

        BinSuggestion suggestion = new BinSuggestion();
        suggestion.setMentorId(mentor.getEmpId());
        suggestion.setMentorName(mentor.getEmpName());
        suggestion.setCouncil(council.trim());
        suggestion.setLatitude(latLng[0]);
        suggestion.setLongitude(latLng[1]);
        suggestion.setLocation(latLng[0] + "," + latLng[1]);
        suggestion.setCategory(normalizeCategory(request.getCategory()));
        suggestion.setNotes(trimOrNull(request.getNotes()));
        suggestion.setImageUrl(trimOrNull(request.getImageUrl()));
        suggestion.setStatus("PENDING");

        BinSuggestion saved = binSuggestionRepository.save(suggestion);
        notificationPublisher.binSuggestionSubmitted(saved);
        return saved;
    }

    public List<BinSuggestion> getMySuggestions(String mentorEmail) {
        FieldMentor mentor = requireFieldMentor(mentorEmail);
        return binSuggestionRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getEmpId());
    }

    public List<BinSuggestion> getAllForRequester(String requesterEmail) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return binSuggestionRepository.findAll();
        }
        assertAdminCanModerate(requesterEmail);
        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (councilOpt.isEmpty()) {
            return List.of();
        }
        return binSuggestionRepository.findByCouncilIgnoreCaseOrderByCreatedAtDesc(councilOpt.get());
    }

    public BinSuggestion getById(Long id, String requesterEmail) {
        if (isFieldMentor(requesterEmail)) {
            FieldMentor mentor = requireFieldMentor(requesterEmail);
            BinSuggestion suggestion = binSuggestionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Bin suggestion not found"));
            if (suggestion.getMentorId() == null || !suggestion.getMentorId().equals(mentor.getEmpId())) {
                throw new AccessDeniedException("Suggestion is not visible to this mentor");
            }
            return suggestion;
        }

        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return binSuggestionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Bin suggestion not found"));
        }

        assertAdminCanModerate(requesterEmail);
        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (councilOpt.isEmpty()) {
            throw new AccessDeniedException("No council assigned for current user");
        }
        return binSuggestionRepository.findByIdAndCouncilIgnoreCase(id, councilOpt.get())
                .orElseThrow(() -> new RuntimeException("Bin suggestion not found"));
    }

    @Transactional
    public BinSuggestion updateStatus(Long id, String status, String resolutionNotes, String requesterEmail) {
        assertAdminCanModerate(requesterEmail);

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String normalizedStatus = normalizeStatus(status);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported suggestion status: " + status);
        }
        if ("PENDING".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Cannot revert suggestion to pending");
        }

        BinSuggestion suggestion = getById(id, requesterEmail);
        if (!"PENDING".equalsIgnoreCase(suggestion.getStatus())) {
            throw new IllegalArgumentException("Suggestion has already been reviewed");
        }

        suggestion.setStatus(normalizedStatus);
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            suggestion.setResolutionNotes(resolutionNotes.trim());
        } else if ("REJECTED".equals(normalizedStatus)) {
            suggestion.setResolutionNotes("Rejected by admin");
        }

        if ("APPROVED".equals(normalizedStatus)) {
            Bin created = binService.createBinFromSuggestion(suggestion);
            suggestion.setCreatedBinId(created.getId());
            if (resolutionNotes == null || resolutionNotes.isBlank()) {
                suggestion.setResolutionNotes("Approved and bin created");
            }
        }

        BinSuggestion saved = binSuggestionRepository.save(suggestion);
        taskAlertBroadcaster.notifyMentorBinSuggestionUpdated(saved);
        notificationPublisher.binSuggestionResolved(saved);
        return saved;
    }

    private FieldMentor requireFieldMentor(String email) {
        var user = UserLookup.requireUser(userRepository, email);
        if (!(user instanceof FieldMentor mentor)) {
            throw new AccessDeniedException("Only field mentors can submit bin suggestions");
        }
        return mentor;
    }

    private boolean isFieldMentor(String email) {
        return userRepository.findFirstByEmailIgnoreCase(UserLookup.normalizeEmail(email))
                .map(user -> user instanceof FieldMentor)
                .orElse(false);
    }

    private void assertAdminCanModerate(String requesterEmail) {
        if (councilAccessService.isAdmin(requesterEmail)) {
            return;
        }
        throw new AccessDeniedException("Only admins can approve or reject bin suggestions");
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(normalized) || "ACCEPT".equals(normalized) || "ACCEPTED".equals(normalized)) {
            return "APPROVED";
        }
        if ("REJECT".equals(normalized)) {
            return "REJECTED";
        }
        return normalized;
    }

    private double[] resolveCoordinates(BinSuggestionCreateRequest request) {
        if (request.getLatitude() != null && request.getLongitude() != null) {
            return new double[] { request.getLatitude(), request.getLongitude() };
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            String[] parts = request.getLocation().split(",");
            if (parts.length >= 2) {
                try {
                    return new double[] {
                            Double.parseDouble(parts[0].trim()),
                            Double.parseDouble(parts[1].trim())
                    };
                } catch (NumberFormatException ignored) {
                }
            }
        }
        throw new IllegalArgumentException("Valid location coordinates are required");
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "general";
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
