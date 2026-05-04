package com.garbo.api.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffListDto {
    private Long empId;
    private String empName;
    private String email;
    private String role;
    private Boolean onDuty;
}
