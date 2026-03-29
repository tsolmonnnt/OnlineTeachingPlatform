package com.tsolmon.online_teaching_platform.notification.api.dto;

import com.tsolmon.online_teaching_platform.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

