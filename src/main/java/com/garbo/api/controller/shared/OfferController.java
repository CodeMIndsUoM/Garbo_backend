package com.garbo.api.controller.shared;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.api.dto.collection.CancelOfferDto;
import com.garbo.api.dto.collection.ConfirmDto;
import com.garbo.api.dto.collection.OfferDto;
import com.garbo.core.service.shared.CollectionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Shared offer lifecycle endpoints used by both flows:
//   Citizen side -> accept / reject / confirm (+rate)
//   Collector side -> withdraw / cancel / start / complete
// This controller is intentionally shared so both sides operate on the same offer resource.
@RestController
@RequestMapping("/api/offers")
public class OfferController {
    private final CollectionRequestService collectionRequestService;

    public OfferController(CollectionRequestService collectionRequestService) {
        this.collectionRequestService = collectionRequestService;
    }

    // Citizen accepts a pending offer and request moves to ASSIGNED.
    @PostMapping("/{offerId}/accept")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<OfferDto>> accept(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.acceptOffer(offerId)));
    }

    // Citizen rejects a pending offer.
    @PostMapping("/{offerId}/reject")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<OfferDto>> reject(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.rejectOffer(offerId)));
    }

    // Citizen confirms completed collection and submits rating/feedback.
    @PostMapping("/{offerId}/confirm")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<OfferDto>> confirm(
            @PathVariable Long offerId,
            @Valid @RequestBody ConfirmDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.confirmCompletion(offerId, request)));
    }

    // Third-party collector withdraws a pending offer.
    @PostMapping("/{offerId}/withdraw")
    @PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
    public ResponseEntity<ApiResponse<OfferDto>> withdraw(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.withdrawOffer(offerId)));
    }

    // Third-party collector hides historical offers from my-jobs view.
    @PostMapping("/{offerId}/hide")
    @PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
    public ResponseEntity<ApiResponse<OfferDto>> hide(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.hideOfferFromCollectorList(offerId)));
    }

    // Third-party collector cancels accepted/in-progress work.
    // Ownership/role checks are enforced in service layer.
    @PostMapping("/{offerId}/cancel")
    public ResponseEntity<ApiResponse<OfferDto>> cancel(
            @PathVariable Long offerId,
            @Valid @RequestBody CancelOfferDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.cancelAcceptedOffer(offerId, request)));
    }

    // Third-party collector starts accepted work.
    @PostMapping("/{offerId}/start")
    @PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
    public ResponseEntity<ApiResponse<OfferDto>> start(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.startOffer(offerId)));
    }

    // Third-party collector completes work with location proof and optional photo/weight.
    @PostMapping(value = "/{offerId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
    public ResponseEntity<ApiResponse<OfferDto>> complete(
            @PathVariable Long offerId,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "weightKg", required = false) Double weightKg,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "notes", required = false) String notes) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        collectionRequestService.completeOfferWithPhoto(
                                offerId,
                                photo,
                                weightKg,
                                latitude,
                                longitude,
                                notes)));
    }
}
