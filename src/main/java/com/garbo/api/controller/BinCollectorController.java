package com.garbo.api.controller;

import com.garbo.core.service.BinCollectorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bincollectors")
public class BinCollectorController {
    final private BinCollectorService binCollectorService;

    public BinCollectorController(BinCollectorService binCollectorService) {
        this.binCollectorService = binCollectorService;
    }
}
