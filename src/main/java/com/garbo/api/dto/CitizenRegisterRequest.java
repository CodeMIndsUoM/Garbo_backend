package com.garbo.api.dto;

import lombok.Data;

@Data
public class CitizenRegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String council;
    private String address;
    private String area;
}
