package com.garbo.core.service.notification;

import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.Event;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.notification.NotificationContext;
import com.garbo.core.notification.NotificationType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationPublisher {

    private final NotificationService notificationService;
    private final AdminNotificationRouter adminNotificationRouter;

    public NotificationPublisher(
            NotificationService notificationService,
            AdminNotificationRouter adminNotificationRouter
    ) {
        this.notificationService = notificationService;
        this.adminNotificationRouter = adminNotificationRouter;
    }

    public void binAssigned(FieldMentor mentor, Bin bin) {
        if (mentor == null || mentor.getEmpId() == null || bin == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("binId", bin.getId());
        data.put("binCode", bin.getBinCode());
        data.put("council", bin.getCouncil());
        notificationService.notifyUser(
                mentor.getEmpId(),
                NotificationType.BIN_ASSIGNED,
                NotificationContext.of(
                        NotificationType.BIN_ASSIGNED,
                        "Bin assigned",
                        "Bin " + safe(bin.getBinCode()) + " has been assigned to you.",
                        data,
                        "bin-assigned-" + bin.getId() + "-" + mentor.getEmpId()
                )
        );
    }

    public void binDiscrepancyReported(Bin bin, Long reportId) {
        if (bin == null || bin.getCouncil() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("binId", bin.getId());
        data.put("binCode", bin.getBinCode());
        data.put("council", bin.getCouncil());
        data.put("reportId", reportId);
        NotificationContext context = NotificationContext.of(
                NotificationType.BIN_DISCREPANCY_REPORTED,
                "Status discrepancy",
                "Discrepancy reported on bin " + safe(bin.getBinCode()) + ".",
                data,
                "bin-discrepancy-" + bin.getId() + "-" + reportId
        );
        notifyCouncilAdmins(bin.getCouncil(), NotificationType.BIN_DISCREPANCY_REPORTED, context);
    }

    public void binSuggestionSubmitted(BinSuggestion suggestion) {
        if (suggestion == null || suggestion.getCouncil() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("suggestionId", suggestion.getId());
        data.put("council", suggestion.getCouncil());
        data.put("mentorId", suggestion.getMentorId());
        NotificationContext context = NotificationContext.of(
                NotificationType.BIN_SUGGESTION_SUBMITTED,
                "New bin suggestion",
                suggestion.getMentorName() + " suggested a new bin location.",
                data,
                "bin-suggestion-submitted-" + suggestion.getId()
        );
        notifyCouncilAdmins(suggestion.getCouncil(), NotificationType.BIN_SUGGESTION_SUBMITTED, context);
    }

    public void binSuggestionResolved(BinSuggestion suggestion) {
        if (suggestion == null || suggestion.getMentorId() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("suggestionId", suggestion.getId());
        data.put("status", suggestion.getStatus());
        data.put("createdBinId", suggestion.getCreatedBinId());
        notificationService.notifyUser(
                suggestion.getMentorId(),
                NotificationType.BIN_SUGGESTION_RESOLVED,
                NotificationContext.of(
                        NotificationType.BIN_SUGGESTION_RESOLVED,
                        "Suggestion " + safe(suggestion.getStatus()),
                        "Your bin suggestion was " + safe(suggestion.getStatus()).toLowerCase() + ".",
                        data,
                        "bin-suggestion-resolved-" + suggestion.getId()
                )
        );
    }

    public void routeAssigned(Long collectorEmpId, String sessionId, int binCount) {
        if (collectorEmpId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("binCount", binCount);
        notificationService.notifyUser(
                collectorEmpId,
                NotificationType.ROUTE_ASSIGNED,
                NotificationContext.of(
                        NotificationType.ROUTE_ASSIGNED,
                        "Route assigned",
                        "A route with " + binCount + " bin(s) has been assigned to you.",
                        data,
                        "route-assigned-" + sessionId + "-" + collectorEmpId
                )
        );
    }

    public void marketplaceRequestUpdated(Long userId, Long requestId, String status) {
        if (userId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("status", status);
        notificationService.notifyUser(
                userId,
                NotificationType.MARKETPLACE_REQUEST_UPDATED,
                NotificationContext.of(
                        NotificationType.MARKETPLACE_REQUEST_UPDATED,
                        "Request updated",
                        "Your collection request is now " + safe(status) + ".",
                        data,
                        "request-updated-" + requestId + "-" + userId + "-" + safe(status)
                )
        );
    }

    public void marketplaceOfferUpdated(Long userId, Long requestId, Long offerId, String status) {
        if (userId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("offerId", offerId);
        data.put("status", status);
        notificationService.notifyUser(
                userId,
                NotificationType.MARKETPLACE_OFFER_UPDATED,
                NotificationContext.of(
                        NotificationType.MARKETPLACE_OFFER_UPDATED,
                        "Offer updated",
                        "A marketplace offer is now " + safe(status) + ".",
                        data,
                        "offer-updated-" + offerId + "-" + userId + "-" + safe(status)
                )
        );
    }

    public void complaintSubmitted(Complaint complaint) {
        if (complaint == null || complaint.getCouncil() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("complaintId", complaint.getId());
        data.put("council", complaint.getCouncil());
        data.put("title", complaint.getTitle());
        NotificationContext context = NotificationContext.of(
                NotificationType.COMPLAINT_SUBMITTED,
                "New complaint",
                "New complaint: " + safe(complaint.getTitle()),
                data,
                "complaint-submitted-" + complaint.getId()
        );
        notifyCouncilAdmins(complaint.getCouncil(), NotificationType.COMPLAINT_SUBMITTED, context);
    }

    public void complaintStatusUpdated(Complaint complaint) {
        if (complaint == null || complaint.getCitizenId() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("complaintId", complaint.getId());
        data.put("status", complaint.getStatus());
        notificationService.notifyUser(
                complaint.getCitizenId(),
                NotificationType.COMPLAINT_STATUS_UPDATED,
                NotificationContext.of(
                        NotificationType.COMPLAINT_STATUS_UPDATED,
                        "Complaint update",
                        "Your complaint is now " + safe(complaint.getStatus()) + ".",
                        data,
                        "complaint-status-" + complaint.getId() + "-" + safe(complaint.getStatus())
                )
        );
    }

    public void eventSuggestionSubmitted(Event event) {
        if (event == null || event.getCouncil() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getId());
        data.put("council", event.getCouncil());
        data.put("title", event.getTitle());
        NotificationContext context = NotificationContext.of(
                NotificationType.EVENT_SUGGESTION_SUBMITTED,
                "New event suggestion",
                "New event suggestion: " + safe(event.getTitle()),
                data,
                "event-suggestion-submitted-" + event.getId()
        );
        notifyCouncilAdmins(event.getCouncil(), NotificationType.EVENT_SUGGESTION_SUBMITTED, context);
    }

    public void eventSuggestionResolved(Event event, boolean approved) {
        if (event == null || event.getOrganizerCitizen() == null) {
            return;
        }
        Long citizenId = event.getOrganizerCitizen().getEmpId();
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getId());
        data.put("status", event.getStatus());
        data.put("approved", approved);
        notificationService.notifyUser(
                citizenId,
                NotificationType.EVENT_SUGGESTION_RESOLVED,
                NotificationContext.of(
                        NotificationType.EVENT_SUGGESTION_RESOLVED,
                        approved ? "Event approved" : "Event rejected",
                        approved
                                ? "Your event suggestion was approved."
                                : "Your event suggestion was rejected.",
                        data,
                        "event-suggestion-resolved-" + event.getId()
                )
        );
    }

    public void thirdPartyRegistrationPending(ThirdPartyCollector collector) {
        if (collector == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("collectorId", collector.getEmpId());
        data.put("company", collector.getCompany());
        NotificationContext context = NotificationContext.of(
                NotificationType.THIRD_PARTY_REGISTRATION_PENDING,
                "Registration pending",
                collector.getEmpName() + " submitted a collector registration.",
                data,
                "third-party-pending-" + collector.getEmpId()
        );
        List<Long> recipients = adminNotificationRouter.resolveSuperAdminOnly();
        notificationService.notifyUsers(recipients, NotificationType.THIRD_PARTY_REGISTRATION_PENDING, context);
    }

    public void registrationApproved(ThirdPartyCollector collector) {
        if (collector == null || collector.getEmpId() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("collectorId", collector.getEmpId());
        notificationService.notifyUser(
                collector.getEmpId(),
                NotificationType.REGISTRATION_APPROVED,
                NotificationContext.of(
                        NotificationType.REGISTRATION_APPROVED,
                        data,
                        "registration-approved-" + collector.getEmpId()
                )
        );
    }

    public void registrationRejected(ThirdPartyCollector collector) {
        if (collector == null || collector.getEmpId() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("collectorId", collector.getEmpId());
        notificationService.notifyUser(
                collector.getEmpId(),
                NotificationType.REGISTRATION_REJECTED,
                NotificationContext.of(
                        NotificationType.REGISTRATION_REJECTED,
                        data,
                        "registration-rejected-" + collector.getEmpId()
                )
        );
    }

    private void notifyCouncilAdmins(String council, NotificationType type, NotificationContext context) {
        List<Long> recipients = adminNotificationRouter.resolveCouncilAndSuperAdminUserIds(council);
        notificationService.notifyUsers(recipients, type, context);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
