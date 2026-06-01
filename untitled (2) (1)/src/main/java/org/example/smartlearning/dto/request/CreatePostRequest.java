package org.example.smartlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建社区帖子请求DTO
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Long subjectId;

    private Boolean anonymous = false;
}
