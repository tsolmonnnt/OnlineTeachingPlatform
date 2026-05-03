package com.tsolmon.online_teaching_platform.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUser_IdOrderByCreatedAtDesc(Long recipientUserId);

    long countByRecipientUser_IdAndIsReadFalse(Long recipientUserId);
}

