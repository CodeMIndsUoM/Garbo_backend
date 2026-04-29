package com.garbo.core.service.shared;

import com.garbo.api.exception.CollectionException;
import com.garbo.core.dto.collection.CancelOfferDto;
import com.garbo.core.dto.collection.CancelRequestDto;
import com.garbo.core.dto.collection.CompleteOfferDto;
import com.garbo.core.dto.collection.ConfirmDto;
import com.garbo.core.dto.collection.CreateOfferDto;
import com.garbo.core.dto.collection.CreateRequestDto;
import com.garbo.core.dto.collection.OfferDto;
import com.garbo.core.dto.collection.RequestDetailDto;
import com.garbo.core.dto.collection.RequestSummaryDto;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.CollectionOffer;
import com.garbo.core.entity.CollectionRequest;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.entity.User;
import com.garbo.core.enums.CancellationReason;
import com.garbo.core.enums.OfferStatus;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.CollectionOfferRepository;
import com.garbo.core.repository.CollectionRequestRepository;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CollectionRequestService {
    private static final List<OfferStatus> ACTIVE_OFFER_STATUSES = List.of(OfferStatus.PENDING, OfferStatus.ACCEPTED,
            OfferStatus.IN_PROGRESS);
    private static final List<OfferStatus> HIDEABLE_OFFER_STATUSES = List.of(
            OfferStatus.REJECTED,
            OfferStatus.WITHDRAWN,
            OfferStatus.CANCELLED,
            OfferStatus.COMPLETED);

    private final CollectionRequestRepository requestRepository;
    private final CollectionOfferRepository offerRepository;
    private final CitizenRepository citizenRepository;
    private final ThirdPartyCollectorRepository collectorRepository;
    private final UserRepository userRepository;
    private final CloudinaryUploadService cloudinaryUploadService;
    private final CollectorDashboardService collectorDashboardService;

    public CollectionRequestService(CollectionRequestRepository requestRepository,
            CollectionOfferRepository offerRepository,
            CitizenRepository citizenRepository,
            ThirdPartyCollectorRepository collectorRepository,
            UserRepository userRepository,
            CloudinaryUploadService cloudinaryUploadService,
            CollectorDashboardService collectorDashboardService) {
        this.requestRepository = requestRepository;
        this.offerRepository = offerRepository;
        this.citizenRepository = citizenRepository;
        this.collectorRepository = collectorRepository;
        this.userRepository = userRepository;
        this.cloudinaryUploadService = cloudinaryUploadService;
        this.collectorDashboardService = collectorDashboardService;
    }

    @Transactional
    public RequestSummaryDto createRequest(Long citizenId, CreateRequestDto dto) {
        requireCurrentUser(citizenId);
        validateCreateRequest(dto);

        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> notFound("Citizen not found"));

        CollectionRequest request = new CollectionRequest();
        request.setCitizen(citizen);
        request.setWasteType(dto.wasteType());
        request.setQuantityLabel(dto.quantityLabel().trim());
        request.setQuantityKgEstimate(dto.quantityKgEstimate());
        request.setAddressLine(dto.addressLine().trim());
        request.setLatitude(dto.latitude());
        request.setLongitude(dto.longitude());
        request.setPreferredDate(dto.preferredDate());
        request.setPreferredSlot(dto.preferredSlot());
        request.setContactPhone(dto.contactPhone().trim());
        request.setNotes(blankToNull(dto.notes()));
        request.setPhotoUrl(blankToNull(dto.photoUrl()));
        request.setStatus(RequestStatus.OPEN);

        CollectionRequest saved = requestRepository.save(request);
        return RequestSummaryDto.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public String uploadCitizenRequestPhoto(Long citizenId, MultipartFile photo) {
        requireCurrentUser(citizenId);
        return cloudinaryUploadService.uploadRequestPhoto(photo, citizenId);
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> listCitizenRequests(Long citizenId, RequestStatus status) {
        requireCurrentUser(citizenId);
        List<CollectionRequest> requests = status == null
                ? requestRepository.findByCitizen_EmpIdOrderByCreatedAtDesc(citizenId)
                : requestRepository.findByCitizen_EmpIdAndStatusOrderByCreatedAtDesc(citizenId, status);
        return requests.stream()
                .map(request -> RequestSummaryDto.from(request,
                        offerRepository.findByRequest_IdOrderByCreatedAtDesc(request.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestDetailDto getRequestDetail(Long requestId) {
        User viewer = currentUser();
        CollectionRequest request = getRequest(requestId);
        if (!canViewRequest(viewer, request)) {
            throw forbidden("You are not allowed to view this request");
        }
        return RequestDetailDto.from(request, offerRepository.findByRequest_IdOrderByCreatedAtDesc(requestId));
    }

    @Transactional(readOnly = true)
    public List<OfferDto> listOffersByRequest(Long requestId) {
        User viewer = currentUser();
        CollectionRequest request = getRequest(requestId);
        requireCitizenOwner(viewer, request);
        return offerRepository.findByRequest_IdOrderByCreatedAtDesc(requestId).stream()
                .map(OfferDto::from)
                .toList();
    }

    @Transactional
    public OfferDto acceptOffer(Long offerId) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        CollectionRequest request = offer.getRequest();
        requireCitizenOwner(viewer, request);

        if (request.getStatus() != RequestStatus.OPEN) {
            throw conflict("Request is not open for accepting offers");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw conflict("Only pending offers can be accepted");
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        request.setStatus(RequestStatus.ASSIGNED);
        request.setAcceptedOfferId(offer.getId());

        for (CollectionOffer other : offerRepository.findByRequest_IdAndStatus(request.getId(), OfferStatus.PENDING)) {
            if (!other.getId().equals(offer.getId())) {
                other.setStatus(OfferStatus.REJECTED);
            }
        }

        return OfferDto.from(offer);
    }

    @Transactional
    public OfferDto rejectOffer(Long offerId) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCitizenOwner(viewer, offer.getRequest());
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw conflict("Only pending offers can be rejected");
        }
        offer.setStatus(OfferStatus.REJECTED);
        return OfferDto.from(offer);
    }

    @Transactional
    public RequestSummaryDto cancelRequest(Long requestId, CancelRequestDto dto) {
        User viewer = currentUser();
        CollectionRequest request = getRequest(requestId);
        requireCitizenOwner(viewer, request);
        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.ASSIGNED) {
            throw conflict("Request can only be cancelled before collection starts");
        }

        for (CollectionOffer offer : offerRepository.findByRequest_IdOrderByCreatedAtDesc(requestId)) {
            if (offer.getStatus() == OfferStatus.PENDING) {
                offer.setStatus(OfferStatus.REJECTED);
            } else if (offer.getStatus() == OfferStatus.ACCEPTED) {
                offer.setStatus(OfferStatus.CANCELLED);
                offer.setCancellationNote(blankToNull(dto != null ? dto.reason() : null));
            }
        }
        request.setStatus(RequestStatus.CANCELLED);
        return RequestSummaryDto.from(request, offerRepository.findByRequest_IdOrderByCreatedAtDesc(requestId).size());
    }

    @Transactional
    public OfferDto confirmCompletion(Long offerId, ConfirmDto dto) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        CollectionRequest request = offer.getRequest();
        requireCitizenOwner(viewer, request);
        if (offer.getStatus() != OfferStatus.COMPLETED || request.getStatus() != RequestStatus.COMPLETED) {
            throw conflict("Only completed offers can be confirmed");
        }
        if (dto == null || dto.rating() == null || dto.rating() < 1 || dto.rating() > 5) {
            throw badRequest("Rating must be between 1 and 5");
        }

        offer.setCitizenRating(dto.rating());
        offer.setCitizenFeedback(blankToNull(dto.feedback()));
        request.setStatus(RequestStatus.CONFIRMED);
        offer.getCollector().setCompletedRequests(offer.getCollector().getCompletedRequests() + 1);
        return OfferDto.from(offer);
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> browseFeed(Long collectorId, Double lat, Double lng) {
        requireCurrentUser(collectorId);
        validateCoordinates(lat, lng, false);
        collectorRepository.findById(collectorId)
                .orElseThrow(() -> notFound("Collector not found"));

        final List<CollectionRequest> openRequests = (lat == null || lng == null)
                ? requestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.OPEN)
                : requestRepository.findOpenFeedNear(lat, lng);

        return openRequests.stream()
                .filter(request -> !offerRepository.existsByRequest_IdAndCollector_EmpIdAndStatusNot(
                        request.getId(), collectorId, OfferStatus.WITHDRAWN))
                .map(request -> RequestSummaryDto.from(request,
                        offerRepository.findByRequest_IdOrderByCreatedAtDesc(request.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferDto> listMyOffers(Long collectorId, OfferStatus status) {
        requireCurrentUser(collectorId);
        List<CollectionOffer> offers = status == null
                ? offerRepository.findByCollector_EmpIdAndCollectorHiddenFalseOrderByCreatedAtDesc(collectorId)
                : offerRepository.findByCollector_EmpIdAndStatusAndCollectorHiddenFalseOrderByCreatedAtDesc(
                        collectorId,
                        status);
        return offers.stream().map(OfferDto::from).toList();
    }

    @Transactional
    public OfferDto hideOfferFromCollectorList(Long offerId) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCollectorOwner(viewer, offer);

        if (!HIDEABLE_OFFER_STATUSES.contains(offer.getStatus())) {
            throw conflict("Only rejected, withdrawn, cancelled, or completed offers can be removed from list");
        }

        offer.setCollectorHidden(true);
        offer.setCollectorHiddenAt(Instant.now());
        return OfferDto.from(offer);
    }

    @Transactional
    public int hideOffersFromCollectorList(Long collectorId, List<OfferStatus> statuses) {
        requireCurrentUser(collectorId);

        List<OfferStatus> allowedStatuses;
        if (statuses == null || statuses.isEmpty()) {
            allowedStatuses = HIDEABLE_OFFER_STATUSES;
        } else {
            allowedStatuses = statuses.stream()
                    .filter(HIDEABLE_OFFER_STATUSES::contains)
                    .distinct()
                    .toList();
        }

        if (allowedStatuses.isEmpty()) {
            throw badRequest("No hideable statuses were provided");
        }

        List<CollectionOffer> collectorOffers = offerRepository.findByCollector_EmpIdOrderByCreatedAtDesc(collectorId);
        Instant now = Instant.now();

        List<CollectionOffer> offersToHide = collectorOffers.stream()
                .filter(offer -> !offer.isCollectorHidden())
                .filter(offer -> allowedStatuses.contains(offer.getStatus()))
                .toList();

        offersToHide.forEach(offer -> {
            offer.setCollectorHidden(true);
            offer.setCollectorHiddenAt(now);
        });

        if (!offersToHide.isEmpty()) {
            offerRepository.saveAll(offersToHide);
        }

        return offersToHide.size();
    }

    @Transactional(readOnly = true)
    public List<OfferDto> listActiveJobs(Long collectorId) {
        requireCurrentUser(collectorId);
        return offerRepository.findByCollector_EmpIdAndStatusInOrderByCreatedAtDesc(
                collectorId, List.of(OfferStatus.ACCEPTED, OfferStatus.IN_PROGRESS))
                .stream()
                .map(OfferDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.garbo.core.dto.collection.CollectorDashboardDto getCollectorDashboard(Long collectorId) {
        requireCurrentUser(collectorId);
        return collectorDashboardService.getCollectorDashboard(collectorId);
    }

    @Transactional
    public OfferDto sendOffer(Long requestId, CreateOfferDto dto) {
        User viewer = currentUser();
        Long collectorId = viewer.getEmpId();
        validateCreateOffer(dto);
        CollectionRequest request = getRequest(requestId);
        if (request.getStatus() != RequestStatus.OPEN) {
            throw conflict("Offers can only be sent for open requests");
        }
        ThirdPartyCollector collector = collectorRepository.findById(collectorId)
                .orElseThrow(() -> notFound("Collector not found"));
        offerRepository
                .findFirstByRequest_IdAndCollector_EmpIdAndStatusIn(requestId, collectorId, ACTIVE_OFFER_STATUSES)
                .ifPresent(existing -> {
                    throw conflict("Collector already has an active offer for this request");
                });

        CollectionOffer offer = new CollectionOffer();
        offer.setRequest(request);
        offer.setCollector(collector);
        offer.setPricePerUnit(dto.pricePerUnit());
        offer.setPriceUnit(dto.priceUnit());
        offer.setProposedPickupAt(dto.proposedPickupAt());
        offer.setMessageToCitizen(blankToNull(dto.messageToCitizen()));
        offer.setStatus(OfferStatus.PENDING);
        return OfferDto.from(offerRepository.save(offer));
    }

    @Transactional
    public OfferDto withdrawOffer(Long offerId) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCollectorOwner(viewer, offer);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw conflict("Only pending offers can be withdrawn");
        }
        offer.setStatus(OfferStatus.WITHDRAWN);
        return OfferDto.from(offer);
    }

    @Transactional
    public OfferDto cancelAcceptedOffer(Long offerId, CancelOfferDto dto) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCollectorOwner(viewer, offer);
        if (offer.getStatus() != OfferStatus.ACCEPTED && offer.getStatus() != OfferStatus.IN_PROGRESS) {
            throw conflict("Only accepted or in-progress offers can be cancelled");
        }
        if (dto == null || dto.reason() == null) {
            throw badRequest("Cancellation reason is required");
        }
        if (dto.reason() == CancellationReason.OTHER && isBlank(dto.note())) {
            throw badRequest("Cancellation note is required when reason is OTHER");
        }

        CollectionRequest request = offer.getRequest();
        offer.setStatus(OfferStatus.CANCELLED);
        offer.setCancellationReason(dto.reason());
        offer.setCancellationNote(blankToNull(dto.note()));
        request.setStatus(RequestStatus.OPEN);
        request.setAcceptedOfferId(null);

        for (CollectionOffer rejected : offerRepository.findByRequest_IdAndStatus(request.getId(),
                OfferStatus.REJECTED)) {
            rejected.setStatus(OfferStatus.PENDING);
        }
        return OfferDto.from(offer);
    }

    @Transactional
    public OfferDto startOffer(Long offerId) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCollectorOwner(viewer, offer);
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw conflict("Only accepted offers can be started");
        }
        offer.setStatus(OfferStatus.IN_PROGRESS);
        offer.setStartedAt(Instant.now());
        offer.getRequest().setStatus(RequestStatus.IN_PROGRESS);
        return OfferDto.from(offer);
    }

    @Transactional
    public OfferDto completeOffer(Long offerId, CompleteOfferDto dto) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCollectorOwner(viewer, offer);
        if (offer.getStatus() != OfferStatus.IN_PROGRESS) {
            throw conflict("Only in-progress offers can be completed");
        }
        if (dto == null || isBlank(dto.photoUrl())) {
            throw badRequest("Completion photo URL is required");
        }
        if (dto.latitude() == null || dto.longitude() == null) {
            throw badRequest("Completion latitude and longitude are required");
        }
        validateCoordinates(dto.latitude(), dto.longitude(), true);
        if (offer.getRequest().getWasteType().isWeightRequiredAtCompletion()
                && (dto.weightKg() == null || dto.weightKg() <= 0)) {
            throw badRequest("Weight is required for this waste type");
        }

        offer.setCompletionPhotoUrl(dto.photoUrl().trim());
        offer.setCompletionWeightKg(dto.weightKg());
        offer.setCompletionLat(dto.latitude());
        offer.setCompletionLng(dto.longitude());
        offer.setCompletionNotes(blankToNull(dto.notes()));
        offer.setCompletedAt(Instant.now());
        offer.setStatus(OfferStatus.COMPLETED);
        offer.getRequest().setStatus(RequestStatus.COMPLETED);
        return OfferDto.from(offer);
    }

    @Transactional
    public OfferDto completeOfferWithPhoto(
            Long offerId,
            MultipartFile photo,
            Double weightKg,
            Double latitude,
            Double longitude,
            String notes) {
        User viewer = currentUser();
        CollectionOffer offer = getOffer(offerId);
        requireCollectorOwner(viewer, offer);

        if (offer.getStatus() != OfferStatus.IN_PROGRESS) {
            throw conflict("Only in-progress offers can be completed");
        }
        if (latitude == null || longitude == null) {
            throw badRequest("Completion latitude and longitude are required");
        }
        validateCoordinates(latitude, longitude, true);
        if (offer.getRequest().getWasteType().isWeightRequiredAtCompletion()
                && (weightKg == null || weightKg <= 0)) {
            throw badRequest("Weight is required for this waste type");
        }

        String uploadedPhotoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            uploadedPhotoUrl = cloudinaryUploadService.uploadCompletionPhoto(photo, offerId);
        }

        offer.setCompletionPhotoUrl(uploadedPhotoUrl);
        offer.setCompletionWeightKg(weightKg);
        offer.setCompletionLat(latitude);
        offer.setCompletionLng(longitude);
        offer.setCompletionNotes(blankToNull(notes));
        offer.setCompletedAt(Instant.now());
        offer.setStatus(OfferStatus.COMPLETED);
        offer.getRequest().setStatus(RequestStatus.COMPLETED);
        return OfferDto.from(offer);
    }

    @Transactional
    public int expireStalePendingOffers() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        List<CollectionOffer> staleOffers = offerRepository.findByStatusAndCreatedAtBefore(OfferStatus.PENDING, cutoff);
        staleOffers.forEach(offer -> offer.setStatus(OfferStatus.WITHDRAWN));
        return staleOffers.size();
    }

    private boolean canViewRequest(User viewer, CollectionRequest request) {
        if (request.getCitizen().getEmpId().equals(viewer.getEmpId())) {
            return true;
        }
        if ("THIRD_PARTY_COLLECTOR".equals(viewer.getRole())) {
            return request.getStatus() == RequestStatus.OPEN
                    || offerRepository.findFirstByRequest_IdAndCollector_EmpIdAndStatusIn(
                            request.getId(), viewer.getEmpId(), List.of(OfferStatus.PENDING, OfferStatus.ACCEPTED,
                                    OfferStatus.REJECTED, OfferStatus.WITHDRAWN, OfferStatus.CANCELLED,
                                    OfferStatus.IN_PROGRESS, OfferStatus.COMPLETED))
                            .isPresent();
        }
        return false;
    }

    private CollectionRequest getRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> notFound("Collection request not found"));
    }

    private CollectionOffer getOffer(Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> notFound("Collection offer not found"));
    }

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new CollectionException(HttpStatus.UNAUTHORIZED, "Authentication is required", "UNAUTHORIZED");
        }
        return userRepository.findFirstByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new CollectionException(HttpStatus.UNAUTHORIZED, "Authenticated user not found",
                        "UNAUTHORIZED"));
    }

    private void requireCurrentUser(Long expectedUserId) {
        User user = currentUser();
        if (!user.getEmpId().equals(expectedUserId)) {
            throw forbidden("Path user id does not match authenticated user");
        }
    }

    private void requireCitizenOwner(User viewer, CollectionRequest request) {
        if (!request.getCitizen().getEmpId().equals(viewer.getEmpId())) {
            throw forbidden("Only the request owner can perform this action");
        }
    }

    private void requireCollectorOwner(User viewer, CollectionOffer offer) {
        if (!offer.getCollector().getEmpId().equals(viewer.getEmpId())) {
            throw forbidden("Only the offer collector can perform this action");
        }
    }

    private void validateCreateRequest(CreateRequestDto dto) {
        if (dto == null || dto.wasteType() == null || dto.preferredDate() == null || dto.preferredSlot() == null) {
            throw badRequest("Waste type, preferred date and preferred slot are required");
        }
        if (isBlank(dto.quantityLabel()) || isBlank(dto.addressLine()) || isBlank(dto.contactPhone())) {
            throw badRequest("Quantity, address and contact phone are required");
        }
        if (dto.quantityKgEstimate() != null && dto.quantityKgEstimate() <= 0) {
            throw badRequest("Quantity estimate must be greater than zero");
        }
        if (dto.latitude() == null || dto.longitude() == null) {
            throw badRequest("Latitude and longitude are required");
        }
        validateCoordinates(dto.latitude(), dto.longitude(), true);
    }

    private void validateCreateOffer(CreateOfferDto dto) {
        if (dto == null || dto.pricePerUnit() == null || dto.priceUnit() == null || dto.proposedPickupAt() == null) {
            throw badRequest("Price, price unit and proposed pickup time are required");
        }
        if (dto.pricePerUnit() <= 0) {
            throw badRequest("Price must be greater than zero");
        }
        if (dto.proposedPickupAt().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES))) {
            throw badRequest("Proposed pickup time cannot be in the past");
        }
    }

    private void validateCoordinates(Double lat, Double lng, boolean required) {
        if (!required && lat == null && lng == null) {
            return;
        }
        if (lat == null || lng == null) {
            throw badRequest("Both latitude and longitude are required");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw badRequest("Invalid latitude or longitude");
        }
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private CollectionException badRequest(String message) {
        return new CollectionException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST");
    }

    private CollectionException notFound(String message) {
        return new CollectionException(HttpStatus.NOT_FOUND, message, "NOT_FOUND");
    }

    private CollectionException forbidden(String message) {
        return new CollectionException(HttpStatus.FORBIDDEN, message, "FORBIDDEN");
    }

    private CollectionException conflict(String message) {
        return new CollectionException(HttpStatus.CONFLICT, message, "CONFLICT");
    }
}
