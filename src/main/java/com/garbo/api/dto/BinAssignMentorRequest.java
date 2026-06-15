package com.garbo.api.dto;

import lombok.Data;

@Data
public class BinAssignMentorRequest {
    /** Field mentor empId; null to unassign. */
    private Long mentorEmpId;
}
