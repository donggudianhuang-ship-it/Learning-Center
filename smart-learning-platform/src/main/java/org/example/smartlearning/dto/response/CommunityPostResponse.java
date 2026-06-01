package org.example.smartlearning.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子响应DTO（替代Map，提供类型安全）
 */
@Data
@Builder
public class CommunityPostResponse {

    private Long id;
    private String title;
    private String content;
    private Long subjectId;
    private String subjectName;
    private Long sectionId;
    private String sectionName;
    private Integer anonymous;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;

    /** 作者信息（匿名时显示"匿名用户"） */
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 当前用户是否已收藏 */
    private Boolean collected;

    /** 帖子图片/视频附件 */
    private List<PostMediaResponse> medias;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
