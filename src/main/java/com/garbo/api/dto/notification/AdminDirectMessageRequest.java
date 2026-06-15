package com.garbo.api.dto.notification;

import lombok.Data;

@Data
public class AdminDirectMessageRequest {

    private Long empId;
    private String title;
    private String body;
    private String priority;
}
