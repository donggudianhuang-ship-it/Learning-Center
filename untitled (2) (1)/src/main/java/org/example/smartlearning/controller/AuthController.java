package org.example.smartlearning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.LoginRequest;
import org.example.smartlearning.dto.request.RegisterRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.LoginResponse;
import org.example.smartlearning.service.auth.AuthService;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }
}
