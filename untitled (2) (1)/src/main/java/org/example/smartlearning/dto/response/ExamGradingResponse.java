package org.example.smartlearning.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 整卷批改结果响应
 */
@Data
public class ExamGradingResponse {

    private Long submissionId;

    private Long examId;

    private String examName;

    private BigDecimal totalScore;

    private BigDecimal maxScore;

    private BigDecimal accuracyRate;

    private Integer totalQuestions;

    private Integer correctCount;

    private Integer wrongCount;

    private Integer duration;

    private LocalDateTime submittedAt;

    /**
     * 各题批改详情
     */
    private List<QuestionGradingDetail> questions;

    /**
     * AI 总体评价
     */
    private String aiSummary;

    /**
     * 错误点分析
     */
    private List<ErrorPointAnalysis> errorPoints;

    /**
     * 单题批改详情
     */
    @Data
    public static class QuestionGradingDetail {
        private Long questionId;
        private Integer questionOrder;
        private String questionContent;
        private String questionType;
        private Integer difficulty;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private BigDecimal score;
        private BigDecimal maxScore;
        private String aiAnalysis;
        private String mistakeType;
        private String knowledgePoints;
    }

    /**
     * 错误点分析
     */
    @Data
    public static class ErrorPointAnalysis {
        private Long questionId;
        private Integer questionOrder;
        private String questionContent;
        private String userAnswer;
        private String correctAnswer;
        private String mistakeType;
        private String mistakeReason;
        private String knowledgeGap;
        private String suggestion;
    }
}
