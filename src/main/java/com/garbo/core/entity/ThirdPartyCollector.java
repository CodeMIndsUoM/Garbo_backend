package com.garbo.core.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garbo.core.enums.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "third_party_collectors")
@PrimaryKeyJoinColumn(name = "emp_id")
public class ThirdPartyCollector extends User {
    @JsonProperty("NIC")
    private String NIC;
    private String company;
    private String contractId;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private int completedRequests;

    @Column(name = "nic_photo_url")
    private String nicPhotoUrl;

    @Column(name = "nic_photo_back_url")
    private String nicPhotoBackUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "assigned_councils")
    private String assignedCouncils;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.ManyToMany(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinTable(
        name = "third_party_collector_assigned_councils",
        joinColumns = @jakarta.persistence.JoinColumn(name = "collector_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "council_id")
    )
    private java.util.Set<Council> councilEntities = new java.util.HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status")
    private RegistrationStatus registrationStatus;
}
