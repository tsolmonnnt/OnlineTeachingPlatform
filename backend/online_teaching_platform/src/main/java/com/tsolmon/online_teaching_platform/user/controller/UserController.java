package com.tsolmon.online_teaching_platform.user.controller;

import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
import com.tsolmon.online_teaching_platform.user.dto.UpdateUserRoleRequest;
import com.tsolmon.online_teaching_platform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return userService.updateUserRole(id, request.role());
    }
}
