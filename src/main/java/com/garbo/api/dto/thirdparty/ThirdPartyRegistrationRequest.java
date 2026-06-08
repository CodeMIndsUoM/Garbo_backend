package com.garbo.api.dto.thirdparty;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ThirdPartyRegistrationRequest {
    private String empName;
    private String email;
    private String phone;
    @JsonProperty("NIC")
    private String NIC;
    private String dateOfBirth;
    private String company;
    private String contractId;
    private String contractStart;
    private String contractEnd;
    private String defaultAddress;
    private String nicPhotoUrl;
    private String nicPhotoBackUrl;
    private List<String> assignedCouncils;
}
