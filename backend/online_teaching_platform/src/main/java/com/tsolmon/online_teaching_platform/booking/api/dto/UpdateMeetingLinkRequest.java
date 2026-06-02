package com.tsolmon.online_teaching_platform.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeetingLinkRequest(
        @NotBlank
        @Size(max = 1024)
        String meetingLink
) {
}

