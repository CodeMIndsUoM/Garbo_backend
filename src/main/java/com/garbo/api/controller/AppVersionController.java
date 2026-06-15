package com.garbo.api.controller;

import com.garbo.api.dto.AppVersionResponse;
import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.service.AppVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@CrossOrigin(origins = "*")
public class AppVersionController {

    private final AppVersionService appVersionService;

    public AppVersionController(AppVersionService appVersionService) {
        this.appVersionService = appVersionService;
    }

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<AppVersionResponse>> getLatestVersion(
            @RequestParam(defaultValue = "android") String platform) {
        return ResponseEntity.ok(ApiResponse.success(appVersionService.getVersion(platform)));
    }
}
