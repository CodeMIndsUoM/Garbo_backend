package com.garbo.api.controller;

import com.garbo.core.service.CitizenService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {
    @SuppressWarnings("unused")
    private final CitizenService citizenService;

    public CitizenController(CitizenService citizenService) {
        this.citizenService = citizenService;
    }
}
