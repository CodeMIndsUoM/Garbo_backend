package com.garbo.api.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreateRequest {
    private String fullName;
    private String email;
    private String contactNumber;
    /** Required when superadmin creates staff for a specific council. */
    private String council;
}
