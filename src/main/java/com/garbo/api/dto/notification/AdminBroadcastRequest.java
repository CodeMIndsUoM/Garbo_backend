package com.garbo.api.dto.notification;

import lombok.Data;

import java.util.List;

@Data
public class AdminBroadcastRequest {

    /** Required headline shown in the inbox and push notification. */
    private String title;

    /** Required message body. One-way — recipients cannot reply. */
    private String body;

    /**
     * ALL_INTERNAL, FIELD_MENTOR, or BIN_COLLECTOR.
     * Ignored when {@link #recipientIds} is non-empty.
     */
    private String audience;

    /** Superadmin council filter; council admins always use their own council. */
    private String council;

    /** Optional explicit staff empIds (field mentors / bin collectors only). */
    private List<Long> recipientIds;

    /** NORMAL (default) or HIGH. */
    private String priority;
}
