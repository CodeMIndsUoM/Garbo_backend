package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.collection.CreateRequestDto;
import com.garbo.core.dto.collection.RequestSummaryDto;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.service.CollectionRequestService;
import com.garbo.core.service.CitizenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {
    @SuppressWarnings("unused")
    private final CitizenService citizenService;
    private final CollectionRequestService collectionRequestService;

    public CitizenController(CitizenService citizenService,
                             CollectionRequestService collectionRequestService) {
        this.citizenService = citizenService;
        this.collectionRequestService = collectionRequestService;
    }

    @PostMapping("/{citizenId}/collection-requests")
    public ResponseEntity<ApiResponse<RequestSummaryDto>> createCollectionRequest(
            @PathVariable Long citizenId,
            @RequestBody CreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collectionRequestService.createRequest(citizenId, request)));
    }

    @GetMapping("/{citizenId}/collection-requests")
    public ResponseEntity<ApiResponse<List<RequestSummaryDto>>> listCollectionRequests(
            @PathVariable Long citizenId,
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listCitizenRequests(citizenId, status)));
    }
}
