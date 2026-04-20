package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.collection.CancelRequestDto;
import com.garbo.core.dto.collection.CreateOfferDto;
import com.garbo.core.dto.collection.OfferDto;
import com.garbo.core.dto.collection.RequestDetailDto;
import com.garbo.core.dto.collection.RequestSummaryDto;
import com.garbo.core.service.CollectionRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collection-requests")
public class CollectionRequestController {
    private final CollectionRequestService collectionRequestService;

    public CollectionRequestController(CollectionRequestService collectionRequestService) {
        this.collectionRequestService = collectionRequestService;
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailDto>> detail(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.getRequestDetail(requestId)));
    }

    @GetMapping("/{requestId}/offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> offers(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listOffersByRequest(requestId)));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<RequestSummaryDto>> cancel(
            @PathVariable Long requestId,
            @RequestBody(required = false) CancelRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.cancelRequest(requestId, request)));
    }

    @PostMapping("/{requestId}/offers")
    public ResponseEntity<ApiResponse<OfferDto>> sendOffer(
            @PathVariable Long requestId,
            @RequestBody CreateOfferDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collectionRequestService.sendOffer(requestId, request)));
    }
}
