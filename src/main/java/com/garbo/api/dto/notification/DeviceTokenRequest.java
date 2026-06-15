package com.garbo.api.dto.notification;

import lombok.Data;

@Data
public class DeviceTokenRequest {
    private String token;
    private String platform;
}
