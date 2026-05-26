package com.tsolmon.online_teaching_platform.common.api;

public record ApiFieldError(
        String field,
        String code,
        String message
) {
}
