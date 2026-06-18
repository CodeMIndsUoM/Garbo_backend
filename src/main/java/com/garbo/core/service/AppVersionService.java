package com.garbo.core.service;

import com.garbo.api.dto.AppVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppVersionService {

    @Value("${garbo.app.android.latest-version:1.0.0}")
    private String androidLatestVersion;

    @Value("${garbo.app.android.store-url:}")
    private String androidStoreUrl;

    @Value("${garbo.app.android.release-notes:}")
    private String androidReleaseNotes;

    @Value("${garbo.app.ios.latest-version:1.0.0}")
    private String iosLatestVersion;

    @Value("${garbo.app.ios.store-url:}")
    private String iosStoreUrl;

    @Value("${garbo.app.ios.release-notes:}")
    private String iosReleaseNotes;

    public AppVersionResponse getVersion(String platform) {
        if ("ios".equalsIgnoreCase(platform)) {
            return new AppVersionResponse(
                    iosLatestVersion,
                    iosStoreUrl,
                    blankToNull(iosReleaseNotes));
        }
        return new AppVersionResponse(
                androidLatestVersion,
                androidStoreUrl,
                blankToNull(androidReleaseNotes));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
