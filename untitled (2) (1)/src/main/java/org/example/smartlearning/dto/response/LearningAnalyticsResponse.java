package org.example.smartlearning.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学情分析报告响应DTO
 */
@Data
public class LearningAnalyticsResponse {

    private OverallStats overallStats;
    private LearningDiagnosis diagnosis;
    private KnowledgeCoverage knowledgeCoverage;
    private List<KnowledgeHeatmap> knowledgeHeatmap;
    private List<WeakPointAnalysis> weakPoints;
    private List<MistakeTypeStats> mistakeTypeStats;
    private LearningTrend learningTrend;
    private List<ActionRecommendation> recommendations;

    @Data
    public static class OverallStats {
        private Integer totalQuestions;
        private Integer correctCount;
        private BigDecimal accuracyRate;
        private BigDecimal recentAccuracyRate;
        private Integer totalStudyTime;
        private Integer streakDays;
        private Integer activeDays;
        private Integer mistakeCount;
        private Integer dueReviewCount;
        private Integer totalKnowledgeCount;
        private Integer masteredKnowledgeCount;
        private Integer weakKnowledgeCount;
    }

    @Data
    public static class LearningDiagnosis {
        private String level;
        private String learningStage;
        private String summary;
        private String primaryIssue;
        private String nextFocus;
        private String strongestPoint;
        private String weakestPoint;
        private String dataConfidence;
        private List<String> evidence;
    }

    @Data
    public static class KnowledgeCoverage {
        private Integer totalTaggedKnowledge;
        private Integer practicedKnowledge;
        private Integer masteredKnowledge;
        private Integer weakKnowledge;
        private Integer unpracticedKnowledge;
        private BigDecimal coverageRate;
    }

    @Data
    public static class KnowledgeHeatmap {
        private Long knowledgeId;
        private String knowledgeName;
        private Long subjectId;
        private String subjectName;
        private BigDecimal masteryLevel;
        private BigDecimal accuracyRate;
        private Integer totalQuestions;
        private Integer correctQuestions;
        private Integer wrongQuestions;
        private Integer mistakeCount;
        private Integer dueReviewCount;
        private String status;
        private String riskLevel;
        private LocalDateTime lastPracticeAt;
    }

    @Data
    public static class WeakPointAnalysis {
        private Long knowledgeId;
        private String knowledgeName;
        private BigDecimal masteryLevel;
        private Integer totalQuestions;
        private Integer correctQuestions;
        private Integer wrongQuestions;
        private Integer mistakeCount;
        private Integer recentWrongCount;
        private Integer dueReviewCount;
        private String weaknessType;
        private BigDecimal priorityScore;
        private String evidence;
        private String suggestion;
        private List<Long> relatedQuestionIds;
    }

    @Data
    public static class MistakeTypeStats {
        private String mistakeType;
        private String label;
        private Integer count;
        private BigDecimal percentage;
        private String suggestion;
    }

    @Data
    public static class LearningTrend {
        private List<String> dates;
        private List<Integer> questionCounts;
        private List<Integer> correctCounts;
        private List<BigDecimal> accuracyRates;
    }

    @Data
    public static class ActionRecommendation {
        private String type;
        private String title;
        private String description;
        private Long knowledgeId;
        private String knowledgeName;
        private Integer priority;
        private Integer estimatedMinutes;
        private String action;
    }
}
