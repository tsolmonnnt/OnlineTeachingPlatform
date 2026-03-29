package com.tsolmon.online_teaching_platform.user.service;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
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
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }
}
