package com.garbo.core.entity;

import com.garbo.core.enums.CancellationReason;
import com.garbo.core.enums.OfferStatus;
import com.garbo.core.enums.PriceUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "collection_offers",
        indexes = {
                @Index(name = "idx_co_request", columnList = "request_id"),
                @Index(name = "idx_co_collector_status", columnList = "collector_id, status")
        }
)
public class CollectionOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CollectionRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collector_id", referencedColumnName = "emp_id", nullable = false)
    private ThirdPartyCollector collector;

    @Column(name = "price_per_unit", nullable = false)
    private Double pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", nullable = false, length = 20)
    private PriceUnit priceUnit;

    @Column(name = "proposed_pickup_at", nullable = false)
    private Instant proposedPickupAt;

    @Column(name = "message_to_citizen", length = 500)
    private String messageToCitizen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 30)
    private CancellationReason cancellationReason;

    @Column(name = "cancellation_note", length = 500)
    private String cancellationNote;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completion_photo_url", length = 500)
    private String completionPhotoUrl;

    @Column(name = "completion_weight_kg")
    private Double completionWeightKg;

    @Column(name = "completion_lat")
    private Double completionLat;

    @Column(name = "completion_lng")
    private Double completionLng;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "citizen_rating")
    private Integer citizenRating;

    @Column(name = "citizen_feedback", columnDefinition = "TEXT")
    private String citizenFeedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = OfferStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
