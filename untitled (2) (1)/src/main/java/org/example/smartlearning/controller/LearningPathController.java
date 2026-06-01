package org.example.smartlearning.controller;

import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.service.learning.LearningPathService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习路径控制器
 */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @GetMapping("/mistakes")
    public ApiResponse<List<Map<String, Object>>> getMistakeBook(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "subjectId", required = false) String subjectId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return ApiResponse.success(learningPathService.getMistakeBook(userId, subjectId, page, size));
    }

    @GetMapping("/recommend")
    public ApiResponse<List<Map<String, Object>>> getRecommendations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResponse.success(learningPathService.recommendQuestions(userId, limit));
    }

    @GetMapping("/path")
    public ApiResponse<Map<String, Object>> getPersonalizedPath(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(learningPathService.getPersonalizedPath(userId));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> getLearningTasks(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(learningPathService.getLearningTasks(userId));
    }

    @PutMapping("/tasks/{taskId}/complete")
    public ApiResponse<Void> completeLearningTask(
            @AuthenticationPrincipal Long userId,
            @PathVariable("taskId") Long taskId) {
        learningPathService.completeLearningTask(userId, taskId);
        return ApiResponse.success();
    }

    @GetMapping("/review-plan")
    public ApiResponse<Map<String, Object>> getReviewPlan(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(learningPathService.generateReviewPlan(userId));
    }

    @PostMapping("/mistakes/{mistakeId}/review")
    public ApiResponse<Void> reviewMistake(
            @AuthenticationPrincipal Long userId,
            @PathVariable("mistakeId") Long mistakeId,
            @RequestParam("isCorrect") boolean isCorrect) {
        learningPathService.reviewMistake(userId, mistakeId, isCorrect);
        return ApiResponse.success();
    }

    @PutMapping("/mistakes/{mistakeId}/resolve")
    public ApiResponse<Void> resolveMistake(
            @AuthenticationPrincipal Long userId,
            @PathVariable("mistakeId") Long mistakeId) {
        learningPathService.reviewMistake(userId, mistakeId, true);
        return ApiResponse.success();
    }

    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> getLearningProgress(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(learningPathService.getLearningProgress(userId));
    }
}
