package com.garbo.core.service;

import com.garbo.api.dto.EventCreateRequest;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.Event;
import com.garbo.core.entity.User;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.EventRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.notification.NotificationPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final CouncilAccessService councilAccessService;
    private final NotificationPublisher notificationPublisher;

    public EventService(EventRepository eventRepository,
            UserRepository userRepository,
            CitizenRepository citizenRepository,
            CouncilAccessService councilAccessService,
            NotificationPublisher notificationPublisher) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.citizenRepository = citizenRepository;
        this.councilAccessService = councilAccessService;
        this.notificationPublisher = notificationPublisher;
    }

    public Event createEvent(EventCreateRequest request, String citizenEmail) {
        return createEventInternal(request, citizenEmail, false);
    }

    public Event suggestEvent(EventCreateRequest request, String citizenEmail) {
        return createEventInternal(request, citizenEmail, true);
    }

    private Event createEventInternal(EventCreateRequest request, String requesterEmail, boolean suggestedByCitizen) {
        User requester = UserLookup.requireUser(userRepository, requesterEmail);
        String council = resolveCouncilForRequest(request, requesterEmail, requester);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("Event title is required");
        }
        if (request.getEventDate() == null) {
            throw new RuntimeException("Event date is required");
        }

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setLocation(request.getLocation());
        event.setCategory(request.getCategory());
        event.setImageUrl(request.getImageUrl());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setCouncil(council);
        event.setOrganizerCitizen(requester);
        event.setStatus(suggestedByCitizen ? "PENDING_APPROVAL" : "ACTIVE");
        event.setEnrolledCount(0);
        Event saved = eventRepository.save(event);
        if (suggestedByCitizen) {
            notificationPublisher.eventSuggestionSubmitted(saved);
        }
        return saved;
    }

    private String resolveCouncilForRequest(EventCreateRequest request, String requesterEmail, User requester) {
        Optional<Citizen> citizenOpt = UserLookup.findCitizen(citizenRepository, requesterEmail);
        boolean isCitizen = citizenOpt.isPresent()
                && !councilAccessService.isSuperAdmin(requesterEmail)
                && !councilAccessService.isAdmin(requesterEmail);

        if (isCitizen) {
            String council = UserLookup.resolveCitizenCouncil(citizenOpt.get());
            if (council == null || council.isBlank()) {
                throw new RuntimeException("Citizen council is required before creating events");
            }
            return council;
        }

        String council = request.getCouncil();
        if (council == null || council.isBlank()) {
            council = councilAccessService.resolveCouncilForEmail(requesterEmail).orElse(null);
        }
        if (council == null || council.isBlank()) {
            throw new RuntimeException("Council is required when creating an event");
        }
        return council.trim();
    }

    public List<Event> getMyEvents(String citizenEmail) {
        User citizen = UserLookup.requireUser(userRepository, citizenEmail);
        return eventRepository.findByOrganizerCitizenOrderByCreatedAtDesc(citizen);
    }

    public List<Event> getVisibleEvents(String requesterEmail) {
        return getVisibleEvents(requesterEmail, null);
    }

    public List<Event> getVisibleEvents(String requesterEmail, String councilFromToken) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return eventRepository.findAll().stream()
                    .filter(this::isVisibleEventStatus)
                    .toList();
        }
        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (councilOpt.isEmpty() && councilFromToken != null && !councilFromToken.isBlank()) {
            councilOpt = Optional.of(councilFromToken.trim());
        }
        if (councilOpt.isEmpty()) {
            return List.of();
        }
        return eventRepository.findByCouncilIgnoreCaseOrderByEventDateAsc(councilOpt.get()).stream()
                .filter(this::isVisibleEventStatus)
                .toList();
    }

    private boolean isVisibleEventStatus(Event event) {
        if (event.getStatus() == null) {
            return false;
        }
        String status = event.getStatus().trim().toUpperCase(Locale.ROOT);
        return status.equals("ACTIVE") || status.equals("APPROVED");
    }

    public Event enrollInEvent(Long eventId, String requesterEmail) {
        Event event = getVisibleEventById(eventId, requesterEmail);
        int current = event.getEnrolledCount() == null ? 0 : event.getEnrolledCount();
        Integer max = event.getMaxParticipants();
        if (max != null && max > 0 && current >= max) {
            throw new RuntimeException("Event is already full");
        }
        event.setEnrolledCount(current + 1);
        return eventRepository.save(event);
    }

    public Event updateEventStatus(Long eventId, String status, String requesterEmail) {
        Event event = getVisibleEventById(eventId, requesterEmail);
        event.setStatus(status);
        return eventRepository.save(event);
    }

    public List<Event> getPendingSuggestions(String requesterEmail) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return eventRepository.findAll().stream()
                    .filter(event -> event.getStatus() != null
                            && "PENDING_APPROVAL".equalsIgnoreCase(event.getStatus().trim()))
                    .toList();
        }
        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (councilOpt.isEmpty()) {
            return List.of();
        }
        return eventRepository.findByCouncilIgnoreCaseAndStatusOrderByCreatedAtDesc(
                councilOpt.get(),
                "PENDING_APPROVAL");
    }

    public Event approveSuggestion(Long eventId, String requesterEmail) {
        Event event = getVisibleEventById(eventId, requesterEmail);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(event.getStatus())) {
            throw new RuntimeException("Only pending suggestions can be approved");
        }
        event.setStatus("APPROVED");
        event.setRejectionReason(null);
        Event saved = eventRepository.save(event);
        notificationPublisher.eventSuggestionResolved(saved, true);
        return saved;
    }

    public Event rejectSuggestion(Long eventId, String reason, String requesterEmail) {
        Event event = getVisibleEventById(eventId, requesterEmail);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(event.getStatus())) {
            throw new RuntimeException("Only pending suggestions can be rejected");
        }
        event.setStatus("REJECTED");
        event.setRejectionReason(reason == null ? null : reason.trim());
        Event saved = eventRepository.save(event);
        notificationPublisher.eventSuggestionResolved(saved, false);
        return saved;
    }

    private Event getVisibleEventById(Long eventId, String requesterEmail) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
        }
        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (councilOpt.isEmpty()) {
            throw new AccessDeniedException("No council assigned for current user");
        }
        return eventRepository.findByIdAndCouncilIgnoreCase(eventId, councilOpt.get())
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }
}
