package com.tsolmon.online_teaching_platform.user.service;

import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;

public interface UserService {
    UserResponse getUserById(Long id);
}
