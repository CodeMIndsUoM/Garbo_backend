package com.garbo.api.controller;

import com.garbo.core.service.BinCollectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bincollectors")
public class BinCollectorController {
    @Autowired
    private BinCollectorService binCollectorService;
}
