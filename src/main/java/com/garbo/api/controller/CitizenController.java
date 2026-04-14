package com.garbo.api.controller;

import com.garbo.core.service.CitizenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {
    @Autowired
    private CitizenService citizenService;
}
