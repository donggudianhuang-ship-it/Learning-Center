package org.example.smartlearning.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 帖子详情响应DTO（替代Map，提供类型安全）
 */
@Data
@Builder
public class PostDetailResponse {

    /** 帖子信息 */
    private CommunityPostResponse post;

    /** 评论列表 */
    private List<CommentResponse> comments;
}
