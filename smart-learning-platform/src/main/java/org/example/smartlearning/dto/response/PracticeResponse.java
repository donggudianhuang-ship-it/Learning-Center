package org.example.smartlearning.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 专项练习响应
 */
@Data
public class PracticeResponse {

    /**
     * 练习ID（用于提交时关联）
     */
    private String practiceId;

    /**
     * 练习类型
     */
    private String practiceType;

    /**
     * 练习名称
     */
    private String practiceName;

    /**
     * 题目列表
     */
    private List<QuestionItem> questions;

    /**
     * 总题数
     */
    private Integer totalQuestions;

    /**
     * 当前题目索引
     */
    private Integer currentIndex;

    /**
     * 练习配置信息
     */
    private Map<String, Object> config;

    @Data
    public static class QuestionItem {
        private Long questionId;
        private Integer order;
        private String type;
        private String content;
        private String options;
        private Integer difficulty;
        private List<String> knowledgeNames;
        private String subjectName;
    }
}