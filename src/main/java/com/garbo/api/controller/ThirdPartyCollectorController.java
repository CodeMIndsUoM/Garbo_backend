package com.garbo.api.controller;

import com.garbo.core.service.ThirdPartyCollectorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thirdpartycollectors")
public class ThirdPartyCollectorController {
    @Autowired
    private ThirdPartyCollectorService thirdPartyCollectorService;
}
