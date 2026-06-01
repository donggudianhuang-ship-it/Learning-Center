package org.example.smartlearning.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应DTO
 */
@Data
@Builder
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private List<String> mentions;

    /** 评论作者信息 */
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    /** 被回复者的名字（parentId > 0 时有值，前端直接展示"A 回复了 B"） */
    private String parentAuthorName;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    private LocalDateTime createdAt;
}
