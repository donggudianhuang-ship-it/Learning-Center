package org.example.smartlearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 专项练习请求
 */
@Data
public class PracticeRequest {

    /**
     * 练习类型: SUBJECT-按科目, KNOWLEDGE-按知识点, TYPE-按题型, MISTAKE-错题专项
     */
    @NotNull(message = "练习类型不能为空")
    private String practiceType;

    /**
     * 科目ID（按科目练习时必填）
     */
    private Long subjectId;

    /**
     * 知识点ID（按知识点练习时必填）
     */
    private Long knowledgeId;

    /**
     * 指定题目ID（从错题详情进入专项练习时使用）
     */
    private Long questionId;

    /**
     * 题型（按题型练习时必填）: SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER, ESSAY
     */
    private String questionType;

    /**
     * 难度等级保留兼容旧请求；当前不再作为练习入口使用
     */
    private Integer difficulty;

    /**
     * 题目数量，默认10
     */
    private Integer limit = 10;

    /**
     * 是否只做错题
     */
    private Boolean onlyMistakes = false;

    /**
     * 是否排除已做对的题
     */
    private Boolean excludeCorrect = false;
}
