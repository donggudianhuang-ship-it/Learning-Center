package org.example.smartlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建笔记请求DTO
 */
@Data
public class CreateNoteRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    private Long subjectId;

    private Long knowledgeId;

    private Boolean isPublic = false;
}
