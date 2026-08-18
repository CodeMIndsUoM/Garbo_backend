package com.garbo.core.notification;

public enum NotificationType {
    BIN_ASSIGNED("Bin assigned", "A new bin has been assigned to you.", "HIGH"),
    BIN_SUGGESTION_RESOLVED("Suggestion update", "Your bin suggestion has been reviewed.", "NORMAL"),
    ROUTE_ASSIGNED("Route assigned", "A new collection route has been assigned to you.", "HIGH"),
    ROUTE_UPDATED("Route updated", "Your assigned route has been updated.", "NORMAL"),
    MARKETPLACE_REQUEST_UPDATED("Request update", "Your collection request status changed.", "NORMAL"),
    MARKETPLACE_OFFER_UPDATED("Offer update", "A marketplace offer status changed.", "NORMAL"),
    REGISTRATION_APPROVED("Registration approved", "Your registration has been approved.", "HIGH"),
    REGISTRATION_REJECTED("Registration rejected", "Your registration was not approved.", "HIGH"),
    COMPLAINT_SUBMITTED("New complaint", "A citizen submitted a new complaint.", "HIGH"),
    COMPLAINT_STATUS_UPDATED("Complaint update", "Your complaint status has changed.", "NORMAL"),
    COMPLAINT_ASSIGNED("Special Task Assigned", "You have been assigned a new special task (complaint).", "HIGH"),
    BIN_SUGGESTION_SUBMITTED("New bin suggestion", "A field mentor suggested a new bin location.", "NORMAL"),
    BIN_DISCREPANCY_REPORTED("Bin discrepancy", "A status discrepancy was reported on a bin.", "HIGH"),
    EVENT_SUGGESTION_SUBMITTED("New event suggestion", "A citizen suggested a new community event.", "NORMAL"),
    EVENT_SUGGESTION_RESOLVED("Event suggestion update", "Your event suggestion has been reviewed.", "NORMAL"),
    THIRD_PARTY_REGISTRATION_PENDING("New registration", "A third-party collector registration is pending review.", "NORMAL"),
    STAFF_ACCOUNT_CREATED("Account created", "Your staff account has been created.", "LOW"),
    ADMIN_MESSAGE("Message from admin", "You have a new message from your council admin.", "NORMAL");

    private final String defaultTitle;
    private final String defaultBody;
    private final String defaultPriority;

    NotificationType(String defaultTitle, String defaultBody, String defaultPriority) {
        this.defaultTitle = defaultTitle;
        this.defaultBody = defaultBody;
        this.defaultPriority = defaultPriority;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public String getDefaultBody() {
        return defaultBody;
    }

    public String getDefaultPriority() {
        return defaultPriority;
    }
}
