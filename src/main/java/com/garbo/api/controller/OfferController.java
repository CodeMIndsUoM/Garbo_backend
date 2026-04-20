package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.collection.CancelOfferDto;
import com.garbo.core.dto.collection.CompleteOfferDto;
import com.garbo.core.dto.collection.ConfirmDto;
import com.garbo.core.dto.collection.OfferDto;
import com.garbo.core.service.CollectionRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offers")
public class OfferController {
    private final CollectionRequestService collectionRequestService;

    public OfferController(CollectionRequestService collectionRequestService) {
        this.collectionRequestService = collectionRequestService;
    }

    @PostMapping("/{offerId}/accept")
    public ResponseEntity<ApiResponse<OfferDto>> accept(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.acceptOffer(offerId)));
    }

    @PostMapping("/{offerId}/reject")
    public ResponseEntity<ApiResponse<OfferDto>> reject(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.rejectOffer(offerId)));
    }

    @PostMapping("/{offerId}/confirm")
    public ResponseEntity<ApiResponse<OfferDto>> confirm(
            @PathVariable Long offerId,
            @RequestBody ConfirmDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.confirmCompletion(offerId, request)));
    }

    @PostMapping("/{offerId}/withdraw")
    public ResponseEntity<ApiResponse<OfferDto>> withdraw(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.withdrawOffer(offerId)));
    }

    @PostMapping("/{offerId}/cancel")
    public ResponseEntity<ApiResponse<OfferDto>> cancel(
            @PathVariable Long offerId,
            @RequestBody CancelOfferDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.cancelAcceptedOffer(offerId, request)));
    }

    @PostMapping("/{offerId}/start")
    public ResponseEntity<ApiResponse<OfferDto>> start(@PathVariable Long offerId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.startOffer(offerId)));
    }

    @PostMapping("/{offerId}/complete")
    public ResponseEntity<ApiResponse<OfferDto>> complete(
            @PathVariable Long offerId,
            @RequestBody CompleteOfferDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.completeOffer(offerId, request)));
    }
}
