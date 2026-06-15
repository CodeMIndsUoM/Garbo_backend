package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppVersionResponse {
    private String latestVersion;
    private String storeUrl;
    private String releaseNotes;
}
