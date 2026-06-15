package com.garbo.api.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateRequest {
    private String fullName;
    private String contactNumber;
    /** Superadmin may reassign council. */
    private String council;
    /** When true, generates a new temporary password and emails it. */
    private Boolean resetPassword;
}
