package org.example.smartlearning.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 试卷详情响应
 */
@Data
public class ExamPaperResponse {

    private Long id;

    private String name;

    private Long subjectId;

    private String subjectName;

    private String description;

    private Integer duration;

    private Integer totalQuestions;

    private Integer totalScore;

    private Integer status;

    private List<QuestionDetail> questions;

    @Data
    public static class QuestionDetail {
        private Long questionId;
        private Integer sortOrder;
        private Integer score;
        private String content;
        private String type;
        private Integer difficulty;
        private String options;
        private String knowledgePoints;
    }
}
