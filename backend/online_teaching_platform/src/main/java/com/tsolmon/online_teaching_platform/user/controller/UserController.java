package com.tsolmon.online_teaching_platform.user.controller;

import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
import com.tsolmon.online_teaching_platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
