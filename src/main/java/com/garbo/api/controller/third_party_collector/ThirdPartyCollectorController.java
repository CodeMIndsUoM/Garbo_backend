package com.garbo.api.controller.third_party_collector;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.api.dto.collection.OfferDto;
import com.garbo.api.dto.collection.RequestSummaryDto;
import com.garbo.core.enums.OfferStatus;
import com.garbo.core.service.shared.CollectionRequestService;
import com.garbo.core.service.third_party_collector.ThirdPartyCollectorService;
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

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/thirdpartycollectors")
@PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
public class ThirdPartyCollectorController {
    private final ThirdPartyCollectorService thirdPartyCollectorService;
    private final CollectionRequestService collectionRequestService;

    public ThirdPartyCollectorController(ThirdPartyCollectorService thirdPartyCollectorService,
            CollectionRequestService collectionRequestService) {
        this.thirdPartyCollectorService = thirdPartyCollectorService;
        this.collectionRequestService = collectionRequestService;
    }

    // Collector feed of OPEN citizen requests (optional geo query for proximity).
    @GetMapping("/{collectorId}/feed")
    public ResponseEntity<ApiResponse<List<RequestSummaryDto>>> browseFeed(
            @PathVariable Long collectorId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.browseFeed(collectorId, lat, lng)));
    }

    // Collector's own offers list used by My Jobs screen.
    @GetMapping("/{collectorId}/my-offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> myOffers(
            @PathVariable Long collectorId,
            @RequestParam(required = false) OfferStatus status) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listMyOffers(collectorId, status)));
    }

    // Bulk hide rejected/withdrawn/cancelled/completed offers from collector list.
    @PostMapping("/{collectorId}/my-offers/hide")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> hideMyOffers(
            @PathVariable Long collectorId,
            @RequestParam(required = false) List<OfferStatus> statuses) {
        int hiddenCount = collectionRequestService.hideOffersFromCollectorList(collectorId, statuses);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hiddenCount", hiddenCount)));
    }

    // Collector jobs currently in ACCEPTED/IN_PROGRESS states.
    @GetMapping("/{collectorId}/active-jobs")
    public ResponseEntity<ApiResponse<List<OfferDto>>> activeJobs(@PathVariable Long collectorId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listActiveJobs(collectorId)));
    }

    @GetMapping("/{collectorId}/dashboard")
    public ResponseEntity<ApiResponse<com.garbo.api.dto.collection.CollectorDashboardDto>> getDashboard(
            @PathVariable Long collectorId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.getCollectorDashboard(collectorId)));
    }

    // Collector profile for app profile view/edit.
    @GetMapping("/{collectorId}/profile")
    public ResponseEntity<ApiResponse<ThirdPartyCollector>> getProfile(@PathVariable Long collectorId) {
        return thirdPartyCollectorService.getProfile(collectorId)
                .map(collector -> ResponseEntity.ok(ApiResponse.success(collector)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Updates collector profile fields.
    @PutMapping("/{collectorId}/profile")
    public ResponseEntity<ApiResponse<ThirdPartyCollector>> updateProfile(
            @PathVariable Long collectorId,
            @RequestBody ThirdPartyCollector updatedDetails) {
        ThirdPartyCollector updated = thirdPartyCollectorService.updateProfile(collectorId, updatedDetails);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{collectorId}/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @PathVariable Long collectorId,
            @RequestParam("photo") MultipartFile photo) {
        ThirdPartyCollector updated = thirdPartyCollectorService.uploadAvatar(collectorId, photo);
        return ResponseEntity.ok(ApiResponse.success(Map.of("avatarUrl", updated.getAvatarUrl())));
    }

    @DeleteMapping("/{collectorId}/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> removeAvatar(@PathVariable Long collectorId) {
        thirdPartyCollectorService.removeAvatar(collectorId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Avatar removed")));
    }
}
