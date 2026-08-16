package com.garbo.api.controller.citizen;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.api.dto.collection.CreateRequestDto;
import com.garbo.api.dto.collection.RequestSummaryDto;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.service.shared.CollectionRequestService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

// Citizen request flow:
//   Citizens create a collection request (with optional photo), see the
//   list of their requests, and act on offers via OfferController
//   (accept / reject / confirm-and-rate).
// This controller owns citizen-scoped request creation/list/photo upload APIs.
@RestController
@RequestMapping("/api/citizens")
@PreAuthorize("hasRole('CITIZEN')")
public class CitizenController {
    private final CollectionRequestService collectionRequestService;

    public CitizenController(CollectionRequestService collectionRequestService) {
        this.collectionRequestService = collectionRequestService;
    }

    private void enforceCitizenOwnership(Long citizenId) {
        if (!com.garbo.core.service.CurrentUserService.isCurrentUserOrAdmin(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this citizen resource");
        }
    }

    // Creates a new citizen collection request; business rules stay in service.
    @PostMapping("/{citizenId}/collection-requests")
    public ResponseEntity<ApiResponse<RequestSummaryDto>> createCollectionRequest(
            @PathVariable Long citizenId,
            @Valid @RequestBody CreateRequestDto request) {
        enforceCitizenOwnership(citizenId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collectionRequestService.createRequest(citizenId, request)));
    }

    // Lists requests for the authenticated citizen; optional status filter is supported.
    @GetMapping("/{citizenId}/collection-requests")
    public ResponseEntity<ApiResponse<List<RequestSummaryDto>>> listCollectionRequests(
            @PathVariable Long citizenId,
            @RequestParam(required = false) RequestStatus status) {
        enforceCitizenOwnership(citizenId);
        return ResponseEntity.ok(ApiResponse.success(collectionRequestService.listCitizenRequests(citizenId, status)));
    }

    // Uploads request photo first and returns URL, then Flutter sends URL in create request payload.
    @PostMapping(value = "/{citizenId}/request-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadRequestPhoto(
            @PathVariable Long citizenId,
            @RequestParam("photo") MultipartFile photo) {
        enforceCitizenOwnership(citizenId);
        String url = collectionRequestService.uploadCitizenRequestPhoto(citizenId, photo);
        return ResponseEntity.ok(ApiResponse.success(Map.of("photoUrl", url)));
    }
}
