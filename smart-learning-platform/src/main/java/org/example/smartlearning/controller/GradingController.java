package org.example.smartlearning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.SubmitAnswerRequest;
import org.example.smartlearning.dto.request.SubmitExamRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.ExamGradingResponse;
import org.example.smartlearning.dto.response.ExamPaperResponse;
import org.example.smartlearning.dto.response.GradingResultResponse;
import org.example.smartlearning.dto.response.ScanGradingResponse;
import org.example.smartlearning.service.grading.GradingService;
import org.example.smartlearning.service.grading.PaperScanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI精准判卷控制器
 */
@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;
    private final PaperScanService paperScanService;

    /**
     * 上传试卷图片进行扫描判卷
     */
    @PostMapping("/scan")
    public ApiResponse<ScanGradingResponse> scanAndGrade(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "grade", required = false) String grade) {

        if (file.isEmpty()) {
            return ApiResponse.error("请上传文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error("当前扫描判卷仅支持图片文件，请将PDF转为图片后上传");
        }

        ScanGradingResponse result = paperScanService.scanAndGrade(userId, file, subjectId, grade);
        return ApiResponse.success(result);
    }

    /**
     * 获取拍照/上传图片 AI 判卷记录
     */
    @GetMapping("/ai-records")
    public ApiResponse<List<Map<String, Object>>> getAiGradingRecords(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return ApiResponse.success(paperScanService.getAiGradingRecords(userId, limit));
    }

    /**
     * 获取拍照/上传图片 AI 判卷记录详情
     */
    @GetMapping("/ai-records/{recordId}")
    public ApiResponse<ScanGradingResponse> getAiGradingRecordDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable("recordId") Long recordId) {
        return ApiResponse.success(paperScanService.getAiGradingRecordDetail(userId, recordId));
    }

    /**
     * 提交单题答案并批改
     */
    @PostMapping("/submit")
    public ApiResponse<GradingResultResponse> submitAnswer(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.success(gradingService.submitAndGrade(userId, request));
    }

    /**
     * 提交整卷并批改
     */
    @PostMapping("/exam/submit")
    public ApiResponse<ExamGradingResponse> submitExam(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubmitExamRequest request) {
        return ApiResponse.success(gradingService.submitAndGradeExam(userId, request));
    }

    /**
     * 获取试卷详情（用于答题）
     */
    @GetMapping("/exam/{examId}")
    public ApiResponse<ExamPaperResponse> getExamPaper(
            @AuthenticationPrincipal Long userId,
            @PathVariable("examId") Long examId) {
        return ApiResponse.success(gradingService.getExamPaper(examId));
    }

    /**
     * 获取可用的试卷列表
     */
    @GetMapping("/exams")
    public ApiResponse<List<ExamPaperResponse>> getAvailableExams(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "subjectId", required = false) Long subjectId) {
        return ApiResponse.success(gradingService.getAvailableExams(subjectId));
    }

    /**
     * 获取历史提交记录
     */
    @GetMapping("/history")
    public ApiResponse<List<ExamGradingResponse>> getGradingHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResponse.success(gradingService.getGradingHistory(userId, limit));
    }

    /**
     * 获取提交详情
     */
    @GetMapping("/submission/{submissionId}")
    public ApiResponse<ExamGradingResponse> getSubmissionDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable("submissionId") Long submissionId) {
        return ApiResponse.success(gradingService.getSubmissionDetail(userId, submissionId));
    }
}
