package org.example.smartlearning.controller;

import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.entity.User;
import org.example.smartlearning.service.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(@AuthenticationPrincipal Long userId) {
        User user = userService.getById(userId);
        user.setPassword(null); // 不返回密码
        return ApiResponse.success(user);
    }

    @PutMapping("/me")
    public ApiResponse<Void> updateProfile(@AuthenticationPrincipal Long userId, @RequestBody User updateInfo) {
        userService.updateProfile(userId, updateInfo);
        return ApiResponse.success("更新成功", null);
    }
}
