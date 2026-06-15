package com.garbo.core.entity;

import com.garbo.core.enums.PreferredSlot;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.enums.WasteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "collection_requests",
        indexes = {
                @Index(name = "idx_cr_status_created", columnList = "status, created_at"),
                @Index(name = "idx_cr_citizen_status", columnList = "citizen_id, status"),
                @Index(name = "idx_cr_council_status_created", columnList = "council, status, created_at")
        }
)
public class CollectionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", referencedColumnName = "emp_id", nullable = false)
    private Citizen citizen;

    @Column(length = 120)
    private String council;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id")
    private Council councilEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WasteType wasteType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "collection_request_waste_types",
            joinColumns = @JoinColumn(name = "request_id")
    )
    @OrderColumn(name = "sort_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", length = 20, nullable = false)
    private List<WasteType> wasteTypes = new ArrayList<>();

    @Column(name = "quantity_label", nullable = false, length = 50)
    private String quantityLabel;

    @Column(name = "quantity_kg_estimate")
    private Double quantityKgEstimate;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_slot", nullable = false, length = 20)
    private PreferredSlot preferredSlot;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.OPEN;

    @Column(name = "accepted_offer_id")
    private Long acceptedOfferId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = RequestStatus.OPEN;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
