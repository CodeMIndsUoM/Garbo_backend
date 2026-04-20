package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.collection.OfferDto;
import com.garbo.core.dto.collection.RequestSummaryDto;
import com.garbo.core.enums.OfferStatus;
import com.garbo.core.service.CollectionRequestService;
import com.garbo.core.service.ThirdPartyCollectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/thirdpartycollectors")
@PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
public class ThirdPartyCollectorController {
    @SuppressWarnings("unused")
    private final ThirdPartyCollectorService thirdPartyCollectorService;
    private final CollectionRequestService collectionRequestService;

    public ThirdPartyCollectorController(ThirdPartyCollectorService thirdPartyCollectorService,
                                         CollectionRequestService collectionRequestService) {
        this.thirdPartyCollectorService = thirdPartyCollectorService;
        this.collectionRequestService = collectionRequestService;
    }

    @GetMapping("/{collectorId}/feed")
    public ResponseEntity<ApiResponse<List<RequestSummaryDto>>> browseFeed(
            @PathVariable Long collectorId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.browseFeed(collectorId, lat, lng)));
    }

    @GetMapping("/{collectorId}/my-offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> myOffers(
            @PathVariable Long collectorId,
            @RequestParam(required = false) OfferStatus status) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listMyOffers(collectorId, status)));
    }

    @GetMapping("/{collectorId}/active-jobs")
    public ResponseEntity<ApiResponse<List<OfferDto>>> activeJobs(@PathVariable Long collectorId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listActiveJobs(collectorId)));
    }
}
