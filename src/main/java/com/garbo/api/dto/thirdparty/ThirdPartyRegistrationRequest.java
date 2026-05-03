package com.garbo.api.dto.thirdparty;

import lombok.Data;

@Data
public class ThirdPartyRegistrationRequest {
    private String empName;
    private String email;
    private String phone;
    private String NIC;
    private String dateOfBirth;
    private String company;
    private String contractId;
    private String contractStart;
    private String contractEnd;
    private String defaultAddress;
    private String nicPhotoUrl;
    private String nicPhotoBackUrl;
    private String assignedCouncil;
}
