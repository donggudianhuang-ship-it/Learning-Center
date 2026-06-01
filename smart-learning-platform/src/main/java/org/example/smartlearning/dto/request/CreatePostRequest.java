package org.example.smartlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

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

    private Long sectionId;

    private Boolean anonymous = false;

    /** 预上传的媒体文件URL列表，发帖时关联到帖子 */
    private List<String> mediaUrls;
}
