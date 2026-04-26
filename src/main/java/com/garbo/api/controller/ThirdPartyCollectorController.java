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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garbo.core.entity.ThirdPartyCollector;

import java.util.Map;
import java.util.List;

// Third-party collector flow:
//   Collectors browse the open-request feed, send offers (via
//   CollectionRequestController), and manage their own offers and active
//   jobs through OfferController (start / complete / withdraw / cancel).
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

    @PostMapping("/{collectorId}/my-offers/hide")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> hideMyOffers(
            @PathVariable Long collectorId,
            @RequestParam(required = false) List<OfferStatus> statuses) {
        int hiddenCount = collectionRequestService.hideOffersFromCollectorList(collectorId, statuses);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hiddenCount", hiddenCount)));
    }

    @GetMapping("/{collectorId}/active-jobs")
    public ResponseEntity<ApiResponse<List<OfferDto>>> activeJobs(@PathVariable Long collectorId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listActiveJobs(collectorId)));
    }

    @GetMapping("/{collectorId}/dashboard")
    public ResponseEntity<ApiResponse<com.garbo.core.dto.collection.CollectorDashboardDto>> getDashboard(
            @PathVariable Long collectorId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.getCollectorDashboard(collectorId)));
    }

    @GetMapping("/{collectorId}/profile")
    public ResponseEntity<ApiResponse<ThirdPartyCollector>> getProfile(@PathVariable Long collectorId) {
        return thirdPartyCollectorService.getProfile(collectorId)
                .map(collector -> ResponseEntity.ok(ApiResponse.success(collector)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{collectorId}/profile")
    public ResponseEntity<ApiResponse<ThirdPartyCollector>> updateProfile(
            @PathVariable Long collectorId,
            @RequestBody ThirdPartyCollector updatedDetails) {
        ThirdPartyCollector updated = thirdPartyCollectorService.updateProfile(collectorId, updatedDetails);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
