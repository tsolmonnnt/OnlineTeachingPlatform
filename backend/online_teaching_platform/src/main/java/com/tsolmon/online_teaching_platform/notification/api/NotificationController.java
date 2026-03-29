package com.tsolmon.online_teaching_platform.notification.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.notification.api.dto.NotificationResponse;
import com.tsolmon.online_teaching_platform.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/me")
    public List<NotificationResponse> myNotifications(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return notificationService.getMyNotifications(authUser);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(Authentication authentication, @PathVariable Long notificationId) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return notificationService.markAsRead(authUser, notificationId);
    }
}

