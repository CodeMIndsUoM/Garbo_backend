package com.garbo.core.service.notification;

import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InternalUserRecipientResolver {

    public static final String AUDIENCE_ALL = "ALL_INTERNAL";
    public static final String AUDIENCE_FIELD_MENTOR = "FIELD_MENTOR";
    public static final String AUDIENCE_BIN_COLLECTOR = "BIN_COLLECTOR";

    private final FieldMentorRepository fieldMentorRepository;
    private final BinCollectorRepository binCollectorRepository;
    private final UserRepository userRepository;

    public InternalUserRecipientResolver(
            FieldMentorRepository fieldMentorRepository,
            BinCollectorRepository binCollectorRepository,
            UserRepository userRepository
    ) {
        this.fieldMentorRepository = fieldMentorRepository;
        this.binCollectorRepository = binCollectorRepository;
        this.userRepository = userRepository;
    }

    public List<Long> resolveByAudience(String audience, String council) {
        String normalizedAudience = normalizeAudience(audience);
        Set<Long> ids = new LinkedHashSet<>();

        if (AUDIENCE_ALL.equals(normalizedAudience) || AUDIENCE_FIELD_MENTOR.equals(normalizedAudience)) {
            ids.addAll(listVisibleMentorIds(council));
        }
        if (AUDIENCE_ALL.equals(normalizedAudience) || AUDIENCE_BIN_COLLECTOR.equals(normalizedAudience)) {
            ids.addAll(listVisibleCollectorIds(council));
        }

        return new ArrayList<>(ids);
    }

    public List<Long> resolveExplicitRecipients(
            List<Long> recipientIds,
            String councilScope,
            boolean allowCrossCouncil
    ) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = new LinkedHashSet<>();
        for (Long recipientId : recipientIds) {
            if (recipientId == null) {
                continue;
            }
            userRepository.findById(recipientId).ifPresent(user -> {
                if (isVisibleInternalUser(user)
                        && (allowCrossCouncil || isInCouncilScope(user, councilScope))) {
                    ids.add(user.getEmpId());
                }
            });
        }
        return new ArrayList<>(ids);
    }

    private List<Long> listVisibleMentorIds(String council) {
        List<FieldMentor> mentors = council == null || council.isBlank()
                ? fieldMentorRepository.findAll()
                : fieldMentorRepository.findByAssignedCouncil(council.trim());
        return mentors.stream()
                .filter(this::isVisibleInternalUser)
                .map(FieldMentor::getEmpId)
                .toList();
    }

    private List<Long> listVisibleCollectorIds(String council) {
        List<BinCollector> collectors = council == null || council.isBlank()
                ? binCollectorRepository.findAll()
                : binCollectorRepository.findByAssignedCouncil(council.trim());
        return collectors.stream()
                .filter(this::isVisibleInternalUser)
                .map(BinCollector::getEmpId)
                .toList();
    }

    private boolean isVisibleInternalUser(User user) {
        if (user instanceof FieldMentor mentor) {
            return !Boolean.TRUE.equals(mentor.getAdminHidden());
        }
        if (user instanceof BinCollector collector) {
            return !Boolean.TRUE.equals(collector.getAdminHidden());
        }
        return false;
    }

    private boolean isVisibleInternalUser(FieldMentor mentor) {
        return mentor != null && !Boolean.TRUE.equals(mentor.getAdminHidden());
    }

    private boolean isVisibleInternalUser(BinCollector collector) {
        return collector != null && !Boolean.TRUE.equals(collector.getAdminHidden());
    }

    private boolean isInCouncilScope(User user, String councilScope) {
        if (councilScope == null || councilScope.isBlank()) {
            return true;
        }
        String assigned = null;
        if (user instanceof FieldMentor mentor) {
            assigned = mentor.getAssignedCouncil();
        } else if (user instanceof BinCollector collector) {
            assigned = collector.getAssignedCouncil();
        }
        return assigned != null && assigned.trim().equalsIgnoreCase(councilScope.trim());
    }

    public String normalizeAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            return AUDIENCE_ALL;
        }
        String normalized = audience.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL", "ALL_INTERNAL", "INTERNAL", "STAFF" -> AUDIENCE_ALL;
            case "FIELD_MENTOR", "FIELD_MENTORS", "FIELD_STAFF", "MENTOR", "MENTORS" -> AUDIENCE_FIELD_MENTOR;
            case "BIN_COLLECTOR", "BIN_COLLECTORS", "COLLECTOR", "COLLECTORS" -> AUDIENCE_BIN_COLLECTOR;
            default -> normalized;
        };
    }
}
