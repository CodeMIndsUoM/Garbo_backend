package com.garbo.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
// Standard API envelope used by scoped mobile flows (field staff, citizen, third-party collector).
// Moved from core/dto to api/dto/common to reflect transport-layer responsibility.
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String error;

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }
}
