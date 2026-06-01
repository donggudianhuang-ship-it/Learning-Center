package org.example.smartlearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交整卷答案请求
 */
@Data
public class SubmitExamRequest {

    @NotNull(message = "试卷ID不能为空")
    private Long examId;

    /**
     * 答案列表，格式：题目ID:答案,题目ID:答案
     * 例如：1:A,2:B,3:正确,4:这是主观题答案
     */
    private String answers;

    /**
     * 考试时长（分钟）
     */
    private Integer duration;
}
