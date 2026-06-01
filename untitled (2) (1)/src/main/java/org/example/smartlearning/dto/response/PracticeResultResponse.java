package org.example.smartlearning.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 专项练习结果响应
 */
@Data
public class PracticeResultResponse {

    /**
     * 练习ID
     */
    private String practiceId;

    /**
     * 练习类型
     */
    private String practiceType;

    /**
     * 总题数
     */
    private Integer totalQuestions;

    /**
     * 已答题数
     */
    private Integer answeredQuestions;

    /**
     * 正确题数
     */
    private Integer correctCount;

    /**
     * 错误题数
     */
    private Integer wrongCount;

    /**
     * 正确率
     */
    private BigDecimal accuracyRate;

    /**
     * 总得分
     */
    private BigDecimal totalScore;

    /**
     * 总满分
     */
    private BigDecimal maxScore;

    /**
     * 用时（秒）
     */
    private Integer duration;

    /**
     * 各题结果
     */
    private List<QuestionResult> questionResults;

    /**
     * 按学科分类的答题详情
     */
    private List<SubjectResult> subjectResults;

    /**
     * 错误知识点统计
     */
    private List<Map<String, Object>> errorKnowledgeStats;

    /**
     * 错误类型统计
     */
    private List<Map<String, Object>> errorTypeStats;

    /**
     * AI学习建议
     */
    private String aiSuggestion;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    @Data
    public static class QuestionResult {
        private Long questionId;
        private Integer order;
        private String questionContent;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private BigDecimal score;
        private BigDecimal maxScore;
        private String mistakeType;
        private String aiAnalysis;
    }

    /**
     * 按学科分类的结果
     */
    @Data
    public static class SubjectResult {
        private Long subjectId;
        private String subjectName;
        private String subjectIcon;
        private Integer totalQuestions;
        private Integer correctCount;
        private Integer wrongCount;
        private BigDecimal accuracyRate;
        private BigDecimal totalScore;
        private BigDecimal maxScore;
        private List<QuestionResult> questions;
    }
}