package org.example.smartlearning.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷扫描判卷结果响应
 */
@Data
public class ScanGradingResponse {

    /**
     * AI判卷记录ID
     */
    private Long recordId;

    /**
     * 记录标题
     */
    private String title;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 判卷状态：PROCESSING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * OCR识别文本，详情页用于排查识别问题
     */
    private String ocrText;

    /**
     * AI识别出的科目
     */
    private String subject;

    /**
     * AI推断的年级
     */
    private String grade;

    /**
     * 识别出的题目列表
     */
    private List<RecognizedQuestion> questions;

    /**
     * 总体得分
     */
    private BigDecimal totalScore;

    /**
     * 总分
     */
    private BigDecimal maxScore;

    /**
     * 正确率
     */
    private BigDecimal accuracyRate;

    /**
     * 正确题数
     */
    private Integer correctCount;

    /**
     * 错误题数
     */
    private Integer wrongCount;

    /**
     * AI 总体评价
     */
    private String aiSummary;

    /**
     * 错误点分析
     */
    private List<ErrorPoint> errorPoints;

    /**
     * 识别出的题目
     */
    @Data
    public static class RecognizedQuestion {
        private Integer questionNumber;
        private String questionContent;
        private String questionType;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private BigDecimal score;
        private BigDecimal maxScore;
        private String aiAnalysis;
        private String mistakeType;
        private String knowledgePoints;
        private String suggestion;
    }

    /**
     * 错误点
     */
    @Data
    public static class ErrorPoint {
        private Integer questionNumber;
        private String questionContent;
        private String userAnswer;
        private String correctAnswer;
        private String mistakeType;
        private String mistakeReason;
        private String suggestion;
    }
}
