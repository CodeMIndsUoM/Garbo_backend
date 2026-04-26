package com.garbo.api.controller;

import com.garbo.core.dto.ApiResponse;
import com.garbo.core.dto.collection.CreateRequestDto;
import com.garbo.core.dto.collection.RequestSummaryDto;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.service.CollectionRequestService;
import com.garbo.core.service.CitizenService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// Citizen request flow:
//   Citizens create a collection request (with optional photo), see the
//   list of their requests, and act on offers via OfferController
//   (accept / reject / confirm-and-rate).
@RestController
@RequestMapping("/api/citizens")
@PreAuthorize("hasRole('CITIZEN')")
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
            @Valid @RequestBody CreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collectionRequestService.createRequest(citizenId, request)));
    }

    @GetMapping("/{citizenId}/collection-requests")
    public ResponseEntity<ApiResponse<List<RequestSummaryDto>>> listCollectionRequests(
            @PathVariable Long citizenId,
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listCitizenRequests(citizenId, status)));
    }

    @PostMapping(value = "/{citizenId}/request-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadRequestPhoto(
            @PathVariable Long citizenId,
            @RequestParam("photo") MultipartFile photo) {
        String url = collectionRequestService.uploadCitizenRequestPhoto(citizenId, photo);
        return ResponseEntity.ok(ApiResponse.success(Map.of("photoUrl", url)));
    }
}
