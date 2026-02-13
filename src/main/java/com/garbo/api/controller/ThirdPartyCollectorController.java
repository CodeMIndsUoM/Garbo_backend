package com.garbo.api.controller;

import com.garbo.core.service.ThirdPartyCollectorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thirdpartycollectors")
public class ThirdPartyCollectorController {
    private final ThirdPartyCollectorService thirdPartyCollectorService;

    public ThirdPartyCollectorController(ThirdPartyCollectorService thirdPartyCollectorService) {
        this.thirdPartyCollectorService = thirdPartyCollectorService;
    }
}
