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

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "assigned_council")
    private String assignedCouncil;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status")
    private RegistrationStatus registrationStatus;
}
