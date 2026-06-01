package org.example.smartlearning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.PracticeRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.PracticeResponse;
import org.example.smartlearning.dto.response.PracticeResultResponse;
import org.example.smartlearning.service.practice.PracticeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专项练习控制器
 */
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    /**
     * 获取专项练习类型列表
     */
    @GetMapping("/types")
    public ApiResponse<List<Map<String, Object>>> getPracticeTypes() {
        return ApiResponse.success(practiceService.getPracticeTypes());
    }

    /**
     * 获取科目的知识点列表
     */
    @GetMapping("/knowledge-points")
    public ApiResponse<List<Map<String, Object>>> getKnowledgePoints(
            @RequestParam("subjectId") Long subjectId) {
        return ApiResponse.success(practiceService.getKnowledgePointsBySubject(subjectId));
    }

    /**
     * 获取科目当前题库中实际存在的题型
     */
    @GetMapping("/question-types")
    public ApiResponse<List<Map<String, Object>>> getQuestionTypes(
            @RequestParam("subjectId") Long subjectId) {
        return ApiResponse.success(practiceService.getQuestionTypesBySubject(subjectId));
    }

    /**
     * 生成专项练习
     */
    @PostMapping("/generate")
    public ApiResponse<PracticeResponse> generatePractice(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PracticeRequest request) {
        return ApiResponse.success(practiceService.generatePractice(userId, request));
    }

    /**
     * 提交单题答案
     */
    @PostMapping("/{practiceId}/submit")
    public ApiResponse<Map<String, Object>> submitAnswer(
            @AuthenticationPrincipal Long userId,
            @PathVariable("practiceId") String practiceId,
            @RequestParam("questionId") Long questionId,
            @RequestParam("userAnswer") String userAnswer,
            @RequestParam(value = "questionOrder", required = false) Integer questionOrder) {
        return ApiResponse.success(practiceService.submitAnswer(userId, practiceId, questionId, userAnswer, questionOrder));
    }

    /**
     * 完成练习
     */
    @PostMapping("/{practiceId}/finish")
    public ApiResponse<PracticeResultResponse> finishPractice(
            @AuthenticationPrincipal Long userId,
            @PathVariable("practiceId") String practiceId) {
        return ApiResponse.success(practiceService.finishPractice(userId, practiceId));
    }

    /**
     * 获取练习历史
     */
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getPracticeHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "practiceType", required = false) String practiceType,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return ApiResponse.success(practiceService.getPracticeHistory(userId, practiceType, limit));
    }

    /**
     * 获取练习统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getPracticeStats(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(practiceService.getPracticeStats(userId));
    }
}
