package org.example.smartlearning.controller;

import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.LearningAnalyticsResponse;
import org.example.smartlearning.service.analytics.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 学情分析控制器
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/report")
    public ApiResponse<LearningAnalyticsResponse> getLearningReport(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(analyticsService.getLearningAnalytics(userId));
    }
}
