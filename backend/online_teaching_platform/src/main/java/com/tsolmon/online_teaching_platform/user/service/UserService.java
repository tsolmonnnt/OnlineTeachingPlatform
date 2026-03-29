package com.tsolmon.online_teaching_platform.user.service;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse updateUserRole(Long id, Role role);
}
