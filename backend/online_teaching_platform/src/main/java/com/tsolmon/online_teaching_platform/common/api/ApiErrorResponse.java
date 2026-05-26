package com.tsolmon.online_teaching_platform.common.api;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<ApiFieldError> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, List.of());
    }
}
