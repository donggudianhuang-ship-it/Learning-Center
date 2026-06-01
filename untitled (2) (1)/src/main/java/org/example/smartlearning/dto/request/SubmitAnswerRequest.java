package org.example.smartlearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 提交答案请求DTO
 */
@Data
public class SubmitAnswerRequest {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    private Long examSubmissionId;

    private String userAnswer;
}
