package org.example.smartlearning.controller;

import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.LearningAnalyticsResponse;
import org.example.smartlearning.service.analytics.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @GetMapping("/period-report")
    public ApiResponse<Map<String, Object>> getPeriodReport(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "period", defaultValue = "week") String period) {
        return ApiResponse.success(analyticsService.getPeriodReport(userId, period));
    }
}
