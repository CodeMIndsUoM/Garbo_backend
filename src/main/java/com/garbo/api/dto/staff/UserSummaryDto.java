package com.garbo.api.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long empId;
    private String empName;
    private String email;
    private String role;
    private String assignedCouncil;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;
    private Boolean onDuty;
    private Double rewardPoints;
}
