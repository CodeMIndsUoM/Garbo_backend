package com.garbo.api.dto.thirdparty;

import lombok.Data;

@Data
public class ThirdPartySetPasswordRequest {
    private String email;
    private String password;
}
