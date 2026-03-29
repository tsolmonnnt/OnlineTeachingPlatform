package com.tsolmon.online_teaching_platform.notification.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.notification.api.dto.NotificationResponse;
import com.tsolmon.online_teaching_platform.notification.domain.Notification;
import com.tsolmon.online_teaching_platform.notification.domain.NotificationRepository;
import com.tsolmon.online_teaching_platform.user.entity.User;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notifyUser(Long userId, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Notification notification = new Notification();
        notification.setRecipientUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(AuthUser authUser) {
        return notificationRepository.findByRecipientUser_IdOrderByCreatedAtDesc(authUser.id()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(AuthUser authUser, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getRecipientUser().getId().equals(authUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access this notification");
        }

        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}

