package com.garbo.api.controller.shared;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.api.dto.collection.CancelRequestDto;
import com.garbo.api.dto.collection.CreateOfferDto;
import com.garbo.api.dto.collection.OfferDto;
import com.garbo.api.dto.collection.RequestDetailDto;
import com.garbo.api.dto.collection.RequestSummaryDto;
import com.garbo.core.service.shared.CollectionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Shared collection-request endpoints used by both citizen and third-party collector flows.
// Ownership split in current architecture:
//   - CitizenController: create/list/upload request-photo
//   - CollectionRequestController: detail/cancel/send-offer entry points
//   - OfferController: offer lifecycle actions
@RestController
@RequestMapping("/api/collection-requests")
public class CollectionRequestController {
    private final CollectionRequestService collectionRequestService;

    public CollectionRequestController(CollectionRequestService collectionRequestService) {
        this.collectionRequestService = collectionRequestService;
    }

    // Returns full request view used by citizen details and collector my-jobs
    // context.
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailDto>> detail(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.getRequestDetail(requestId)));
    }

    // Returns offers for a request; currently optional in Flutter (detail already
    // includes offers).
    @GetMapping("/{requestId}/offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> offers(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listOffersByRequest(requestId)));
    }

    // Citizen-side cancel endpoint for request-level cancellation before/around
    // assignment stage.
    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<RequestSummaryDto>> cancel(
            @PathVariable Long requestId,
            @RequestBody(required = false) CancelRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.cancelRequest(requestId, request)));
    }

    // Third-party collector submits an offer for an open citizen request.
    @PostMapping("/{requestId}/offers")
    @PreAuthorize("hasRole('THIRD_PARTY_COLLECTOR')")
    public ResponseEntity<ApiResponse<OfferDto>> sendOffer(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateOfferDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collectionRequestService.sendOffer(requestId, request)));
    }
}
